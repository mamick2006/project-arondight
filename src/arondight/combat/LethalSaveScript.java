package arondight.combat;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

public class LethalSaveScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {

	protected static final String STAT_ID = "arondight_lethal_save";
	protected static final float WINDOW = 2f;

	protected final ShipAPI ship;
	protected final String floatText;
	protected boolean triggered = false;
	protected float timer = 0f;

	public LethalSaveScript(ShipAPI ship, String floatText) {
		this.ship = ship;
		this.floatText = floatText;
	}

	public boolean isTriggered() {
		return triggered;
	}

	@Override
	public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
		if (!triggered && damageAmount >= ship.getHitpoints()) {
			triggered = true;
			ship.setHitpoints(1f);
			ship.getMutableStats().getHullDamageTakenMult().modifyMult(STAT_ID, 0.000001f);
			float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
			Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(), floatText,
					40f, new Color(160, 220, 190), ship, 16f * timeMult, 3.2f / timeMult,
					2f / timeMult, 0f, 0f, 1f);
			Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f,
					ship.getLocation(), ship.getVelocity());
			Global.getCombatEngine().setDoNotEndCombat(false);
			Global.getCombatEngine().endCombat(WINDOW, FleetSide.ENEMY);
			return true;
		}
		return triggered;
	}

	@Override
	public void advance(float amount) {
		if (!triggered) return;
		timer += amount;
		if (timer < WINDOW * 2f) {
			ship.getMutableStats().getHullDamageTakenMult().modifyMult(STAT_ID, 0.000001f);
		} else {
			ship.getMutableStats().getHullDamageTakenMult().unmodify(STAT_ID);
		}
	}
}
