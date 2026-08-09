package arondight;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ArondightIds {

    private ArondightIds() {}

    public static final String FORM_BASE = "arondight_base";
    public static final String FORM_DURENDAL = "arondight_durendal";
    public static final String FORM_HAUTECLERE = "arondight_hauteclere";
    public static final String FORM_JOYEUSE = "arondight_joyeuse";
    public static final String FORM_LONGINUS = "arondight_longinus";
    public static final String FORM_GAEBOLG = "arondight_gaebolg";
    public static final String FORM_RHONGOMYNIAD = "arondight_rhongomyniad";
    public static final String FORM_CURTANA = "arondight_curtana";
    public static final String FORM_CLARENT = "arondight_clarent";
    public static final String FORM_FRAGARACH = "arondight_fragarach";

    public static final List<String> ALL_FORMS = Collections.unmodifiableList(Arrays.asList(
            FORM_BASE,
            FORM_DURENDAL, FORM_LONGINUS, FORM_CURTANA,
            FORM_HAUTECLERE, FORM_JOYEUSE,
            FORM_GAEBOLG, FORM_RHONGOMYNIAD,
            FORM_CLARENT, FORM_FRAGARACH));

    public static final String SYSTEM_SHIFT = "arondight_shift";
    public static final String SYSTEM_SPOTTER = "arondight_spotter";

    public static final String DEFENSE_WARD = "arondight_ward";
    public static final String DEFENSE_WARD_FLASH = "arondight_ward_flash";
    public static final String DEFENSE_WARD_BULWARK = "arondight_ward_bulwark";

    public static final String WEAPON_SEALED_COVER = "arondight_sealed_cover";

    public static String armWeaponFor(String formHullId) {
        return formHullId + "_arm";
    }

    public static final String SLOT_ARM_R = "WS_ARM_R";
    public static final String SLOT_ARM_L = "WS_ARM_L";
    public static final String SLOT_SHOULDER_L = "WS_SH_L";
    public static final String SLOT_SHOULDER_R = "WS_SH_R";

    public static final String HULLMOD_LATTICE = "arondight_lattice";
    public static final String HULLMOD_FORM_DURENDAL = "arondight_form_durendal";
    public static final String HULLMOD_FORM_HAUTECLERE = "arondight_form_hauteclere";
    public static final String HULLMOD_FORM_JOYEUSE = "arondight_form_joyeuse";
    public static final String HULLMOD_FORM_LONGINUS = "arondight_form_longinus";
    public static final String HULLMOD_FORM_GAEBOLG = "arondight_form_gaebolg";
    public static final String HULLMOD_FORM_RHONGOMYNIAD = "arondight_form_rhongomyniad";
    public static final String HULLMOD_FORM_CURTANA = "arondight_form_curtana";
    public static final String HULLMOD_FORM_CLARENT = "arondight_form_clarent";
    public static final String HULLMOD_FORM_FRAGARACH = "arondight_form_fragarach";

    public static final List<String> FORM_HULLMODS = Collections.unmodifiableList(Arrays.asList(
            HULLMOD_FORM_DURENDAL, HULLMOD_FORM_HAUTECLERE, HULLMOD_FORM_JOYEUSE,
            HULLMOD_FORM_LONGINUS, HULLMOD_FORM_GAEBOLG, HULLMOD_FORM_RHONGOMYNIAD,
            HULLMOD_FORM_CURTANA, HULLMOD_FORM_CLARENT, HULLMOD_FORM_FRAGARACH));

    public static final String NODE_ROOT = "root";
    public static final String NODE_REINFORCED_WEAVE = "reinforced_weave";
    public static final String NODE_DENSE_PLATING = "dense_plating";
    public static final String NODE_REACTOR_BLOOM = "reactor_bloom";
    public static final String NODE_VENT_LATTICE = "vent_lattice";
    public static final String NODE_DRIVE_RESONANCE = "drive_resonance";
    public static final String NODE_NEURAL_MESH = "neural_mesh";
    public static final String NODE_SHOULDER_LEFT = "shoulder_left";
    public static final String NODE_SHOULDER_RIGHT = "shoulder_right";
    public static final String NODE_EXPANDED_LATTICE_1 = "expanded_lattice_1";
    public static final String NODE_EXPANDED_LATTICE_2 = "expanded_lattice_2";
    public static final String NODE_MUNITIONS_LOOM = "munitions_loom";
    public static final String NODE_ARM_ACTUATORS = "arm_actuators";
    public static final String NODE_CHARGE_CELL = "charge_cell";
    public static final String NODE_QUICK_CYCLE = "quick_cycle";
    public static final String NODE_ZERO_POINT_IDLE = "zero_point_idle";
    public static final String NODE_WATCHDOG_REMOTES = "watchdog_remotes";
    public static final String NODE_SELF_WEAVE = "self_weave";
    public static final String NODE_AFTERIMAGE = "afterimage";
    public static final String NODE_STATIC_BLEED = "static_bleed";
    public static final String NODE_HARVEST_REFLEX = "harvest_reflex";
    public static final String NODE_LOOM_SALVAGE = "loom_salvage";
    public static final String NODE_LATTICE_SIGHT = "lattice_sight";
    public static final String NODE_KINETIC_BUFFER = "kinetic_buffer";

    public static final String PERSISTENT_KEY = "arondight_state";

    public static final String TAG_APPLIED = "arondight_applied";
    public static final String TAG_WEAVE_PREFIX = "arondight_weave_";

    public static final String QUEST_FRAME_RECOVERED = "frame_recovered";
    public static final String QUEST_M1_COMPLETE = "m1_complete";
    public static final String QUEST_M2_COMPLETE = "m2_complete";

    public static final String SET_NODE_SP_COST = "arondight_node_sp_cost";
    public static final String SET_NODE_BONUS_XP = "arondight_node_bonus_xp";
    public static final String SET_RESPEC_SP_COST = "arondight_respec_sp_cost";
    public static final String SET_RESPEC_BONUS_XP = "arondight_respec_bonus_xp";
    public static final String SET_TIER1_SP_COST = "arondight_tier1_sp_cost";
    public static final String SET_TIER1_BONUS_XP = "arondight_tier1_bonus_xp";
    public static final String SET_TIER2_SP_COST = "arondight_tier2_sp_cost";
    public static final String SET_TIER2_BONUS_XP = "arondight_tier2_bonus_xp";
    public static final String SET_WEAVE_CAP = "arondight_weave_cap";
    public static final String SET_WEAVE_LOGISTICS_STEP = "arondight_weave_logistics_step";
    public static final String SET_MGMT_INTEL_ONLY = "arondight_mgmt_intel_only";
}
