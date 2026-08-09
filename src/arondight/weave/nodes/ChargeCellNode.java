package arondight.weave.nodes;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightIds;
import arondight.state.ArondightState;

public final class ChargeCellNode extends BaseNode {

    private static final String SPOTTER_FACT = " The spotter lattice holds no charges.";

    public ChargeCellNode(String name, String description) {
        super(ArondightIds.NODE_CHARGE_CELL, name, description, Cluster.SYSTEM,
                java.util.Arrays.asList(ArondightIds.NODE_QUICK_CYCLE));
    }

    @Override
    public String getDescription() {
        String base = super.getDescription();
        FleetMemberAPI frame = ArondightState.findPlayerFrame();
        if (frame == null) return base;
        String form = ArondightState.getCurrentForm(frame.getId());
        if (form == null) return base;
        try {
            ShipHullSpecAPI spec = com.fs.starfarer.api.Global.getSettings().getHullSpec(form);
            if (spec != null && ArondightIds.SYSTEM_SPOTTER.equals(spec.getShipSystemId())) {
                return base + SPOTTER_FACT;
            }
        } catch (Exception ignored) {
        }
        return base;
    }
}
