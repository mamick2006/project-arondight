package arondight.combat.reclaimer;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import arondight.campaign.ArondightQuest;

public class LatticeSurgeScript implements AdvanceableListener {

	protected static final String STAT_ID = "arondight_lattice_surge";

	protected final ShipAPI ship;
	protected boolean systemWasActive = false;
	protected float surgeLeft = 0f;
	protected float cooldownLeft = 0f;

	public LatticeSurgeScript(ShipAPI ship) {
		this.ship = ship;
	}

	@Override
	public void advance(float amount) {
		if (!ship.isAlive()) return;

		boolean active = ship.getSystem() != null && ship.getSystem().isActive();
		boolean rising = active && !systemWasActive;
		systemWasActive = active;

		if (cooldownLeft > 0f) cooldownLeft -= amount;

		if (rising && cooldownLeft <= 0f && surgeLeft <= 0f) {
			surgeLeft = ArondightQuest.surgeDuration();
			cooldownLeft = ArondightQuest.surgeCooldown();
			float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
			Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(), "LATTICE SURGE",
					32f, new Color(150, 230, 195), ship, 12f * timeMult, 2.5f / timeMult,
					1f / timeMult, 0f, 0f, 1f);
		}

		if (surgeLeft > 0f) {
			surgeLeft -= amount;
			ship.getMutableStats().getTimeMult().modifyMult(STAT_ID, ArondightQuest.surgeTimeMult());
			ship.setJitterUnder(this, new Color(150, 230, 195, 120), 1f, 8, 6f);
			if (surgeLeft <= 0f) {
				ship.getMutableStats().getTimeMult().unmodify(STAT_ID);
			}
		}
	}
}
