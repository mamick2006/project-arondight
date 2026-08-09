package arondight.weave.nodes;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import arondight.weave.SpecMutators;

public final class ShoulderUnsealNode extends BaseNode {

    private final String slotId;

    public ShoulderUnsealNode(String id, String name, String description, String slotId,
                              String parentNodeId) {
        super(id, name, description, Cluster.HARDPOINT,
                parentNodeId == null ? null : java.util.Arrays.asList(parentNodeId),
                SpecMutators.CAP_SLOT);
        this.slotId = slotId;
    }

    public String getSlotId() {
        return slotId;
    }

    @Override
    public void mutate(ShipHullSpecAPI spec, ShipVariantAPI variant) {
        SpecMutators.unsealShoulder(spec, slotId);
    }
}
