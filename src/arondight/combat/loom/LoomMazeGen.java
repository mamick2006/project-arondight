package arondight.combat.loom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class LoomMazeGen {

	public static final float HALF_W = 6000f;
	public static final float HALF_H = 4500f;
	public static final float SEG_LEN = 512f;
	public static final float THICK = 128f;
	public static final float JAMB = THICK / 2f;

	public static final int COLS = 11;
	public static final int MAZE_ROWS = 7;
	public static final int TOP = MAZE_ROWS;

	public static final int[] X = new int[COLS + 1];
	public static final int[] Y = new int[MAZE_ROWS + 2];
	static {
		for (int c = 0; c <= COLS; c++) X[c] = Math.round(-HALF_W + c * (2f * HALF_W) / COLS);
		for (int r = 0; r <= MAZE_ROWS + 1; r++) Y[r] = Math.round(-HALF_H + r * 1125f);
	}

	private static final int HALL_C0 = 4, HALL_C1 = 6;
	private static final int START_C = 4;

	private static final float MIN_PIECE = 500f;
	private static final float MIN_DOOR = 8f * 44f;
	private static final float GATE_FIT = 460f;
	private static final float ROUTE_CLEAR = 200f;
	private static final int MIN_DEAD_ENDS = 8;
	private static final int REROLL_ATTEMPTS = 32;

	public static final long BAKED_SEED = 20260717L;

	public static class Params {
		public float playerSpeed = 140f;
		public float bossSpeed = 135f;
		public float bossDelay = 17f;
		public float alphaMin = 1.05f;
		public float alphaMax = 2.4f;
	}

	public static final class Layout {
		public long seed;
		public List<float[]> pieces;
		public List<float[]> runs;
		public List<float[]> openings;
		public List<float[]> course;
		public List<float[]> bossRoute;
		public float exitGateX, exitGateY;
		public int exitCol;
		public int lairCell = -1;
		public float dP, dB;
		public int deadEnds;
		public float minPiece, minDoor, gateFit, courseClear, bossClear;
		public String invalidReason;

		public boolean valid() { return invalidReason == null; }
	}

	private LoomMazeGen() {}

	public static Layout generate(long startSeed, Params p) {
		Layout last = null;
		for (int attempt = 0; attempt < REROLL_ATTEMPTS; attempt++) {
			last = build(startSeed + attempt, p);
			if (last.valid()) return last;
		}
		Layout baked = build(BAKED_SEED, p);
		if (baked.valid()) return baked;
		return last;
	}

	private static int cell(int c, int r) { return r * COLS + c; }
	private static int cellC(int id) { return id % COLS; }
	private static int cellR(int id) { return id / COLS; }

	private static final int HALL = cell(START_C, 0);

	private static int nodeOf(int cellId) {
		int c = cellC(cellId), r = cellR(cellId);
		if (r == 0 && c >= HALL_C0 && c <= HALL_C1) return HALL;
		return cellId;
	}

	private static long edgeKey(int a, int b) {
		return a < b ? a * 1000L + b : b * 1000L + a;
	}

	private static final Set<Long> FORCED = new HashSet<Long>();
	static {
		FORCED.add(edgeKey(cell(4, 0), cell(5, 0)));
		FORCED.add(edgeKey(cell(5, 0), cell(6, 0)));
	}

	private static float centerX(int cellId) { return (X[cellC(cellId)] + X[cellC(cellId) + 1]) / 2f; }
	private static float centerY(int cellId) { return (Y[cellR(cellId)] + Y[cellR(cellId) + 1]) / 2f; }

	static Layout build(long seed, Params p) {
		Layout out = new Layout();
		out.seed = seed;
		Random rng = new Random(seed);

		Set<Long> edges = new HashSet<Long>(FORCED);
		boolean[] visited = new boolean[COLS * MAZE_ROWS];
		int[] parentNode = new int[COLS * MAZE_ROWS];
		int[] parentCellP = new int[COLS * MAZE_ROWS];
		int[] parentCellC = new int[COLS * MAZE_ROWS];
		java.util.Arrays.fill(parentNode, -1);
		visited[HALL] = true;
		int visitedCount = 1;
		ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
		stack.push(HALL);
		int[] candCell = new int[16];
		int[] candNCell = new int[16];
		while (!stack.isEmpty()) {
			int node = stack.peek();
			int n = 0;
			int m0 = node == HALL ? cell(HALL_C0, 0) : node;
			int m1 = node == HALL ? cell(HALL_C1, 0) : node;
			for (int mc = m0; mc <= m1; mc++) {
				int c = cellC(mc), r = cellR(mc);
				for (int d = 0; d < 4; d++) {
					int nc = c + (d == 0 ? -1 : d == 1 ? 1 : 0);
					int nr = r + (d == 2 ? -1 : d == 3 ? 1 : 0);
					if (nc < 0 || nc >= COLS || nr < 0 || nr >= MAZE_ROWS) continue;
					int ncell = cell(nc, nr);
					int nnode = nodeOf(ncell);
					if (nnode == node || visited[nnode]) continue;
					candCell[n] = mc;
					candNCell[n] = ncell;
					n++;
				}
			}
			if (n == 0) {
				stack.pop();
				continue;
			}
			int pick = rng.nextInt(n);
			int fromCell = candCell[pick], toCell = candNCell[pick];
			int toNode = nodeOf(toCell);
			edges.add(edgeKey(fromCell, toCell));
			visited[toNode] = true;
			visitedCount++;
			parentNode[toNode] = nodeOf(fromCell);
			parentCellP[toNode] = fromCell;
			parentCellC[toNode] = toCell;
			stack.push(toNode);
		}
		int nodeCount = COLS * MAZE_ROWS - (HALL_C1 - HALL_C0);
		if (visitedCount != nodeCount) {
			out.invalidReason = "carve visited " + visitedCount + " of " + nodeCount + " nodes";
			return out;
		}

		List<Integer> exitCols = new ArrayList<Integer>();
		for (int c = 0; c < COLS; c++) {
			if (c < HALL_C0 || c > HALL_C1) exitCols.add(c);
		}
		Collections.shuffle(exitCols, rng);

		String firstFail = null;
		for (int exitCol : exitCols) {
			String fail = tryExit(out, p, exitCol, edges, parentNode, parentCellP, parentCellC);
			if (fail == null) return out;
			if (firstFail == null) firstFail = "exit col " + exitCol + ": " + fail;
		}
		out.invalidReason = "no exit column passed (first miss: " + firstFail + ")";
		return out;
	}

	private static String tryExit(Layout out, Params p, int exitCol, Set<Long> edges,
								  int[] parentNode, int[] parentCellP, int[] parentCellC) {
		String tablesFail = buildTables(out, edges, exitCol);
		if (tablesFail != null) return tablesFail;
		out.exitCol = exitCol;
		int exitCell = cell(exitCol, MAZE_ROWS - 1);

		List<Integer> playerNodes = nodeRoute(parentNode, exitCell);
		List<Integer> playerCells = expandCells(playerNodes, parentNode, parentCellP, parentCellC, cell(START_C, 0));
		Map<Integer, Integer> playerWp = new HashMap<Integer, Integer>();
		out.course = waypoints(playerCells, playerWp, exitCol);
		out.dP = arcLength(out.course);

		Set<Integer> onRoute = new HashSet<Integer>(playerNodes);
		out.deadEnds = countDeadEnds(parentNode, onRoute, exitCell);
		if (out.deadEnds < MIN_DEAD_ENDS) {
			return "only " + out.deadEnds + " dead ends off the route (need " + MIN_DEAD_ENDS + ")";
		}

		List<float[]> rects = tileRects(out.pieces);
		out.gateFit = minRectDist(out.exitGateX, out.exitGateY, rects);
		if (out.gateFit < GATE_FIT) {
			return "exit gate only " + (int) out.gateFit + " su from a wall (need " + (int) GATE_FIT + ")";
		}
		out.courseClear = polylineClearance(out.course, rects);
		if (out.courseClear < ROUTE_CLEAR) {
			return "player route clearance " + (int) out.courseClear + " (need " + (int) ROUTE_CLEAR + ")";
		}

		out.lairCell = HALL;
		out.bossRoute = out.course;
		out.dB = out.dP;
		out.bossClear = out.courseClear;

		if (p.bossSpeed > 0f) {
			float tP = out.dP / p.playerSpeed;
			float tB = p.bossDelay + out.dB / p.bossSpeed;
			if (tB < p.alphaMin * tP || tB > p.alphaMax * tP) {
				return "race ratio " + (tB / tP) + " outside ["
						+ p.alphaMin + ", " + p.alphaMax + "]";
			}
		}
		return null;
	}

	private static float[] door(int cellA, int cellB) {
		int a = Math.min(cellA, cellB), b = Math.max(cellA, cellB);
		int c1 = cellC(a), r1 = cellR(a), c2 = cellC(b);
		if (r1 == cellR(b)) {
			float lo, hi;
			if (FORCED.contains(edgeKey(a, b))) {
				lo = Y[0]; hi = Y[1] + JAMB;
			} else if (r1 == 0) {
				lo = Y[0] + SEG_LEN; hi = Y[1] - JAMB;
			} else if (r1 == MAZE_ROWS - 1) {
				lo = Y[r1] + JAMB; hi = Y[TOP] - SEG_LEN;
			} else {
				lo = Y[r1] + JAMB; hi = Y[r1 + 1] - JAMB - SEG_LEN + THICK;
			}
			return new float[] {1f, c2, lo, hi};
		}
		return doorInHorizontalLine(r1 + 1, c1);
	}

	private static float[] doorInHorizontalLine(int line, int c) {
		float lo, hi;
		if (c == 0) {
			lo = X[0] + SEG_LEN; hi = X[1] - JAMB;
		} else if (c == COLS - 1) {
			lo = X[COLS - 1] + JAMB; hi = X[COLS] - SEG_LEN;
		} else {
			lo = X[c] + JAMB; hi = X[c + 1] - JAMB - SEG_LEN + THICK;
		}
		return new float[] {0f, line, lo, hi};
	}

	private static float[] doorMid(int cellA, int cellB) {
		float[] d = door(cellA, cellB);
		float mid = (d[2] + d[3]) / 2f;
		return d[0] == 1f ? new float[] {X[(int) d[1]], mid} : new float[] {mid, Y[(int) d[1]]};
	}

	private static String buildTables(Layout out, Set<Long> edges, int exitCol) {
		out.runs = new ArrayList<float[]>();
		for (int m = 1; m <= TOP; m++) {
			out.runs.add(new float[] {0f, Y[m], -HALF_W, HALF_W});
		}
		for (int k = 1; k < COLS; k++) {
			out.runs.add(new float[] {1f, X[k], -HALF_H, Y[TOP]});
		}

		List<List<float[]>> holes = new ArrayList<List<float[]>>();
		for (int i = 0; i < out.runs.size(); i++) holes.add(new ArrayList<float[]>());
		List<Long> sortedEdges = new ArrayList<Long>(edges);
		Collections.sort(sortedEdges);
		for (long key : sortedEdges) {
			int a = (int) (key / 1000L), b = (int) (key % 1000L);
			float[] d = door(a, b);
			int runIdx = d[0] == 0f ? (int) d[1] - 1 : (TOP - 1) + (int) d[1];
			holes.get(runIdx).add(new float[] {d[2], d[3]});
		}
		float[] exit = doorInHorizontalLine(TOP, exitCol);
		holes.get(TOP - 1).add(new float[] {exit[2], exit[3]});
		out.exitGateX = (exit[2] + exit[3]) / 2f;
		out.exitGateY = Y[TOP] + (HALF_H - Y[TOP]) / 2f;

		out.openings = new ArrayList<float[]>();
		out.minDoor = Float.MAX_VALUE;
		for (int ri = 0; ri < holes.size(); ri++) {
			List<float[]> hs = holes.get(ri);
			Collections.sort(hs, new java.util.Comparator<float[]>() {
				@Override
				public int compare(float[] u, float[] v) { return Float.compare(u[0], v[0]); }
			});
			List<float[]> merged = new ArrayList<float[]>();
			for (float[] h : hs) {
				if (!merged.isEmpty() && h[0] - merged.get(merged.size() - 1)[1] < MIN_PIECE) {
					merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], h[1]);
				} else {
					merged.add(new float[] {h[0], h[1]});
				}
			}
			for (float[] h : merged) {
				out.openings.add(new float[] {ri, (h[0] + h[1]) / 2f, h[1] - h[0]});
				out.minDoor = Math.min(out.minDoor, h[1] - h[0]);
			}
		}
		if (out.minDoor < MIN_DOOR) {
			return "door " + (int) out.minDoor + " su wide (need " + (int) MIN_DOOR + ")";
		}

		int topOpenings = 0;
		for (float[] op : out.openings) {
			if ((int) op[0] == TOP - 1) topOpenings++;
		}
		if (topOpenings != 1) {
			return topOpenings + " openings in the top perimeter run (need exactly 1)";
		}

		out.pieces = new ArrayList<float[]>();
		out.minPiece = Float.MAX_VALUE;
		for (int ri = 0; ri < out.runs.size(); ri++) {
			float[] run = out.runs.get(ri);
			List<float[]> hs = new ArrayList<float[]>();
			for (float[] op : out.openings) {
				if ((int) op[0] == ri) hs.add(op);
			}
			float cursor = run[2];
			for (float[] op : hs) {
				float from = op[1] - op[2] / 2f, to = op[1] + op[2] / 2f;
				if (from > cursor) {
					out.pieces.add(new float[] {run[0], run[1], cursor, from});
					out.minPiece = Math.min(out.minPiece, from - cursor);
				}
				cursor = to;
			}
			if (cursor < run[3]) {
				out.pieces.add(new float[] {run[0], run[1], cursor, run[3]});
				out.minPiece = Math.min(out.minPiece, run[3] - cursor);
			}
		}
		if (out.minPiece < MIN_PIECE) {
			return "piece " + (int) out.minPiece + " su long (need " + (int) MIN_PIECE + ")";
		}
		return null;
	}

	private static List<Integer> nodeRoute(int[] parentNode, int node) {
		List<Integer> route = new ArrayList<Integer>();
		route.add(node);
		while (route.get(route.size() - 1) != HALL) {
			route.add(parentNode[route.get(route.size() - 1)]);
		}
		Collections.reverse(route);
		return route;
	}

	private static List<Integer> expandCells(List<Integer> nodes, int[] parentNode,
											 int[] parentCellP, int[] parentCellC, int anchorCell) {
		List<Integer> cells = new ArrayList<Integer>();
		cells.add(anchorCell);
		for (int i = 0; i + 1 < nodes.size(); i++) {
			int a = nodes.get(i), b = nodes.get(i + 1);
			int cellA, cellB;
			if (parentNode[b] == a) {
				cellA = parentCellP[b]; cellB = parentCellC[b];
			} else {
				cellA = parentCellC[a]; cellB = parentCellP[a];
			}
			int cur = cells.get(cells.size() - 1);
			if (cur != cellA) {
				int step = cellC(cellA) > cellC(cur) ? 1 : -1;
				for (int c = cellC(cur) + step; c != cellC(cellA) + step; c += step) {
					cells.add(cell(c, cellR(cur)));
				}
			}
			cells.add(cellB);
		}
		return cells;
	}

	private static List<float[]> waypoints(List<Integer> cells, Map<Integer, Integer> cellWpOut, int exitCol) {
		List<float[]> wps = new ArrayList<float[]>();
		wps.add(new float[] {centerX(cells.get(0)), centerY(cells.get(0))});
		cellWpOut.put(cells.get(0), 0);
		for (int i = 0; i + 1 < cells.size(); i++) {
			int a = cells.get(i), b = cells.get(i + 1);
			if (!FORCED.contains(edgeKey(a, b))) {
				wps.add(doorMid(a, b));
			}
			wps.add(new float[] {centerX(b), centerY(b)});
			cellWpOut.put(b, wps.size() - 1);
		}
		float[] exit = doorInHorizontalLine(TOP, exitCol);
		float mid = (exit[2] + exit[3]) / 2f;
		wps.add(new float[] {mid, Y[TOP]});
		wps.add(new float[] {mid, Y[TOP] + (HALF_H - Y[TOP]) / 2f});
		return wps;
	}

	private static float arcLength(List<float[]> wps) {
		float total = 0f;
		for (int i = 1; i < wps.size(); i++) {
			total += (float) Math.hypot(wps.get(i)[0] - wps.get(i - 1)[0], wps.get(i)[1] - wps.get(i - 1)[1]);
		}
		return total;
	}

	private static int countDeadEnds(int[] parentNode, Set<Integer> onRoute, int exitCell) {
		Map<Integer, Integer> deg = new HashMap<Integer, Integer>();
		for (int node = 0; node < parentNode.length; node++) {
			if (parentNode[node] < 0) continue;
			Integer d = deg.get(node);
			deg.put(node, d == null ? 1 : d + 1);
			d = deg.get(parentNode[node]);
			deg.put(parentNode[node], d == null ? 1 : d + 1);
		}
		int count = 0;
		for (Map.Entry<Integer, Integer> e : deg.entrySet()) {
			if (e.getValue() != 1) continue;
			int node = e.getKey();
			if (node == HALL || node == exitCell || onRoute.contains(node)) continue;
			count++;
		}
		return count;
	}

	static List<float[]> tileRects(List<float[]> pieces) {
		List<float[]> rects = new ArrayList<float[]>();
		for (float[] piece : pieces) {
			boolean vertical = piece[0] != 0f;
			float span = piece[3] - piece[2];
			int n = Math.max(1, (int) Math.ceil(span / SEG_LEN - 0.02f));
			float step = n > 1 ? (span - SEG_LEN) / (n - 1) : 0f;
			for (int i = 0; i < n; i++) {
				float along = n > 1 ? piece[2] + SEG_LEN / 2f + step * i : (piece[2] + piece[3]) / 2f;
				if (vertical) {
					rects.add(new float[] {piece[1] - JAMB, piece[1] + JAMB, along - SEG_LEN / 2f, along + SEG_LEN / 2f});
				} else {
					rects.add(new float[] {along - SEG_LEN / 2f, along + SEG_LEN / 2f, piece[1] - JAMB, piece[1] + JAMB});
				}
			}
		}
		return rects;
	}

	private static float rectDist(float px, float py, float[] r) {
		float dx = Math.max(r[0] - px, Math.max(0f, px - r[1]));
		float dy = Math.max(r[2] - py, Math.max(0f, py - r[3]));
		return (float) Math.hypot(dx, dy);
	}

	private static float minRectDist(float px, float py, List<float[]> rects) {
		float min = Float.MAX_VALUE;
		for (float[] r : rects) {
			min = Math.min(min, rectDist(px, py, r));
		}
		return min;
	}

	private static float polylineClearance(List<float[]> wps, List<float[]> rects) {
		float worst = Float.MAX_VALUE;
		for (int i = 1; i < wps.size(); i++) {
			float ax = wps.get(i - 1)[0], ay = wps.get(i - 1)[1];
			float bx = wps.get(i)[0], by = wps.get(i)[1];
			int n = Math.max(1, (int) (Math.hypot(bx - ax, by - ay) / 25f));
			for (int s = 0; s <= n; s++) {
				float t = (float) s / n;
				float px = ax + (bx - ax) * t, py = ay + (by - ay) * t;
				for (float[] r : rects) {
					float d = rectDist(px, py, r);
					if (d < worst) worst = d;
				}
			}
		}
		return worst;
	}

	public static void main(String[] args) {
		int fuzz = 200;
		long start = 20260716L;
		long dumpSeed = Long.MIN_VALUE;
		for (int i = 0; i < args.length; i++) {
			if ("--fuzz".equals(args[i]) && i + 1 < args.length) fuzz = Integer.parseInt(args[++i]);
			else if ("--start".equals(args[i]) && i + 1 < args.length) start = Long.parseLong(args[++i]);
			else if ("--dump".equals(args[i]) && i + 1 < args.length) dumpSeed = Long.parseLong(args[++i]);
		}
		Params p = new Params();
		if (dumpSeed != Long.MIN_VALUE) {
			dump(generate(dumpSeed, p));
			return;
		}

		List<String> fails = new ArrayList<String>();
		Layout baked = build(BAKED_SEED, p);
		if (!baked.valid()) fails.add("BAKED_SEED " + BAKED_SEED + " invalid: " + baked.invalidReason);

		int rawInvalid = 0, maxAttempts = 0;
		float worstClear = Float.MAX_VALUE, worstGate = Float.MAX_VALUE;
		int worstDead = Integer.MAX_VALUE;
		for (int i = 0; i < fuzz; i++) {
			long seed = start + i;
			if (!build(seed, p).valid()) rawInvalid++;
			Layout a = generate(seed, p);
			if (!a.valid()) {
				fails.add("generate(" + seed + ") settled invalid: " + a.invalidReason);
				continue;
			}
			maxAttempts = Math.max(maxAttempts, (int) (a.seed - seed) + 1);
			worstClear = Math.min(worstClear, Math.min(a.courseClear, a.bossClear));
			worstGate = Math.min(worstGate, a.gateFit);
			worstDead = Math.min(worstDead, a.deadEnds);
			String flood = floodCheck(a);
			if (flood != null) fails.add("seed " + a.seed + ": " + flood);
			if (i % 10 == 0) {
				Layout b = generate(seed, p);
				String diff = compare(a, b);
				if (diff != null) fails.add("seed " + seed + " NOT deterministic: " + diff);
			}
		}

		if (!fails.isEmpty()) {
			System.out.println("LOOM MAZE GEN: " + fails.size() + " FAIL");
			for (String f : fails) System.out.println("  - " + f);
			System.exit(1);
		}
		System.out.println("LOOM MAZE GEN: PASS (" + fuzz + " seeds from " + start
				+ "; " + rawInvalid + " raw rerolls, worst settle depth " + maxAttempts
				+ " of " + REROLL_ATTEMPTS + "; worst route clearance " + (int) worstClear
				+ ", worst gate fit " + (int) worstGate + ", min dead ends " + worstDead
				+ "; baked seed " + BAKED_SEED + " valid; determinism sampled every 10th seed)");
	}

	private static String floodCheck(Layout a) {
		final float step = 25f, grow = 44f;
		int nx = (int) (2 * HALF_W / step), ny = (int) (2 * HALF_H / step);
		boolean[] blocked = new boolean[nx * ny];
		for (float[] r : tileRects(a.pieces)) {
			int cx0 = Math.max(0, (int) ((r[0] - grow + HALF_W) / step));
			int cx1 = Math.min(nx - 1, (int) ((r[1] + grow + HALF_W) / step));
			int cy0 = Math.max(0, (int) ((r[2] - grow + HALF_H) / step));
			int cy1 = Math.min(ny - 1, (int) ((r[3] + grow + HALF_H) / step));
			for (int cy = cy0; cy <= cy1; cy++) {
				for (int cx = cx0; cx <= cx1; cx++) blocked[cy * nx + cx] = true;
			}
		}
		float[] startWp = a.course.get(0);
		int sx = Math.min(nx - 1, Math.max(0, (int) ((startWp[0] + HALF_W) / step)));
		int sy = Math.min(ny - 1, Math.max(0, (int) ((startWp[1] + HALF_H) / step)));
		if (blocked[sy * nx + sx]) return "start is inside a wall grown by the frame radius";
		float[] dist = new float[nx * ny];
		java.util.Arrays.fill(dist, -1f);
		dist[sy * nx + sx] = 0f;
		ArrayDeque<Integer> q = new ArrayDeque<Integer>();
		q.add(sy * nx + sx);
		while (!q.isEmpty()) {
			int cur = q.poll();
			int cx = cur % nx, cy = cur / nx;
			float d = dist[cur];
			if (cx > 0 && !blocked[cur - 1] && dist[cur - 1] < 0) { dist[cur - 1] = d + step; q.add(cur - 1); }
			if (cx < nx - 1 && !blocked[cur + 1] && dist[cur + 1] < 0) { dist[cur + 1] = d + step; q.add(cur + 1); }
			if (cy > 0 && !blocked[cur - nx] && dist[cur - nx] < 0) { dist[cur - nx] = d + step; q.add(cur - nx); }
			if (cy < ny - 1 && !blocked[cur + nx] && dist[cur + nx] < 0) { dist[cur + nx] = d + step; q.add(cur + nx); }
		}
		int gx = Math.min(nx - 1, Math.max(0, (int) ((a.exitGateX + HALF_W) / step)));
		int gy = Math.min(ny - 1, Math.max(0, (int) ((a.exitGateY + HALF_H) / step)));
		float geo = dist[gy * nx + gx];
		if (geo < 0) return "exit gate UNREACHABLE from the start at the frame's radius";
		if (geo < a.dP * 0.6f) {
			return "geodesic " + (int) geo + " su vs authored route " + (int) a.dP + ": something bypasses the maze";
		}
		return null;
	}

	private static String compare(Layout a, Layout b) {
		if (a.seed != b.seed) return "settled on different seeds " + a.seed + " vs " + b.seed;
		if (a.exitCol != b.exitCol || a.lairCell != b.lairCell) return "exit/lair differ";
		if (a.pieces.size() != b.pieces.size()) return "piece counts differ";
		for (int i = 0; i < a.pieces.size(); i++) {
			for (int j = 0; j < 4; j++) {
				if (a.pieces.get(i)[j] != b.pieces.get(i)[j]) return "piece " + i + " differs";
			}
		}
		if (a.course.size() != b.course.size() || a.bossRoute.size() != b.bossRoute.size()) return "route sizes differ";
		for (int i = 0; i < a.course.size(); i++) {
			if (a.course.get(i)[0] != b.course.get(i)[0] || a.course.get(i)[1] != b.course.get(i)[1]) return "course wp " + i + " differs";
		}
		for (int i = 0; i < a.bossRoute.size(); i++) {
			if (a.bossRoute.get(i)[0] != b.bossRoute.get(i)[0] || a.bossRoute.get(i)[1] != b.bossRoute.get(i)[1]) return "boss wp " + i + " differs";
		}
		return null;
	}

	private static void dump(Layout a) {
		System.out.println("seed " + a.seed + (a.valid() ? " VALID" : " INVALID: " + a.invalidReason));
		System.out.println("exitCol " + a.exitCol + " gate " + a.exitGateX + "," + a.exitGateY
				+ " lair " + (a.lairCell < 0 ? "-" : cellC(a.lairCell) + "," + cellR(a.lairCell)));
		System.out.println("dP " + (int) a.dP + " dB " + (int) a.dB + " deadEnds " + a.deadEnds);
		System.out.println("RUNS " + a.runs.size());
		for (float[] r : a.runs) System.out.println("  " + r[0] + " " + r[1] + " " + r[2] + " " + r[3]);
		System.out.println("OPENINGS " + a.openings.size());
		for (float[] o : a.openings) System.out.println("  " + o[0] + " " + o[1] + " " + o[2]);
		System.out.println("COURSE " + a.course.size());
		for (float[] w : a.course) System.out.println("  " + w[0] + " " + w[1]);
		System.out.println("BOSS " + a.bossRoute.size());
		for (float[] w : a.bossRoute) System.out.println("  " + w[0] + " " + w[1]);
	}
}
