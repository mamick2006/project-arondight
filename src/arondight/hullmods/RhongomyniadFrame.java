package arondight.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

import arondight.form.mechanics.RhongomyniadAnchor;

public class RhongomyniadFrame extends FormBaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        if (ship.getListeners(RhongomyniadAnchor.class).isEmpty()) {
            ship.addListener(new RhongomyniadAnchor(ship));
        }
        if (ship.getListeners(RangeDoubler.class).isEmpty()) {
            ship.addListener(new RangeDoubler());
        }
    }
}
