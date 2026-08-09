package arondight.weave;

import java.util.List;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public interface ArondightNode {

    enum Cluster { CORE, FRAME, HARDPOINT, SYSTEM, LATTICE }

    String getId();

    String getName();

    String getDescription();

    Cluster getCluster();

    List<String> getPrereqAnyOf();

    boolean isAvailable();

    void mutate(ShipHullSpecAPI clonedSpec, ShipVariantAPI variant);
}
