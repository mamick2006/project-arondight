package arondight.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;

import arondight.util.ArondightSettings;

public class LonginusFrame extends FormBaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && ship.getListeners(RangeMult.class).isEmpty()) {
            ship.addListener(new RangeMult());
        }
    }

    public static class RangeMult implements WeaponBaseRangeModifier {
        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.isDecorative()) return 1f;
            return ArondightSettings.getFloat("arondight_longinus_range_mult", 3f);
        }

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
    }
}
