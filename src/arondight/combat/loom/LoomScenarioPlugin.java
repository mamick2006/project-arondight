package arondight.combat.loom;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import arondight.campaign.ArondightQuest;
import arondight.combat.LethalSaveScript;

public class LoomScenarioPlugin extends BaseEveryFrameCombatPlugin {

	protected static final float GATE_RADIUS = 450f;
	protected static final Color COURSE_COLOR = new Color(140, 220, 180);

	protected static final String[] SWEEPER_HULLS = { "arondight_shuttle_Sweeper", "hive_unit_Type350" };
	protected static final float SWEEPER_ACTIVATION_DELAY = 17f;

	protected CombatEngineAPI engine;
	protected final IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

	protected Vector2f exitGate;
	protected int nextGate = 0;
	protected boolean over = false;
	protected ShipAPI playerFrame = null;
	protected float noPlayerSeconds = 0f;
	protected static final float NO_PLAYER_LIMIT = 10f;
	protected LethalSaveScript lethalSave = null;
	protected boolean weaponsLocked = false;
	protected boolean systemDisabled = false;

	protected List<Vector2f> path;
	protected float[] cumLen;
	protected List<ShipAPI> wallShips = null;
	protected List<float[]> wallHomes = null;
	protected final java.util.Map<ShipAPI, Boolean> wallSet =
			new java.util.IdentityHashMap<ShipAPI, Boolean>();
	protected float[][] hSpans = null;
	protected float[][] vSpans = null;
	protected final java.util.List<ShipAPI> constrained = new java.util.ArrayList<ShipAPI>();
	protected ShipAPI sweeper = null;
	protected List<WeaponAPI> sweeperWeapons = null;
	protected float sweeperProgress = 0f;
	protected float sweeperClock = 0f;
	protected boolean sweeperAnnounced = false;
	protected boolean controlNamesOk = true;

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
		engine.setDoNotEndCombat(true);
		exitGate = LoomLayout.exitGate();
		ArondightQuest.set(ArondightQuest.LOOM_STARTED);

		path = LoomLayout.bossPath();
		cumLen = new float[path.size()];
		cumLen[0] = 0f;
		for (int i = 1; i < path.size(); i++) {
			cumLen[i] = cumLen[i - 1] + Misc.getDistance(path.get(i - 1), path.get(i));
		}

		buildWallSpans();

		live = this;
	}

	protected void buildWallSpans() {
		List<List<float[]>> h = new java.util.ArrayList<List<float[]>>();
		List<List<float[]>> v = new java.util.ArrayList<List<float[]>>();
		for (int m = 0; m <= LoomMazeGen.TOP; m++) h.add(new java.util.ArrayList<float[]>());
		for (int k = 0; k < LoomMazeGen.COLS; k++) v.add(new java.util.ArrayList<float[]>());
		for (float[] piece : LoomLayout.wallPieces()) {
			if (piece[0] == 0f) {
				int m = Math.round((piece[1] + LoomMazeGen.HALF_H) / 1125f);
				if (m >= 1 && m <= LoomMazeGen.TOP) h.get(m).add(piece);
			} else {
				int k = Math.round((piece[1] + LoomMazeGen.HALF_W)
						* LoomMazeGen.COLS / (2f * LoomMazeGen.HALF_W));
				if (k >= 1 && k <= LoomMazeGen.COLS - 1) v.get(k).add(piece);
			}
		}
		hSpans = flattenSpans(h);
		vSpans = flattenSpans(v);
	}

	private static float[][] flattenSpans(List<List<float[]>> buckets) {
		float[][] out = new float[buckets.size()][];
		for (int i = 0; i < buckets.size(); i++) {
			List<float[]> b = buckets.get(i);
			if (b.isEmpty()) continue;
			float[] flat = new float[b.size() * 2];
			for (int j = 0; j < b.size(); j++) {
				flat[j * 2] = b.get(j)[2];
				flat[j * 2 + 1] = b.get(j)[3];
			}
			out[i] = flat;
		}
		return out;
	}

	protected void constrainToMaze(ShipAPI ship) {
		if (ship == null || isWall(ship) || ship.isHulk() || ship == sweeper
				|| hSpans == null) return;
		Vector2f loc = ship.getLocation();
		float band = LoomMazeGen.JAMB + ship.getCollisionRadius();
		int m = Math.round((loc.y + LoomMazeGen.HALF_H) / 1125f);
		if (m >= 1 && m <= LoomMazeGen.TOP) {
			float lineY = LoomMazeGen.Y[m];
			float dy = loc.y - lineY;
			if (dy > -band && dy < band
					&& overlapsSpan(hSpans[m], loc.x - ship.getCollisionRadius(),
							loc.x + ship.getCollisionRadius())) {
				float side = dy >= 0f ? 1f : -1f;
				loc.y = lineY + side * band;
				if (ship.getVelocity().y * side < 0f) ship.getVelocity().y = 0f;
			}
		}
		int k = Math.round((loc.x + LoomMazeGen.HALF_W)
				* LoomMazeGen.COLS / (2f * LoomMazeGen.HALF_W));
		if (k >= 1 && k <= LoomMazeGen.COLS - 1) {
			float lineX = LoomMazeGen.X[k];
			float dx = loc.x - lineX;
			if (dx > -band && dx < band
					&& overlapsSpan(vSpans[k], loc.y - ship.getCollisionRadius(),
							loc.y + ship.getCollisionRadius())) {
				float side = dx >= 0f ? 1f : -1f;
				loc.x = lineX + side * band;
				if (ship.getVelocity().x * side < 0f) ship.getVelocity().x = 0f;
			}
		}
	}

	protected boolean isWall(ShipAPI ship) {
		return wallSet.containsKey(ship);
	}

	protected static boolean overlapsSpan(float[] spans, float lo, float hi) {
		if (spans == null) return false;
		for (int i = 0; i < spans.length; i += 2) {
			if (spans[i] < hi && spans[i + 1] > lo) return true;
		}
		return false;
	}

	protected void refreshConstrained(ShipAPI player) {
		constrained.clear();
		List<ShipAPI> ships = engine.getShips();
		for (int i = 0; i < ships.size(); i++) {
			ShipAPI s = ships.get(i);
			if (s == null || isWall(s) || s.isHulk() || s == sweeper || s == player) continue;
			constrained.add(s);
		}
	}

	protected static LoomScenarioPlugin live = null;

	public static LoomScenarioPlugin getLive() {
		if (live == null) return null;
		if (live.engine == null || live.engine != Global.getCombatEngine()) return null;
		return live;
	}

	public static void resetForLoad() {
		live = null;
		LoomZoomLock.restore();
	}

	public int getNextGateIndex() {
		return nextGate;
	}

	public int getGateCount() {
		return 1;
	}

	public float getCountdown() {
		float speed = ArondightQuest.loomSweeperSpeed();
		if (speed <= 0f) return -1f;
		if (sweeperAnnounced && sweeper == null) return -1f;
		float total = cumLen[cumLen.length - 1];
		float travel = (total - Math.min(sweeperProgress, total)) / speed;
		float delayLeft = Math.max(0f, SWEEPER_ACTIVATION_DELAY - sweeperClock);
		return delayLeft + travel;
	}

	public boolean isOver() {
		return over;
	}

	public boolean isWeaponsLocked() {
		return weaponsLocked;
	}

	public ShipAPI getSweeper() {
		return sweeper;
	}

	public float getSweeperProgress() {
		return sweeperProgress;
	}

	private String fireLockLine;

	private String fireLockLine() {
		if (fireLockLine == null) {
			fireLockLine = arondight.ArondightStrings.combat("combat_loom_fire_lock");
		}
		return fireLockLine;
	}

	private String runLine;

	private String runLine() {
		if (runLine == null) {
			runLine = arondight.ArondightStrings.combat("combat_loom_run");
		}
		return runLine;
	}

	@Override
	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		if (engine == null || over) return;
		try {
			for (int i = 0; i < events.size(); i++) {
				InputEventAPI ev = events.get(i);
				if (ev.isConsumed()) continue;
				if (ev.isMouseScrollEvent()) {
					ev.consume();
					continue;
				}
				if (controlNamesOk) {
					try {
						if (ev.isControlDownEvent("SHIP_SHOW_WARROOM")
								|| ev.isControlDownEvent("GENERAL_ZOOM_IN")
								|| ev.isControlDownEvent("GENERAL_ZOOM_OUT")) {
							ev.consume();
							continue;
						}
					} catch (Exception e) {
						controlNamesOk = false;
						Global.getLogger(LoomScenarioPlugin.class).warn(
								"Project Arondight: combat control enum names did not resolve; "
										+ "falling back to blocking raw TAB for the trial", e);
					}
				}
				if (!controlNamesOk && ev.isKeyDownEvent() && ev.getEventValue() == Keyboard.KEY_TAB) {
					ev.consume();
				}
			}
		} catch (Throwable t) {
			Global.getLogger(LoomScenarioPlugin.class).warn(
					"Project Arondight: processInputPreCoreControls threw (likely a "
							+ "conflicting mod's entity in this battle); ending the trial "
							+ "as a loss instead of leaving the input lock half-applied", t);
			lose(engine.getPlayerShip());
		}
	}

	protected String frameLost(ShipAPI reported) {
		ShipAPI frame = reported != null && !reported.isShuttlePod() ? reported : playerFrame;
		if (frame == null) {
			return "the engine offers only its stand-in shuttle and this trial never "
					+ "flew a frame";
		}
		if (frame != reported) {
			return "the engine swapped the frame for its stand-in shuttle";
		}
		if (frame.isAlive()) {
			return null;
		}
		if (frame.isHulk()) {
			return "the frame is a wreck";
		}
		if (!engine.isEntityInPlay(frame)) {
			return "the frame was removed from the battle at "
					+ (int) frame.getHitpoints() + " hp";
		}
		return "the frame reads dead in play at " + (int) frame.getHitpoints() + " hp";
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused() || over) return;

		try {
			ShipAPI player = engine.getPlayerShip();
			if (player != null && !player.isShuttlePod()) {
				playerFrame = player;
			}
			String lost = frameLost(player);
			if (lost != null) {
				player = null;
				noPlayerSeconds += amount;
				if (noPlayerSeconds > NO_PLAYER_LIMIT) {
					Global.getLogger(LoomScenarioPlugin.class).warn(
							"Project Arondight: no player frame in the Loom for "
									+ NO_PLAYER_LIMIT + "s (" + lost + "); ending the "
									+ "trial as a loss instead of leaving a battle "
									+ "nothing can end");
					lose(null);
					return;
				}
			} else {
				noPlayerSeconds = 0f;
			}
			if (player != null && lethalSave == null) {
				lethalSave = new LethalSaveScript(player, "THE COURSE POWERS DOWN");
				player.addListener(lethalSave);
			}
			if (lethalSave != null && lethalSave.isTriggered()) {
				over = true;
				live = null;
				LoomZoomLock.restore();
				hulkWallsForResolution();
				return;
			}

			if (player != null) {
				weaponsLocked = true;
				player.blockCommandForOneFrame(com.fs.starfarer.api.combat.ShipCommand.FIRE);
				player.blockCommandForOneFrame(com.fs.starfarer.api.combat.ShipCommand.TOGGLE_AUTOFIRE);
				if (!systemDisabled) {
					player.setShipSystemDisabled(true);
					systemDisabled = true;
				}

				float eta = getCountdown();
				if (eta >= 0f) {
					engine.maintainStatusForPlayerShip("arondight_loom_clock",
							"graphics/icons/relay.png", "PROVING COURSE",
							"EXIT SEALS IN " + clock(eta), eta < 15f);
				}
				engine.maintainStatusForPlayerShip("arondight_loom_lock",
						"graphics/icons/relay.png", "PROVING COURSE",
						fireLockLine(), true);
				if (eta >= 0f && sweeperClock < SWEEPER_ACTIVATION_DELAY) {
					engine.maintainStatusForPlayerShip("arondight_loom_run",
							"graphics/icons/relay.png", "PROVING COURSE",
							runLine(), true);
				}

				constrainToMaze(player);

				checkExit(player);
				if (over) return;
			}

			for (int i = 0; i < constrained.size(); i++) {
				constrainToMaze(constrained.get(i));
			}

			advanceSweeper(amount, player);
			if (over) return;

			interval.advance(amount);
			if (interval.intervalElapsed() && player != null) {
				for (com.fs.starfarer.api.combat.WeaponGroupAPI group : player.getWeaponGroupsCopy()) {
					if (group.isAutofiring()) group.toggleOff();
				}
				engine.spawnExplosion(exitGate, new Vector2f(), COURSE_COLOR, 95f, 0.55f);
				rePinWalls();
				refreshConstrained(player);
				sweeperTick();
				if (engine.getFleetManager(0).getTaskManager(false).isInFullRetreat()) {
					lose(player);
					return;
				}
			}
		} catch (Throwable t) {
			Global.getLogger(LoomScenarioPlugin.class).warn(
					"Project Arondight: LoomScenarioPlugin.advance threw (likely a "
							+ "conflicting mod's entity in this battle); ending the trial "
							+ "as a loss instead of leaving it half-updated", t);
			lose(engine.getPlayerShip());
		}
	}

	protected void advanceSweeper(float amount, ShipAPI player) {
		float speed = ArondightQuest.loomSweeperSpeed();
		if (speed <= 0f || player == null) return;

		sweeperClock += amount;
		if (sweeperClock < SWEEPER_ACTIVATION_DELAY) return;

		if (sweeper == null && !sweeperAnnounced) {
			sweeperAnnounced = true;
			sweeper = spawnSweeper();
			if (sweeper == null) return;
			Global.getSoundPlayer().playSound("system_phase_cloak_activate", 0.5f, 1.3f,
					sweeper.getLocation(), sweeper.getVelocity());
		}
		if (sweeper != null && sweeper.isHulk()) {
			sweeper = null;
		}
		if (sweeper == null) return;

		sweeperProgress += speed * amount;
		Vector2f pos = pointAtProgress(sweeperProgress);
		Vector2f next = pointAtProgress(sweeperProgress + 60f);
		float facing = (float) Math.toDegrees(Math.atan2(next.y - pos.y, next.x - pos.x));
		sweeper.getLocation().set(pos);
		sweeper.setFacing(facing);
		sweeper.getVelocity().set((float) Math.cos(Math.toRadians(facing)) * speed,
				(float) Math.sin(Math.toRadians(facing)) * speed);

		if (sweeperProgress >= cumLen[cumLen.length - 1]) {
			lose(player);
			return;
		}

		if (sweeperWeapons != null) {
			for (int i = 0; i < sweeperWeapons.size(); i++) {
				sweeperWeapons.get(i).setForceNoFireOneFrame(true);
			}
		}
	}

	protected void sweeperTick() {
		if (sweeper == null || !sweeper.isAlive()) return;

		engine.addSmoothParticle(sweeper.getLocation(), new Vector2f(),
				150f + (float) Math.random() * 80f, 0.7f, 1.6f, WALL_GLOW);
		engine.spawnExplosion(pointAtProgress(sweeperProgress - 160f), new Vector2f(),
				COURSE_COLOR, 90f, 0.9f);
	}

	protected ShipAPI spawnSweeper() {
		for (int i = 0; i < SWEEPER_HULLS.length; i++) {
			try {
				ShipAPI ship = engine.getFleetManager(1).spawnShipOrWing(SWEEPER_HULLS[i],
						new Vector2f(path.get(0)), 0f, 0.85f, null);
				if (ship == null) continue;
				ship.getMutableStats().getPeakCRDuration().modifyFlat("arondight_loom_sweeper", 100000f);
				ship.setInvalidTransferCommandTarget(true);
				ship.addListener(LoomBCP.IMMORTAL);
				if (i > 0) sweeperWeapons = ship.getAllWeapons();
				ship.setShipSystemDisabled(true);
				return ship;
			} catch (Exception e) {
			}
		}
		return null;
	}

	protected Vector2f pointAtProgress(float progress) {
		if (progress <= 0f) return new Vector2f(path.get(0));
		float total = cumLen[cumLen.length - 1];
		if (progress >= total) return new Vector2f(path.get(path.size() - 1));
		int seg = 1;
		while (seg < cumLen.length && cumLen[seg] < progress) seg++;
		float segStart = cumLen[seg - 1];
		float t = (progress - segStart) / (cumLen[seg] - segStart);
		Vector2f a = path.get(seg - 1);
		Vector2f b = path.get(seg);
		return new Vector2f(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
	}

	protected static final Color WALL_GLOW = new Color(110, 220, 170, 80);

	public void setWallAnchors(List<ShipAPI> ships, List<float[]> homes) {
		wallShips = ships;
		wallHomes = homes;
		wallSet.clear();
		if (ships == null) return;
		for (int i = 0; i < ships.size(); i++) {
			wallSet.put(ships.get(i), Boolean.TRUE);
		}
	}

	protected void rePinWalls() {
		if (wallShips == null) return;
		for (int i = 0; i < wallShips.size(); i++) {
			ShipAPI wall = wallShips.get(i);
			float[] home = wallHomes.get(i);
			wall.getLocation().set(home[0], home[1]);
			wall.getVelocity().set(0f, 0f);
			wall.setFacing(home[2]);
			wall.setAngularVelocity(0f);
			wall.setHitpoints(wall.getMaxHitpoints());
			wall.setHulk(false);
			wall.setStation(true);
			wall.setAlphaMult(1f);
		}
	}

	protected void checkExit(ShipAPI player) {
		if (Misc.getDistance(player.getLocation(), exitGate) > GATE_RADIUS) return;
		nextGate = 1;
		win(player);
	}

	protected void hulkWallsForResolution() {
		if (wallShips == null) return;
		for (int i = 0; i < wallShips.size(); i++) {
			wallShips.get(i).setHulk(true);
		}
	}

	protected void win(ShipAPI player) {
		over = true;
		ArondightQuest.set(ArondightQuest.LOOM_WON);
		LoomZoomLock.restore();
		Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1.2f, 0.8f,
				player.getLocation(), player.getVelocity());
		engine.addFloatingText(new Vector2f(player.getLocation().x, player.getLocation().y + 120f),
				"COURSE COMPLETE", 44f, COURSE_COLOR, player, 1f, 0f);
		for (ShipAPI ship : engine.getShips()) {
			if (ship.getOwner() == 1 && ship.isAlive() && !ship.isFighter()) {
				ship.setHulk(true);
			}
		}
		hulkWallsForResolution();
		engine.setDoNotEndCombat(false);
		engine.endCombat(2f, FleetSide.PLAYER);
		live = null;
	}

	protected void lose(ShipAPI player) {
		over = true;
		live = null;
		LoomZoomLock.restore();
		if (player != null) {
			engine.addFloatingText(new Vector2f(player.getLocation().x, player.getLocation().y + 120f),
					"THE COURSE CLOSES", 44f, Misc.getNegativeHighlightColor(), player, 1f, 0f);
		}
		hulkWallsForResolution();
		engine.setDoNotEndCombat(false);
		engine.endCombat(2f, FleetSide.ENEMY);
	}

	protected String clock(float seconds) {
		int s = (int) Math.max(0f, seconds);
		return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
	}
}
