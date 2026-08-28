package arondight.combat.reclaimer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import arondight.campaign.ArondightQuest;
import arondight.util.ArondightSettings;

public class ReclaimerPhaseScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {

	protected static final String STAT_ID = "arondight_reclaimer_phase";
	protected static final Color TELEGRAPH_COLOR = new Color(150, 255, 200);
	protected static final Color RING_COLOR = new Color(120, 200, 170, 160);

	protected boolean spiralActive() {
		return stage == 1;
	}

	protected boolean braidActive() {
		return stage == 2;
	}

	protected boolean interceptorsActive() {
		return stage == 2;
	}

	protected boolean figureActive() {
		return stage == 3;
	}

	protected boolean swarmActive() {
		return stage == 3;
	}

	protected boolean apertureActive() {
		return stage == 4;
	}

	protected boolean rainActive() {
		return stage == 4;
	}

	protected boolean driftActive() {
		return stage == 4;
	}

	protected boolean cageActive() {
		return stage == 1;
	}

	protected boolean lanceActive() {
		return stage <= 3;
	}

	protected static final float[] AIMED_MULT = { 0f, 1.0f, 0.7778f, 0.6778f, 0f };

	protected final ShipAPI boss;
	protected final Random random = newStormRandom();

	public static Random newStormRandom() {
		long pinned = (long) ArondightQuest.f("arondight_reclaimer_storm_seed", 0f);
		return pinned == 0L ? new Random() : new Random(pinned);
	}

	protected int stage = 1;
	protected int revives = 0;
	protected int interceptCharges = 2;
	protected float regenLeft = 0f;
	protected float regenTarget = 0f;
	protected float regenTotal = 1f;

	protected final IntervalUtil upkeep = new IntervalUtil(0.4f, 0.6f);
	protected final IntervalUtil fanTimer = new IntervalUtil(
			ArondightQuest.bossFanInterval() * 0.9f,
			ArondightQuest.bossFanInterval() * 1.1f);
	protected final IntervalUtil swarmTimer = new IntervalUtil(
			ArondightQuest.bossSwarmInterval() * 0.9f,
			ArondightQuest.bossSwarmInterval() * 1.1f);
	protected final IntervalUtil ringTick = new IntervalUtil(0.45f, 0.55f);

	protected float telegraphLeft = 0f;
	protected int pendingPattern = 0;

	protected float wallClock = ArondightQuest.bossWallGrace();
	protected float wallFormingBonus = 34f;
	protected float wallDoorCenter;
	protected boolean wallAnnounced = false;
	protected int wallEmitLeft = 0;
	protected float wallEmitSeed;
	protected float wallEmitDoor;
	protected float wallEmitDoorHalf;

	protected float aimedClock = 2.5f;

	protected static final int BRAID_ARMS = 4;
	protected static final float BRAID_TICK = 0.18f;
	protected static final float BRAID_STEP_DEG = 1.5f;
	protected float braidStep = 0f;
	protected float braidBearing;
	protected long braidTick = 0;
	protected boolean braidAnnounced = false;

	protected float cageClock = 4f;
	protected float cageConvergeLeft = 0f;

	protected static final int HELD_MAX = 96;
	protected final List<CombatEntityAPI> held = new ArrayList<CombatEntityAPI>();
	protected float heldReleaseLeft = 0f;
	protected float figureClock = 2f;
	protected boolean figureAnnounced = false;
	protected final List<Float> heldRadius = new ArrayList<Float>();

	protected static final int RAIN_SHOTS = 20;
	protected static final int RAIN_SHOTS_CROSS = 10;
	protected static final float RAIN_STANDOFF = 1800f;
	protected static final float RAIN_CROSS_DEG = 70f;
	protected float rainBase;
	protected float rainPrecessed = 0f;
	protected float rainClock = 1f;
	protected boolean rainAnnounced = false;

	protected float driftClock = 2f;

	protected final IntervalUtil addTimer = new IntervalUtil(2.5f, 3.5f);
	protected boolean addsAnnounced = false;

	protected float reviveGraceLeft = 0f;

	protected float fanActiveLeft = 0f;
	protected float fanStep = 0f;
	protected float fanBearing = 0f;
	protected float fanSweepRate = 0f;

	protected float ringRadius = ArondightQuest.bossRingRadius();
	protected boolean fieldAnnounced = false;
	protected boolean closeAnnounced = false;

	protected float spiralStep = 0f;
	protected float spiralBearing;
	protected long spiralTick = 0;
	protected boolean spiralAnnounced = false;

	protected final List<ShipAPI> adds = new ArrayList<ShipAPI>();

	protected Vector2f coreOffset = null;
	protected final List<Vector2f> ringOffsets = new ArrayList<Vector2f>();
	protected float modularEmitRadius = 0f;

	public ReclaimerPhaseScript(ShipAPI boss) {
		this.boss = boss;
		wallDoorCenter = random.nextFloat() * 360f;
		spiralBearing = random.nextFloat() * 360f;
		braidBearing = random.nextFloat() * 360f;
		rainBase = random.nextFloat() * 360f;
		try {
			float maxModuleSlotRadius = 0f;
			for (com.fs.starfarer.api.loading.WeaponSlotAPI slot
					: boss.getHullSpec().getAllWeaponSlotsCopy()) {
				String id = slot.getId();
				if (id != null && id.startsWith("WS_MOD")) {
					maxModuleSlotRadius = Math.max(maxModuleSlotRadius, slot.getLocation().length());
					continue;
				}
				if (id == null || !id.startsWith("WS_EMIT")) continue;
				if ("WS_EMIT_CORE".equals(id)) {
					coreOffset = new Vector2f(slot.getLocation());
				} else {
					ringOffsets.add(new Vector2f(slot.getLocation()));
				}
			}
			if (maxModuleSlotRadius > 0f) {
				modularEmitRadius = maxModuleSlotRadius + 110f;
			}
		} catch (Exception e) {
			coreOffset = null;
			ringOffsets.clear();
			modularEmitRadius = 0f;
		}
	}

	protected Vector2f emitPointAlong(float angleDeg) {
		if (modularEmitRadius > 0f) {
			return pointAt(boss.getLocation(), modularEmitRadius, angleDeg);
		}
		if (coreOffset != null) {
			Vector2f core = shipFrameToWorld(coreOffset);
			return pointAt(core, 45f, angleDeg);
		}
		return pointAt(boss.getLocation(), boss.getCollisionRadius() + 60f, angleDeg);
	}

	protected Vector2f swarmPoint(float angleDeg) {
		if (modularEmitRadius > 0f) {
			return pointAt(boss.getLocation(), modularEmitRadius, angleDeg);
		}
		Vector2f best = null;
		float bestDist = Float.MAX_VALUE;
		for (Vector2f offset : ringOffsets) {
			Vector2f world = shipFrameToWorld(offset);
			float d = angleDist(bearing(boss.getLocation(), world), angleDeg);
			if (d < bestDist) {
				bestDist = d;
				best = world;
			}
		}
		if (best != null) return best;
		return pointAt(boss.getLocation(), boss.getCollisionRadius() + 60f, angleDeg);
	}

	protected Vector2f shipFrameToWorld(Vector2f offset) {
		double rad = Math.toRadians(boss.getFacing());
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);
		return new Vector2f(boss.getLocation().x + offset.x * cos - offset.y * sin,
				boss.getLocation().y + offset.x * sin + offset.y * cos);
	}

	protected ShipAPI resolvePlayer(CombatEngineAPI engine) {
		return engine.getPlayerShip();
	}

	public int getStage() {
		return stage;
	}

	public int getPhase() {
		return stage;
	}

	public int getInterceptCharges() {
		return interceptCharges;
	}

	public boolean isRegenActive() {
		return regenLeft > 0f;
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null || !boss.isAlive()) return;

		if (regenLeft > 0f) {
			advanceRegen(amount);
			return;
		}

		upkeep.advance(amount);
		if (upkeep.intervalElapsed()) {
			upkeepTick();
		}

		ShipAPI player = resolvePlayer(engine);
		if (player == null || !player.isAlive()) return;

		if (reviveGraceLeft > 0f) reviveGraceLeft -= amount;

		advanceField(amount, player);
		advanceSpiral(amount);
		advanceBraid(amount);
		advanceInterceptors(amount);
		advanceWalls(amount);
		advanceAimed(amount, player);
		advanceCage(amount, player);
		advanceFigure(amount);
		advanceSwarm(amount);
		advanceRain(amount, player);
		advanceDrift(amount);

		if (telegraphLeft > 0f) {
			telegraphLeft -= amount;
			boss.setJitter(this, TELEGRAPH_COLOR, 0.6f, 4, 8f);
			if (telegraphLeft <= 0f && pendingPattern > 0) {
				fire(pendingPattern, player);
				pendingPattern = 0;
			}
			return;
		}

		if (fanActiveLeft > 0f) {
			advanceFan(amount, player);
			return;
		}

		if (reviveGraceLeft > 0f) return;
		if (stage == 2) {
			fanTimer.advance(amount);
			if (fanTimer.intervalElapsed()) telegraph(2, "<SWEEP PATTERN>");
		}
	}

	protected static final int SPIRAL_ARMS = 3;
	protected static final float SPIRAL_TICK = 0.10f;
	protected static final float SPIRAL_SPIN_DEG = 8f;
	protected static final float SPIRAL_FLIGHT = 6f;
	protected static final long GATE_CYCLE = 9L, GATE_SKIP_FROM = 7L;

	protected void advanceSpiral(float amount) {
		if (!spiralActive()) return;
		if (!spiralAnnounced) {
			spiralAnnounced = true;
			announce(boss, boss.getLocation(), "<LATTICE SPIRAL>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}
		float tick = SPIRAL_TICK;
		String spiralSpec = "annihilator";
		String st = style();
		if (!"classic".equals(st)) {
			spiralSpec = "arondight_spiral_bolt";
			tick *= 1.35f;
		}
		float stepDeg = SPIRAL_SPIN_DEG * tick;
		spiralStep -= amount;
		int guard = 0;
		CombatEngineAPI engine = Global.getCombatEngine();
		while (spiralStep <= 0f && guard++ < 8) {
			spiralStep += tick;
			spiralBearing += stepDeg;
			spiralTick++;
			for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
				if ((spiralTick + arm * 3L) % GATE_CYCLE >= GATE_SKIP_FROM) continue;
				float angle = spiralBearing + arm * (360f / SPIRAL_ARMS);
				CombatEntityAPI proj = engine.spawnProjectile(boss, null, spiralSpec,
						emitPointAlong(angle), angle, boss.getVelocity());
				extendFlight(proj, SPIRAL_FLIGHT);
			}
		}
	}

	protected void advanceBraid(float amount) {
		if (!braidActive()) return;
		if (!braidAnnounced) {
			braidAnnounced = true;
			announce(boss, boss.getLocation(), "<COUNTER-LATTICE>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}
		String spec = "classic".equals(style()) ? "annihilator" : "arondight_spiral_bolt";
		braidStep -= amount;
		int guard = 0;
		CombatEngineAPI engine = Global.getCombatEngine();
		float spacing = 360f / BRAID_ARMS;
		while (braidStep <= 0f && guard++ < 8) {
			braidStep += BRAID_TICK;
			braidBearing += BRAID_STEP_DEG;
			braidTick++;
			for (int hand = 0; hand < 2; hand++) {
				float base = hand == 0 ? braidBearing : -braidBearing + spacing * 0.5f;
				for (int arm = 0; arm < BRAID_ARMS; arm++) {
					if ((braidTick + arm * 3L + hand * 4L) % GATE_CYCLE >= GATE_SKIP_FROM) continue;
					float angle = base + arm * spacing;
					CombatEntityAPI proj = engine.spawnProjectile(boss, null, spec,
							emitPointAlong(angle), angle, boss.getVelocity());
					extendFlight(proj, SPIRAL_FLIGHT);
				}
			}
		}
	}

	protected void advanceInterceptors(float amount) {
		if (!interceptorsActive()) return;
		if (!addsAnnounced) {
			addsAnnounced = true;
			announce(boss, boss.getLocation(), "<INTERCEPT WING>", 34f, TELEGRAPH_COLOR,
					12f, 2.2f, 1.5f);
		}
		addTimer.advance(amount);
		if (addTimer.intervalElapsed()) spawnAdds();
	}

	protected void announce(ShipAPI attached, Vector2f location, String text, float size,
			Color color, float rise, float duration, float fade) {
		float timeMult = attached.getMutableStats().getTimeMult().getModifiedValue();
		if (timeMult <= 0f) timeMult = 1f;
		Global.getCombatEngine().addFloatingTextAlways(location, text, size, color, attached,
				rise * timeMult, duration / timeMult, fade / timeMult, 0f, 0f, 1f);
	}

	protected void telegraph(int pattern, String announceText) {
		pendingPattern = pattern;
		telegraphLeft = ArondightQuest.bossTelegraphSeconds();
		announce(boss, boss.getLocation(), announceText, 36f, TELEGRAPH_COLOR, 12f, 2.5f, 1.2f);
		Global.getSoundPlayer().playSound("system_phase_cloak_activate", 0.6f, 1.2f,
				boss.getLocation(), boss.getVelocity());
	}

	protected void fire(int pattern, ShipAPI player) {
		if (pattern == 2) {
			startFan(player);
		} else if (pattern == 3) {
			fireSwarm(player);
		}
	}

	protected static final int WALL_SHOTS = 48;
	protected static final float WALL_SPACING = 360f / WALL_SHOTS;
	protected static final float WALL_FLIGHT = 5f;
	protected static final int WALL_EMIT_PER_FRAME = 16;
	protected static final float APERTURE_DEG = 60f;
	protected static final float APERTURE_WALK_DEG = 45f;
	protected static final float APERTURE_CADENCE_MULT = 1.2f;

	public static volatile String styleOverride = null;

	protected String style() {
		String s = styleOverride;
		if (s == null) {
			s = ArondightSettings.getString("arondight_boss_projectile_style", "rods");
		}
		return s;
	}

	protected String wallSpecNow = "hammer";
	protected int wallShotsNow = WALL_SHOTS;
	protected float wallSpacingNow = WALL_SPACING;

	protected void advanceWalls(float amount) {
		if (wallEmitLeft > 0) {
			emitWallShots(WALL_EMIT_PER_FRAME);
		}
		if (!apertureActive()) return;

		wallClock -= amount;
		if (wallClock > 0f) return;
		wallClock += ArondightQuest.bossRadialInterval() * APERTURE_CADENCE_MULT;

		if (!wallAnnounced) {
			wallAnnounced = true;
			announce(boss, boss.getLocation(), "<APERTURE RING>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}

		String st = style();
		wallSpecNow = "rods".equals(st) ? "arondight_wall_rod"
				: "heavy".equals(st) ? "arondight_wall_ball" : "hammer";
		wallShotsNow = "heavy".equals(st) ? 36 : WALL_SHOTS;
		wallSpacingNow = 360f / wallShotsNow;

		float seed = random.nextFloat() * wallSpacingNow;
		queueWallLayer(seed, wallDoorCenter, APERTURE_DEG * 0.5f + wallFormingBonus);
		wallFormingBonus = Math.max(0f, wallFormingBonus - 17f);
		wallDoorCenter += APERTURE_WALK_DEG;
	}

	protected void queueWallLayer(float seed, float doorCenter, float doorHalf) {
		if (wallEmitLeft > 0) {
			emitWallShots(wallShotsNow);
		}
		wallEmitLeft = wallShotsNow;
		wallEmitSeed = seed;
		wallEmitDoor = doorCenter;
		wallEmitDoorHalf = doorHalf;
	}

	protected void emitWallShots(int budget) {
		CombatEngineAPI engine = Global.getCombatEngine();
		while (wallEmitLeft > 0 && budget > 0) {
			int i = wallShotsNow - wallEmitLeft;
			wallEmitLeft--;
			float angle = wallEmitSeed + i * wallSpacingNow;
			if (angleDist(angle, wallEmitDoor) < wallEmitDoorHalf) continue;
			CombatEntityAPI proj = engine.spawnProjectile(boss, null, wallSpecNow,
					emitPointAlong(angle), angle, boss.getVelocity());
			extendFlight(proj, WALL_FLIGHT);
			budget--;
		}
	}

	protected void extendFlight(CombatEntityAPI proj, float flightTime) {
		if (proj instanceof MissileAPI) {
			MissileAPI m = (MissileAPI) proj;
			m.setMaxFlightTime(flightTime);
			m.setArmedWhileFizzling(false);
		}
	}

	protected static final float CAGE_GAP_BIAS = 45f;
	protected static final int CAGE_BOLTS = 24;

	protected void advanceCage(float amount, ShipAPI player) {
		if (cageConvergeLeft > 0f) cageConvergeLeft -= amount;
		if (!cageActive()) return;
		cageClock -= amount;
		if (cageClock > 0f) return;
		cageClock += ArondightQuest.bossCageInterval();
		fireCage(player);
	}

	protected void fireCage(ShipAPI player) {
		CombatEngineAPI engine = Global.getCombatEngine();
		float radius = ArondightQuest.bossCageRadius();
		float speed = ArondightQuest.bossCageSpeed();
		float gapHalf = ArondightQuest.bossCageGap() * 0.5f;
		Vector2f centre = new Vector2f(player.getLocation());
		float tangent = bearing(boss.getLocation(), centre)
				+ (random.nextBoolean() ? 90f : -90f);
		float gapCentre = tangent + (random.nextFloat() - 0.5f) * 2f * CAGE_GAP_BIAS;
		float spacing = 360f / CAGE_BOLTS;
		for (int i = 0; i < CAGE_BOLTS; i++) {
			float outward = i * spacing;
			if (angleDist(outward, gapCentre) < gapHalf) continue;
			Vector2f at = pointAt(centre, radius, outward);
			float inward = outward + 180f;
			if (Misc.getDistance(at, boss.getLocation()) < boss.getCollisionRadius() + 60f) continue;
			engine.spawnProjectile(boss, null, "arondight_cage_bolt", at, inward,
					new Vector2f());
		}
		cageConvergeLeft = radius / Math.max(1f, speed);
		announce(boss, centre, "<CONTAINMENT>", 30f, TELEGRAPH_COLOR, 10f, 2f, 1.2f);
	}

	protected static final int FIGURE_SIDES = 5;
	protected static final int FIGURE_SHOTS = 24;
	protected static final float[] FIGURE_SHELL_MULT = { 1.0f, 0.70f, 0.55f };
	protected static final float FIGURE_EXPANSION_PER_SEC = 0.5f;

	protected void advanceFigure(float amount) {
		if (heldReleaseLeft > 0f) {
			heldReleaseLeft -= amount;
			if (heldReleaseLeft <= 0f) releaseFigure();
			return;
		}
		if (!figureActive()) return;
		if (!figureAnnounced) {
			figureAnnounced = true;
			announce(boss, boss.getLocation(), "<LATTICE FIGURE>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}
		figureClock -= amount;
		if (figureClock > 0f) return;
		figureClock += ArondightQuest.bossFigureInterval();
		spawnFigure();
	}

	protected void spawnFigure() {
		CombatEngineAPI engine = Global.getCombatEngine();
		clearHeld();
		float apothem = ArondightQuest.bossFigureRadius();
		float sector = 360f / FIGURE_SIDES;
		float spacing = 360f / FIGURE_SHOTS;
		float phase = random.nextFloat() * 360f;
		for (int shell = 0; shell < FIGURE_SHELL_MULT.length; shell++) {
			for (int i = 0; i < FIGURE_SHOTS && held.size() < HELD_MAX; i++) {
				float angle = phase + i * spacing;
				float off = ((i * spacing) % sector + sector) % sector - sector * 0.5f;
				float radius = apothem * FIGURE_SHELL_MULT[shell]
						/ (float) Math.cos(Math.toRadians(off));
				Vector2f at = pointAt(boss.getLocation(), radius, angle);
				if (radius < modularEmitRadius + 60f) continue;
				CombatEntityAPI proj = engine.spawnProjectile(boss, null, "arondight_held_mote",
						at, angle, new Vector2f());
				if (proj == null) continue;
				proj.getVelocity().set(0f, 0f);
				held.add(proj);
				heldRadius.add(radius);
			}
		}
		heldReleaseLeft = ArondightQuest.bossHoldSeconds();
	}

	protected void releaseFigure() {
		float k = FIGURE_EXPANSION_PER_SEC;
		for (int i = 0; i < held.size(); i++) {
			CombatEntityAPI proj = held.get(i);
			if (proj == null || !isProjectileLive(proj)) continue;
			float outward = bearing(boss.getLocation(), proj.getLocation());
			float speed = heldRadius.get(i) * k;
			double rad = Math.toRadians(outward);
			proj.getVelocity().set((float) Math.cos(rad) * speed,
					(float) Math.sin(rad) * speed);
			proj.setFacing(outward);
		}
		clearHeld();
	}

	protected void advanceSwarm(float amount) {
		if (!swarmActive()) return;
		if (reviveGraceLeft > 0f) return;
		swarmTimer.advance(amount);
		if (swarmTimer.intervalElapsed()) telegraph(3, "<RECLAMATION SWARM>");
	}

	protected void advanceRain(float amount, ShipAPI player) {
		if (!rainActive()) return;
		if (!rainAnnounced) {
			rainAnnounced = true;
			announce(boss, boss.getLocation(), "<HARVEST TIDE>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}
		rainPrecessed += ArondightQuest.bossRainPrecess() * amount;
		rainClock -= amount;
		if (rainClock > 0f) return;
		rainClock += ArondightQuest.bossRainInterval();
		emitRank(player, rainBase + rainPrecessed, RAIN_SHOTS);
		emitRank(player, rainBase + RAIN_CROSS_DEG - rainPrecessed, RAIN_SHOTS_CROSS);
	}

	protected void emitRank(ShipAPI player, float heading, int shots) {
		CombatEngineAPI engine = Global.getCombatEngine();
		double rad = Math.toRadians(heading);
		float ux = (float) Math.cos(rad), uy = (float) Math.sin(rad);
		float spacing = ArondightQuest.bossRainSpacing();
		Vector2f centre = new Vector2f(player.getLocation().x - ux * RAIN_STANDOFF,
				player.getLocation().y - uy * RAIN_STANDOFF);
		String spec = "heavy".equals(style()) ? "arondight_wall_ball" : "arondight_wall_rod";
		for (int i = 0; i < shots; i++) {
			float off = (i - (shots - 1) * 0.5f) * spacing;
			Vector2f at = new Vector2f(centre.x - uy * off, centre.y + ux * off);
			if (Misc.getDistance(at, boss.getLocation()) < boss.getCollisionRadius() + 60f) continue;
			CombatEntityAPI proj = engine.spawnProjectile(boss, null, spec, at, heading,
					new Vector2f());
			extendFlight(proj, WALL_FLIGHT);
		}
	}

	protected boolean isProjectileLive(CombatEntityAPI proj) {
		CombatEngineAPI engine = Global.getCombatEngine();
		return !proj.isExpired() && (engine == null || engine.isEntityInPlay(proj));
	}

	protected void clearHeld() {
		held.clear();
		heldRadius.clear();
	}

	protected void advanceDrift(float amount) {
		if (!driftActive()) return;
		driftClock -= amount;
		if (driftClock > 0f) return;
		driftClock += ArondightQuest.bossDriftInterval();
		CombatEngineAPI engine = Global.getCombatEngine();
		int motes = 6;
		for (int i = 0; i < motes; i++) {
			float angle = random.nextFloat() * 360f;
			engine.spawnProjectile(boss, null, "arondight_drift_mote",
					emitPointAlong(angle), angle, new Vector2f());
		}
	}

	protected void advanceAimed(float amount, ShipAPI player) {
		if (!lanceActive()) return;
		if (cageConvergeLeft > 0f) return;
		aimedClock -= amount;
		if (aimedClock > 0f) return;
		aimedClock += ArondightQuest.bossAimedInterval() * AIMED_MULT[stage];

		CombatEngineAPI engine = Global.getCombatEngine();
		float toPlayer = bearing(boss.getLocation(), player.getLocation());
		engine.spawnProjectile(boss, null, "arondight_aimed_lance",
				emitPointAlong(toPlayer), toPlayer, boss.getVelocity());
	}

	protected void startFan(ShipAPI player) {
		fanActiveLeft = 2.8f;
		fanStep = 0f;
		fanSweepRate = 55f * (random.nextBoolean() ? 1f : -1f);
		fanBearing = bearing(boss.getLocation(), player.getLocation())
				+ (fanSweepRate > 0f ? 25f : -25f);
	}

	protected void advanceFan(float amount, ShipAPI player) {
		CombatEngineAPI engine = Global.getCombatEngine();
		fanActiveLeft -= amount;
		fanBearing += fanSweepRate * amount;
		fanStep -= amount;
		if (fanStep <= 0f) {
			fanStep = 0.13f;
			for (int spoke = -3; spoke <= 3; spoke++) {
				float angle = fanBearing + spoke * 14f;
				engine.spawnProjectile(boss, null, "arondight_comb_slug",
						emitPointAlong(angle), angle, boss.getVelocity());
			}
		}
	}

	protected void spawnAdds() {
		Iterator<ShipAPI> iter = adds.iterator();
		while (iter.hasNext()) {
			ShipAPI add = iter.next();
			if (add == null || !add.isAlive()) iter.remove();
		}
		if (adds.size() >= 3) return;

		CombatEngineAPI engine = Global.getCombatEngine();
		float angle = random.nextFloat() * 360f;
		Vector2f point = pointAt(boss.getLocation(), boss.getCollisionRadius() + 300f, angle);
		ShipAPI add = engine.getFleetManager(1).spawnShipOrWing("attack_swarm_Attack", point, angle, 0.7f, null);
		if (add != null) {
			adds.add(add);
			announce(add, point, "<INTERCEPTORS>", 28f, TELEGRAPH_COLOR, 10f, 2f, 1f);
		}
	}

	protected static final float[] BRACKETS = { -16f, 16f };
	protected final List<MissileAPI> guided = new ArrayList<MissileAPI>();
	protected final List<MissileAPI> guidedHeavy = new ArrayList<MissileAPI>();

	protected int pruneGuided() {
		pruneDead(guided);
		pruneDead(guidedHeavy);
		return guided.size();
	}

	protected static void pruneDead(List<MissileAPI> list) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Iterator<MissileAPI> iter = list.iterator();
		while (iter.hasNext()) {
			MissileAPI m = iter.next();
			if (m == null || m.isExpired()
					|| (engine != null && !engine.isEntityInPlay(m))) {
				iter.remove();
			}
		}
	}

	protected boolean spawnGuided(String spec, float angle, float flight, boolean heavy) {
		if (guided.size() >= ArondightQuest.bossGuidedCap()) return false;
		if (heavy && guidedHeavy.size() >= ArondightQuest.bossGuidedHeavyCap()) return false;
		CombatEntityAPI proj = Global.getCombatEngine().spawnProjectile(boss, null, spec,
				swarmPoint(angle), angle, boss.getVelocity());
		extendFlight(proj, flight);
		if (proj instanceof MissileAPI) {
			MissileAPI m = (MissileAPI) proj;
			guided.add(m);
			if (heavy) guidedHeavy.add(m);
		}
		return true;
	}

	protected void fireSwarm(ShipAPI player) {
		CombatEngineAPI engine = Global.getCombatEngine();
		float toPlayer = bearing(boss.getLocation(), player.getLocation());
		pruneGuided();
		spawnGuided("arondight_harrier_missile",
				toPlayer + (random.nextFloat() - 0.5f) * 70f, 4.0f, true);
		for (int i = 0; i < 6; i++) {
			float angle = toPlayer + (random.nextFloat() - 0.5f) * 140f;
			if (!spawnGuided("swarmer", angle, 3.5f, false)) break;
		}
		for (float bracket : BRACKETS) {
			CombatEntityAPI torp = engine.spawnProjectile(boss, null, "arondight_bracket_torp",
					swarmPoint(toPlayer + bracket), toPlayer + bracket, boss.getVelocity());
			extendFlight(torp, 12f);
		}
		for (int i = 0; i < 24; i++) {
			float mag = 10f + (i % 12) * 3f;
			float angle = toPlayer + (i < 12 ? -mag : mag);
			CombatEntityAPI proj = engine.spawnProjectile(boss, null, "annihilator",
					swarmPoint(angle), angle, boss.getVelocity());
			extendFlight(proj, 4f);
		}
	}

	protected void advanceField(float amount, ShipAPI player) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!fieldAnnounced) {
			fieldAnnounced = true;
			announce(boss, boss.getLocation(), "<HARVESTING FIELD>", 36f, TELEGRAPH_COLOR,
					12f, 2.5f, 2f);
		}

		ringTick.advance(amount);
		if (!ringTick.intervalElapsed()) return;

		for (int i = 0; i < 7; i++) {
			float angle = random.nextFloat() * 360f;
			Vector2f point = pointAt(boss.getLocation(), ringRadius, angle);
			engine.spawnExplosion(point, new Vector2f(), RING_COLOR, 60f, 0.7f);
		}

		float dist = Misc.getDistance(boss.getLocation(), player.getLocation());
		if (dist > ringRadius) {
			engine.applyDamage(player, player.getLocation(), 90f, DamageType.ENERGY, 90f,
					true, false, boss);
			engine.spawnEmpArcVisual(pointAt(player.getLocation(), 320f,
					bearing(boss.getLocation(), player.getLocation())), null,
					player.getLocation(), player, 14f,
					new Color(120, 255, 190, 220), new Color(235, 255, 245, 255));
		}
	}

	protected void upkeepTick() {
		boss.getMutableStats().getPeakCRDuration().modifyFlat(STAT_ID, 100000f);
		if (boss.getCurrentCR() < 0.7f) boss.setCurrentCR(0.7f);
		advanceStage();
	}

	protected void advanceStage() {
		float edgeHp = boss.getMaxHitpoints() * ArondightQuest.bossStage2Hull();
		int byHull = boss.getHitpoints() <= edgeHp ? 2 : 1;
		int byDeaths = 2 + revives;
		int newStage = Math.max(byHull, revives > 0 ? byDeaths : byHull);
		if (newStage > stage) {
			stage = newStage;
			onStageEntered();
		}
	}

	protected void onStageEntered() {
		announceStage();
		if (apertureActive()) {
			wallAnnounced = false;
			wallFormingBonus = 34f;
			wallClock = ArondightQuest.bossWallGrace();
		}
		spiralAnnounced = false;
		braidAnnounced = false;
		addsAnnounced = false;
		figureAnnounced = false;
		rainAnnounced = false;
	}

	protected void announceStage() {
		String text = stage == 2 ? "<PATTERN SHIFT>"
				: stage == 3 ? "<RECLAMATION PRIORITY>" : "<TERMINAL LATTICE>";
		announce(boss, boss.getLocation(), text, 40f, TELEGRAPH_COLOR, 12f, 2.5f, 2f);
	}

	@Override
	public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
		if (regenLeft > 0f) return true;
		if (stage < 2 && damageAmount >= ship.getHitpoints()) {
			ship.setHitpoints(ship.getMaxHitpoints() * ArondightQuest.bossStage2Hull());
			advanceStage();
			return true;
		}
		if (damageAmount >= ship.getHitpoints() && interceptCharges > 0) {
			interceptCharges--;
			revives++;
			regenTarget = interceptCharges == 1
					? ArondightQuest.bossRegenHullFraction()
					: ArondightQuest.bossRegenHullFraction2();
			regenLeft = ArondightQuest.bossRegenWindow();
			regenTotal = regenLeft;
			ship.setHitpoints(1f);
			ship.getMutableStats().getHullDamageTakenMult().modifyMult(STAT_ID, 0.000001f);
			raiseInvulnShield();
			announce(ship, ship.getLocation(), "<RECLAMATION INTERRUPTED>", 40f,
					Misc.getNegativeHighlightColor(), 12f, 2.5f, 2.5f);
			Global.getSoundPlayer().playSound("system_phase_cloak_activate", 0.8f, 0.7f,
					ship.getLocation(), ship.getVelocity());
			pendingPattern = 0;
			telegraphLeft = 0f;
			fanActiveLeft = 0f;
			wallEmitLeft = 0;
			return true;
		}
		return false;
	}

	protected void raiseInvulnShield() {
		boss.setShield(ShieldAPI.ShieldType.OMNI, 0f, 0f, 360f);
		if (boss.getShield() != null) {
			boss.getShield().toggleOn();
		}
	}

	protected void dropInvulnShield() {
		if (boss.getShield() != null) {
			boss.getShield().toggleOff();
		}
		boss.setShield(ShieldAPI.ShieldType.NONE, 0f, 0f, 0f);
	}

	protected void advanceRegen(float amount) {
		regenLeft -= amount;
		float progress = 1f - Math.max(0f, regenLeft) / regenTotal;
		float target = boss.getMaxHitpoints() * regenTarget;
		boss.setHitpoints(Math.max(1f, Misc.interpolate(1f, target, progress)));
		boss.getFluxTracker().setHardFlux(0f);
		boss.giveCommand(ShipCommand.DECELERATE, null, 0);
		boss.setJitter(this, TELEGRAPH_COLOR, 0.9f, 6, 14f);
		if (boss.getShield() != null && boss.getShield().isOff()) {
			boss.getShield().toggleOn();
		}

		if (regenLeft <= 0f) {
			boss.getMutableStats().getHullDamageTakenMult().unmodify(STAT_ID);
			dropInvulnShield();
			clearSky();
			reviveGraceLeft = ArondightQuest.bossReviveGrace();
			upkeepTick();
		}
	}

	protected void clearSky() {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) return;
		List<CombatEntityAPI> doomed = new ArrayList<CombatEntityAPI>();
		for (Object o : engine.getProjectiles()) {
			CombatEntityAPI e = (CombatEntityAPI) o;
			if (e.getOwner() == boss.getOwner()) doomed.add(e);
		}
		for (Object o : engine.getMissiles()) {
			CombatEntityAPI e = (CombatEntityAPI) o;
			if (e.getOwner() == boss.getOwner()) doomed.add(e);
		}
		for (CombatEntityAPI e : doomed) {
			engine.removeEntity(e);
		}
		held.clear();
		guided.clear();
		guidedHeavy.clear();
		wallEmitLeft = 0;
		heldReleaseLeft = 0f;
		cageConvergeLeft = 0f;
		heldRadius.clear();
	}

	protected static Vector2f pointAt(Vector2f center, float radius, float angleDeg) {
		double rad = Math.toRadians(angleDeg);
		return new Vector2f(center.x + (float) (Math.cos(rad) * radius),
				center.y + (float) (Math.sin(rad) * radius));
	}

	protected static float bearing(Vector2f from, Vector2f to) {
		return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
	}

	protected static float angleDist(float a, float b) {
		float d = Math.abs(((a - b) % 360f + 360f) % 360f);
		return d > 180f ? 360f - d : d;
	}
}
