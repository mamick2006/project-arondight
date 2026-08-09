package arondight.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

import arondight.form.mechanics.GaeBolgPlugin;

public class GaeBolgFrame extends FormBaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        if (ship.getListeners(GaeBolgPlugin.class).isEmpty()) {
            ship.addListener(new GaeBolgPlugin(ship));
        }
        if (ship.getListeners(RangeDoubler.class).isEmpty()) {
            ship.addListener(new RangeDoubler());
        }
    }
}
