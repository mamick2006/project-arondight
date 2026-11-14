package arondight.combat.reclaimer;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import arondight.campaign.ArondightQuest;
import arondight.combat.LethalSaveScript;

public class ReclaimerFightPlugin extends BaseEveryFrameCombatPlugin {

	protected static final String STAT_ID = "arondight_reclaimer_station";

	protected CombatEngineAPI engine;
	protected final IntervalUtil interval = new IntervalUtil(0.4f, 0.6f);

	protected ShipAPI boss = null;
	protected ReclaimerPhaseScript brain = null;
	protected LethalSaveScript lethalSave = null;
	protected boolean over = false;

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
		engine.setDoNotEndCombat(true);
		ArondightQuest.set(ArondightQuest.DUEL_STARTED);
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused() || over) return;

		ShipAPI player = engine.getPlayerShip();
		if (player != null && lethalSave == null) {
			lethalSave = new LethalSaveScript(player, "THE FIELD TAKES HOLD");
			player.addListener(lethalSave);
			player.addListener(new LatticeSurgeScript(player));
		}
		if (lethalSave != null && lethalSave.isTriggered()) {
			over = true;
			return;
		}

		interval.advance(amount);
		if (!interval.intervalElapsed()) {
			maintainHud();
			return;
		}

		if (boss == null) {
			for (ShipAPI ship : engine.getShips()) {
				if (ship.getOwner() != 1 || ship.isFighter() || !ship.isAlive()) continue;
				if (ship.getParentStation() != null || ship.isStationModule()) continue;
				if (boss == null || ship.getMaxHitpoints() > boss.getMaxHitpoints()) {
					boss = ship;
				}
			}
			if (boss != null) {
				pinAsStation(boss);
				enableHullDamage(boss);
				for (ShipAPI arm : boss.getChildModulesCopy()) {
					enableHullDamage(arm);
				}
				brain = new ReclaimerPhaseScript(boss);
				boss.addListener(brain);
			}
		}

		if (boss != null && !boss.isAlive()) {
			win();
			return;
		}

		if (boss != null && boss.getLocation().length() > 30f) {
			boss.getLocation().set(0f, 0f);
			boss.getVelocity().set(0f, 0f);
		}

		if (engine.getFleetManager(0).getTaskManager(false).isInFullRetreat()) {
			over = true;
			engine.setDoNotEndCombat(false);
			engine.endCombat(3f, FleetSide.ENEMY);
			return;
		}

		maintainHud();
	}

	protected void pinAsStation(ShipAPI boss) {
		boss.getLocation().set(0f, 0f);
		boss.getVelocity().set(0f, 0f);
		boss.getMutableStats().getMaxSpeed().modifyMult(STAT_ID, 0f);
		boss.getMutableStats().getAcceleration().modifyMult(STAT_ID, 0f);
		boss.getMutableStats().getDeceleration().modifyMult(STAT_ID, 0f);
		boss.getMutableStats().getTurnAcceleration().modifyMult(STAT_ID, 0f);
		boss.getMutableStats().getMaxTurnRate().modifyMult(STAT_ID, 0f);
	}

	protected void enableHullDamage(ShipAPI ship) {
		ship.getMutableStats().getHullDamageTakenMult().unmodify("vastbulk");
	}

	protected static final String[] STAGE_NUMERALS = { "I", "I", "II", "III", "IV" };

	protected void maintainHud() {
		if (boss == null || !boss.isAlive()) return;
		int pct = (int) (100f * boss.getHitpoints() / boss.getMaxHitpoints());
		int stage = brain == null ? 1 : brain.getStage();
		if (stage < 1) stage = 1;
		if (stage > STAGE_NUMERALS.length - 1) stage = STAGE_NUMERALS.length - 1;
		engine.maintainStatusForPlayerShip("arondight_duel_status",
				"graphics/icons/intel/discovered_entity.png", "RECLAIMER",
				"HULL " + pct + "%   PATTERN " + STAGE_NUMERALS[stage], false);
	}

	protected void win() {
		over = true;
		ArondightQuest.set(ArondightQuest.DUEL_WON);
		ShipAPI player = engine.getPlayerShip();
		if (player != null) {
			engine.addFloatingText(new Vector2f(player.getLocation().x, player.getLocation().y + 120f),
					"RECLAMATION DENIED", 44f, new java.awt.Color(150, 230, 195), player, 1f, 0f);
		}
		for (ShipAPI ship : engine.getShips()) {
			if (ship.getOwner() == 1 && ship.isAlive() && !ship.isFighter()) {
				ship.setHulk(true);
			}
		}
		engine.setDoNotEndCombat(false);
		engine.endCombat(3f, FleetSide.PLAYER);
	}
}
