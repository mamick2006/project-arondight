package arondight.weave;

import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class ArondightWeave {

    private ArondightWeave() {}

    public static List<ArondightNode> getAllNodes() {
        return ArondightNodes.all();
    }

    public static ArondightNode getNode(String nodeId) {
        return ArondightNodes.byId(nodeId);
    }

    public enum Quadrant {
        NW("Frame"), NE("System"), SE("Hardpoints"), SW("Lattice");

        private final String label;

        Quadrant(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static String getRootNodeId() {
        return arondight.ArondightIds.NODE_ROOT;
    }

    public static String getParentNodeId(String nodeId) {
        ArondightNode node = ArondightNodes.byId(nodeId);
        if (node == null || node.getPrereqAnyOf().isEmpty()) return null;
        return node.getPrereqAnyOf().get(0);
    }

    public static List<String> getChildNodeIds(String nodeId) {
        if (nodeId == null) return Collections.emptyList();
        java.util.List<String> children = new java.util.ArrayList<>();
        for (ArondightNode n : ArondightNodes.all()) {
            if (nodeId.equals(getParentNodeId(n.getId()))) children.add(n.getId());
        }
        return children;
    }

    public static Quadrant getQuadrant(String nodeId) {
        ArondightNode node = ArondightNodes.byId(nodeId);
        if (node == null) return null;
        switch (node.getCluster()) {
            case FRAME: return Quadrant.NW;
            case SYSTEM: return Quadrant.NE;
            case HARDPOINT: return Quadrant.SE;
            case LATTICE: return Quadrant.SW;
            default: return null;
        }
    }

    public static String getWeaveBlockReason(FleetMemberAPI member, String nodeId) {
        return WeaveActions.getWeaveBlockReason(member, nodeId);
    }

    public static void healOnGameLoad() {
        WeaveActions.healOnGameLoad();
    }

    public static boolean weaveNode(FleetMemberAPI member, String nodeId, TextPanelAPI textPanel) {
        return WeaveActions.weaveNode(member, nodeId, textPanel);
    }

    public static void apply(FleetMemberAPI member) {
        WeaveApplier.apply(member);
    }

    public static void reapplyPlayerFleetIfNeeded() {
        WeaveApplier.reapplyPlayerFleetIfNeeded();
    }

    public static void applyNodesToVariant(com.fs.starfarer.api.combat.ShipVariantAPI variant,
                                           java.util.Collection<String> nodeIds) {
        WeaveApplier.applyNodesToVariant(variant, nodeIds);
    }

    public static void refreshMounts(com.fs.starfarer.api.combat.ShipVariantAPI variant) {
        if (variant == null) return;
        com.fs.starfarer.api.combat.ShipHullSpecAPI spec = variant.getHullSpec();
        if (spec == null || !spec.getTags().contains(arondight.ArondightIds.TAG_APPLIED)) return;
        WeaveApplier.unhideAnimatedFits(spec, variant);
    }

    public static void snapshotShoulderFits() {
        WeaveApplier.snapshotShoulderFits();
    }

    public static java.util.Set<String> readWovenTags(com.fs.starfarer.api.combat.ShipHullSpecAPI spec) {
        if (spec == null) return java.util.Collections.emptySet();
        java.util.Set<String> nodes = new java.util.LinkedHashSet<>();
        for (String tag : spec.getTags()) {
            if (tag.startsWith(arondight.ArondightIds.TAG_WEAVE_PREFIX)) {
                nodes.add(tag.substring(arondight.ArondightIds.TAG_WEAVE_PREFIX.length()));
            }
        }
        return nodes;
    }
}
