package arondight.weave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightNode.Cluster;
import arondight.weave.nodes.PassiveNode;
import arondight.weave.nodes.ShoulderUnsealNode;
import arondight.weave.nodes.SpecStatNode;
import arondight.weave.nodes.SpecStatNode.Op;

final class ArondightNodes {

    private static List<ArondightNode> ALL;
    private static Map<String, ArondightNode> BY_ID;

    private ArondightNodes() {}

    static List<ArondightNode> all() {
        ensure();
        return ALL;
    }

    static ArondightNode byId(String nodeId) {
        ensure();
        return nodeId == null ? null : BY_ID.get(nodeId);
    }

    private static synchronized void ensure() {
        if (ALL != null) return;

        float hullMult = ArondightSettings.getFloat("arondight_node_hull_mult", 1.30f);
        float armorFlat = ArondightSettings.getFloat("arondight_node_armor_flat", 80f);
        float fluxCapMult = ArondightSettings.getFloat("arondight_node_fluxcap_mult", 1.25f);
        float fluxDissMult = ArondightSettings.getFloat("arondight_node_fluxdiss_mult", 1.15f);
        float speedFlat = ArondightSettings.getFloat("arondight_node_speed_flat", 20f);
        float maneuverMult = ArondightSettings.getFloat("arondight_node_maneuver_mult", 1.5f);
        int opFlat = ArondightSettings.getInt("arondight_node_op_flat", 3);
        int loomSecs = Math.round(1f
                / Math.max(0.001f, ArondightSettings.getFloat("arondight_loom_ammo_per_sec", 0.04f)));
        int watchdogRespawn = Math.round(
                ArondightSettings.getFloat("arondight_watchdog_respawn", 10f));
        int selfweavePer10s = Math.round(
                ArondightSettings.getFloat("arondight_selfweave_rate", 0.005f) * 1000f);
        int selfweaveCap = Math.round(
                ArondightSettings.getFloat("arondight_selfweave_cap", 1.0f) * 100f);
        int afterThresh = Math.round(
                ArondightSettings.getFloat("arondight_afterimage_threshold", 0.05f) * 100f);
        int afterDist = Math.round(ArondightSettings.getFloat("arondight_afterimage_dist", 250f));
        int afterCd = Math.round(ArondightSettings.getFloat("arondight_afterimage_cooldown", 12f));
        int bleedRange = Math.round(ArondightSettings.getFloat("arondight_bleed_range", 500f));
        int bleedDamage = Math.round(ArondightSettings.getFloat("arondight_bleed_damage", 110f));
        int harvestHull = Math.round(
                ArondightSettings.getFloat("arondight_harvest_hull", 0.10f) * 100f);
        int harvestArmor = Math.round(
                ArondightSettings.getFloat("arondight_harvest_armor", 0.10f) * 100f);
        int harvestCr = Math.round(
                ArondightSettings.getFloat("arondight_harvest_cr", 0.02f) * 100f);
        int salvageBonus = Math.round(
                ArondightSettings.getFloat("arondight_salvage_bonus", 0.2f) * 100f);
        int zeroPoint = Math.round(
                ArondightSettings.getFloat("arondight_zero_point_flux_level", 0.15f) * 100f);
        int actuators = Math.round(ArondightSettings.getFloat("arondight_actuators_bonus", 25f));
        int bufferPct = Math.round(
                ArondightSettings.getFloat("arondight_buffer_fraction", 0.25f) * 100f);
        int faradayPct = Math.round((1f
                - ArondightSettings.getFloat("arondight_faraday_emp_mult", 0.5f)) * 100f);
        int dockPct = Math.round(ArondightSettings.getFloat("arondight_dock_cr_recovery_pct", 100f));
        int purgePct = Math.round(ArondightSettings.getFloat("arondight_purge_vent_pct", 25f));

        List<ArondightNode> nodes = new ArrayList<>();

        nodes.add(new PassiveNode(ArondightIds.NODE_ROOT,
                ArondightStrings.weave("node_root_name"),
                ArondightStrings.weave("node_root_desc"),
                Cluster.CORE, null));

        nodes.add(new SpecStatNode(ArondightIds.NODE_REINFORCED_WEAVE,
                ArondightStrings.weave("node_reinforced_weave_name"),
                ArondightStrings.weave("node_reinforced_weave_desc", pct(hullMult)),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_ROOT), Op.HULL_MULT, hullMult));
        nodes.add(new SpecStatNode(ArondightIds.NODE_DENSE_PLATING,
                ArondightStrings.weave("node_dense_plating_name"),
                ArondightStrings.weave("node_dense_plating_desc", (int) armorFlat),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_REINFORCED_WEAVE),
                Op.ARMOR_FLAT, armorFlat));
        nodes.add(new SpecStatNode(ArondightIds.NODE_REACTOR_BLOOM,
                ArondightStrings.weave("node_reactor_bloom_name"),
                ArondightStrings.weave("node_reactor_bloom_desc", pct(fluxCapMult)),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_REINFORCED_WEAVE),
                Op.FLUX_CAP_MULT, fluxCapMult));
        nodes.add(new SpecStatNode(ArondightIds.NODE_VENT_LATTICE,
                ArondightStrings.weave("node_vent_lattice_name"),
                ArondightStrings.weave("node_vent_lattice_desc", pct(fluxDissMult)),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_REACTOR_BLOOM),
                Op.FLUX_DISS_MULT, fluxDissMult));
        nodes.add(new SpecStatNode(ArondightIds.NODE_DRIVE_RESONANCE,
                ArondightStrings.weave("node_drive_resonance_name"),
                ArondightStrings.weave("node_drive_resonance_desc", (int) speedFlat),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_VENT_LATTICE),
                Op.SPEED_FLAT, speedFlat));
        nodes.add(new SpecStatNode(ArondightIds.NODE_NEURAL_MESH,
                ArondightStrings.weave("node_neural_mesh_name"),
                ArondightStrings.weave("node_neural_mesh_desc", pct(maneuverMult)),
                Cluster.FRAME, Arrays.asList(ArondightIds.NODE_DENSE_PLATING),
                Op.MANEUVER_MULT, maneuverMult));

        nodes.add(new ShoulderUnsealNode(ArondightIds.NODE_SHOULDER_LEFT,
                ArondightStrings.weave("node_shoulder_left_name"),
                ArondightStrings.weave("node_shoulder_left_desc"),
                ArondightIds.SLOT_SHOULDER_L, ArondightIds.NODE_EXPANDED_LATTICE_1));
        nodes.add(new ShoulderUnsealNode(ArondightIds.NODE_SHOULDER_RIGHT,
                ArondightStrings.weave("node_shoulder_right_name"),
                ArondightStrings.weave("node_shoulder_right_desc"),
                ArondightIds.SLOT_SHOULDER_R, ArondightIds.NODE_SHOULDER_LEFT));
        nodes.add(new SpecStatNode(ArondightIds.NODE_EXPANDED_LATTICE_1,
                ArondightStrings.weave("node_expanded_lattice_1_name"),
                ArondightStrings.weave("node_expanded_lattice_desc", opFlat),
                Cluster.HARDPOINT, Arrays.asList(ArondightIds.NODE_ROOT), Op.OP_FLAT, opFlat));
        nodes.add(new SpecStatNode(ArondightIds.NODE_EXPANDED_LATTICE_2,
                ArondightStrings.weave("node_expanded_lattice_2_name"),
                ArondightStrings.weave("node_expanded_lattice_desc", opFlat),
                Cluster.HARDPOINT,
                Arrays.asList(ArondightIds.NODE_EXPANDED_LATTICE_1), Op.OP_FLAT, opFlat));
        nodes.add(new PassiveNode(ArondightIds.NODE_MUNITIONS_LOOM,
                ArondightStrings.weave("node_munitions_loom_name"),
                ArondightStrings.weave("node_munitions_loom_desc", loomSecs),
                Cluster.HARDPOINT, Arrays.asList(ArondightIds.NODE_SHOULDER_RIGHT)));
        nodes.add(new PassiveNode(ArondightIds.NODE_ARM_ACTUATORS,
                ArondightStrings.weave("node_arm_actuators_name"),
                ArondightStrings.weave("node_arm_actuators_desc", actuators),
                Cluster.HARDPOINT, Arrays.asList(ArondightIds.NODE_EXPANDED_LATTICE_2)));

        nodes.add(new arondight.weave.nodes.ChargeCellNode(
                ArondightStrings.weave("node_charge_cell_name"),
                ArondightStrings.weave("node_charge_cell_desc")));
        nodes.add(new PassiveNode(ArondightIds.NODE_QUICK_CYCLE,
                ArondightStrings.weave("node_quick_cycle_name"),
                ArondightStrings.weave("node_quick_cycle_desc"),
                Cluster.SYSTEM, Arrays.asList(ArondightIds.NODE_ROOT)));
        nodes.add(new PassiveNode(ArondightIds.NODE_ZERO_POINT_IDLE,
                ArondightStrings.weave("node_zero_point_idle_name"),
                ArondightStrings.weave("node_zero_point_idle_desc", zeroPoint),
                Cluster.SYSTEM, Arrays.asList(ArondightIds.NODE_QUICK_CYCLE)));
        nodes.add(new PassiveNode(ArondightIds.NODE_FARADAY_MESH,
                ArondightStrings.weave("node_faraday_mesh_name"),
                ArondightStrings.weave("node_faraday_mesh_desc", faradayPct),
                Cluster.SYSTEM, Arrays.asList(ArondightIds.NODE_CHARGE_CELL)));
        nodes.add(new PassiveNode(ArondightIds.NODE_DOCK_CYCLE,
                ArondightStrings.weave("node_dock_cycle_name"),
                ArondightStrings.weave("node_dock_cycle_desc", dockPct),
                Cluster.SYSTEM, Arrays.asList(ArondightIds.NODE_ZERO_POINT_IDLE)));
        nodes.add(new PassiveNode(ArondightIds.NODE_PURGE_VALVES,
                ArondightStrings.weave("node_purge_valves_name"),
                ArondightStrings.weave("node_purge_valves_desc", purgePct),
                Cluster.SYSTEM, Arrays.asList(ArondightIds.NODE_DOCK_CYCLE)));

        nodes.add(new PassiveNode(ArondightIds.NODE_WATCHDOG_REMOTES,
                ArondightStrings.weave("node_watchdog_remotes_name"),
                ArondightStrings.weave("node_watchdog_remotes_desc", watchdogRespawn),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_LATTICE_SIGHT)));
        nodes.add(new PassiveNode(ArondightIds.NODE_SELF_WEAVE,
                ArondightStrings.weave("node_self_weave_name"),
                ArondightStrings.weave("node_self_weave_desc", selfweavePer10s, selfweaveCap),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_HARVEST_REFLEX)));
        nodes.add(new PassiveNode(ArondightIds.NODE_AFTERIMAGE,
                ArondightStrings.weave("node_afterimage_name"),
                ArondightStrings.weave("node_afterimage_desc", afterThresh, afterDist, afterCd),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_KINETIC_BUFFER)));
        nodes.add(new PassiveNode(ArondightIds.NODE_STATIC_BLEED,
                ArondightStrings.weave("node_static_bleed_name"),
                ArondightStrings.weave("node_static_bleed_desc", bleedRange, bleedDamage),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_WATCHDOG_REMOTES)));
        nodes.add(new PassiveNode(ArondightIds.NODE_HARVEST_REFLEX,
                ArondightStrings.weave("node_harvest_reflex_name"),
                ArondightStrings.weave("node_harvest_reflex_desc", harvestHull, harvestArmor, harvestCr),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_LOOM_SALVAGE)));
        nodes.add(new PassiveNode(ArondightIds.NODE_LOOM_SALVAGE,
                ArondightStrings.weave("node_loom_salvage_name"),
                ArondightStrings.weave("node_loom_salvage_desc", salvageBonus),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_LATTICE_SIGHT)));
        nodes.add(new PassiveNode(ArondightIds.NODE_LATTICE_SIGHT,
                ArondightStrings.weave("node_lattice_sight_name"),
                ArondightStrings.weave("node_lattice_sight_desc"),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_ROOT)));
        nodes.add(new PassiveNode(ArondightIds.NODE_KINETIC_BUFFER,
                ArondightStrings.weave("node_kinetic_buffer_name"),
                ArondightStrings.weave("node_kinetic_buffer_desc", bufferPct),
                Cluster.LATTICE, Arrays.asList(ArondightIds.NODE_WATCHDOG_REMOTES)));

        Map<String, ArondightNode> byId = new HashMap<>();
        for (ArondightNode n : nodes) {
            byId.put(n.getId(), n);
        }
        BY_ID = byId;
        ALL = Collections.unmodifiableList(nodes);
    }

    private static int pct(float mult) {
        return Math.round((mult - 1f) * 100f);
    }
}
