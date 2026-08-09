package arondight.weave.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import arondight.weave.ArondightNode;
import arondight.weave.SpecMutators;

public abstract class BaseNode implements ArondightNode {

    private final String id;
    private final String name;
    private final String description;
    private final Cluster cluster;
    private final List<String> prereqAnyOf;
    private final List<String> capabilities;

    protected BaseNode(String id, String name, String description, Cluster cluster,
                       List<String> prereqAnyOf, String... capabilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cluster = cluster;
        this.prereqAnyOf = prereqAnyOf == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(prereqAnyOf);
        this.capabilities = Arrays.asList(capabilities);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public List<String> getPrereqAnyOf() {
        return prereqAnyOf;
    }

    @Override
    public boolean isAvailable() {
        if (!SpecMutators.isWorking(SpecMutators.CAP_CLONE)) return false;
        for (String cap : capabilities) {
            if (!SpecMutators.isWorking(cap)) return false;
        }
        return true;
    }

    @Override
    public void mutate(ShipHullSpecAPI clonedSpec, ShipVariantAPI variant) {
    }
}
