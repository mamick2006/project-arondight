package arondight.combat.loom;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;

import arondight.campaign.ArondightQuest;

public final class LoomLayout {

	public static final float HALF_W = LoomMazeGen.HALF_W;
	public static final float HALF_H = LoomMazeGen.HALF_H;
	public static final float SEG_LEN = LoomMazeGen.SEG_LEN;
	public static final float WALL_THICKNESS = LoomMazeGen.THICK;

	public static final float WALL_LOW_Y = LoomMazeGen.Y[1];
	public static final float WALL_HIGH_Y = LoomMazeGen.Y[LoomMazeGen.TOP];

	private static LoomMazeGen.Layout current = null;

	private LoomLayout() {}

	public static long generate(long seed) {
		LoomMazeGen.Params p = new LoomMazeGen.Params();
		p.bossSpeed = ArondightQuest.loomSweeperSpeed();
		p.alphaMin = ArondightQuest.f("arondight_loom_alpha_min", 1.05f);
		p.alphaMax = ArondightQuest.f("arondight_loom_alpha_max", 2.4f);
		current = LoomMazeGen.generate(seed, p);
		return current.seed;
	}

	private static LoomMazeGen.Layout current() {
		if (current == null) {
			Global.getLogger(LoomLayout.class).warn(
					"Project Arondight: LoomLayout read before LoomBCP generated a maze; generating now");
			generate(resolveSeed());
		}
		return current;
	}

	public static long resolveSeed() {
		long pinned = (long) ArondightQuest.f("arondight_loom_seed", 0f);
		if (pinned != 0L) return pinned;
		return Misc.random.nextLong();
	}

	public static long seed() {
		return current().seed;
	}

	public static List<float[]> wallPieces() {
		return current().pieces;
	}

	public static Vector2f exitGate() {
		LoomMazeGen.Layout l = current();
		return new Vector2f(l.exitGateX, l.exitGateY);
	}

	public static List<Vector2f> checkpoints() {
		List<Vector2f> gates = new ArrayList<Vector2f>();
		gates.add(exitGate());
		return gates;
	}

	public static List<Vector2f> coursePath() {
		return toVectors(current().course);
	}

	public static List<Vector2f> bossPath() {
		return toVectors(current().bossRoute);
	}

	public static Vector2f start() {
		float[] wp = current().course.get(0);
		return new Vector2f(wp[0], wp[1]);
	}

	private static List<Vector2f> toVectors(List<float[]> wps) {
		List<Vector2f> path = new ArrayList<Vector2f>(wps.size());
		for (float[] wp : wps) {
			path.add(new Vector2f(wp[0], wp[1]));
		}
		return path;
	}
}
