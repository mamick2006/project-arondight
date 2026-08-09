package arondight.form.mechanics;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class WardSystemScript extends BaseShipSystemScript {

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        cancelPhasing(ship);
        WardMechanics ward = WardMechanics.of(ship);
        if (ward != null) ward.updateSystemState(state, effectLevel);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        cancelPhasing(ship);
        WardMechanics ward = WardMechanics.of(ship);
        if (ward != null) ward.updateSystemState(State.IDLE, 0f);
    }

    static void cancelPhasing(ShipAPI ship) {
        if (ship.isPhased()) ship.setPhased(false);
        if (ship.getCollisionClass() == CollisionClass.NONE) {
            ship.setCollisionClass(CollisionClass.SHIP);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && (state == State.IN || state == State.ACTIVE)) {
            return new StatusData("ward raised", false);
        }
        return null;
    }
}
