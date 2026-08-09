package arondight.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import arondight.util.ArondightSettings;

public class ReclaimerAttenuation extends BaseHullMod {

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float mult = ArondightSettings.getFloat("arondight_boss_damage_mult", 0.5f);
		stats.getDamageToFighters().modifyMult(id, mult);
		stats.getDamageToMissiles().modifyMult(id, mult);
		stats.getDamageToFrigates().modifyMult(id, mult);
		stats.getDamageToDestroyers().modifyMult(id, mult);
		stats.getDamageToCruisers().modifyMult(id, mult);
		stats.getDamageToCapital().modifyMult(id, mult);
	}
}
