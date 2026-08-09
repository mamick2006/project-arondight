package arondight.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

import arondight.form.mechanics.ClarentDrainPlugin;

public class ClarentFrame extends FormBaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && ship.getListeners(ClarentDrainPlugin.class).isEmpty()) {
            ship.addListener(new ClarentDrainPlugin(ship));
        }
    }
}
