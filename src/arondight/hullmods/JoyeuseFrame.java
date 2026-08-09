package arondight.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

import arondight.form.mechanics.WardMechanics;

public class JoyeuseFrame extends FormBaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && WardMechanics.of(ship) == null) {
            ship.addListener(new WardMechanics(ship, WardMechanics.Variant.BULWARK));
        }
    }
}
