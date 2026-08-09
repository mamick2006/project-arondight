package arondight.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

import arondight.ArondightIds;

public final class ArondightSettings {

    public static final String MOD_ID = "arondight";

    private static final Set<String> LUNA_INTS = new HashSet<>(Arrays.asList(
            ArondightIds.SET_NODE_SP_COST, ArondightIds.SET_TIER1_SP_COST,
            ArondightIds.SET_TIER2_SP_COST, ArondightIds.SET_RESPEC_SP_COST));

    private ArondightSettings() {}

    private static boolean lunaEnabled() {
        return Global.getSettings() != null
                && Global.getSettings().getModManager() != null
                && Global.getSettings().getModManager().isModEnabled("lunalib");
    }

    public static float getFloat(String key, float fallback) {
        try {
            return Global.getSettings().getFloat(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int getInt(String key, int fallback) {
        if (LUNA_INTS.contains(key) && lunaEnabled()) {
            Integer luna = LunaSettings.getInt(MOD_ID, key);
            if (luna != null) return luna;
        }
        try {
            return Global.getSettings().getInt(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        if (lunaEnabled()) {
            Boolean luna = LunaSettings.getBoolean(MOD_ID, key);
            if (luna != null) return luna;
        }
        return fallback;
    }

    public static String getString(String key, String fallback) {
        try {
            String value = Global.getSettings().getString(key);
            return value == null ? fallback : value;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Float nodeBonusXp;
    private static Float respecBonusXp;
    private static Float tier1BonusXp;
    private static Float tier2BonusXp;

    public static int nodeSpCost() {
        return getInt(ArondightIds.SET_NODE_SP_COST, 1);
    }

    public static float nodeBonusXp() {
        if (nodeBonusXp == null) nodeBonusXp = getFloat(ArondightIds.SET_NODE_BONUS_XP, 0f);
        return nodeBonusXp;
    }

    public static int respecSpCost() {
        return getInt(ArondightIds.SET_RESPEC_SP_COST, 1);
    }

    public static float respecBonusXp() {
        if (respecBonusXp == null) respecBonusXp = getFloat(ArondightIds.SET_RESPEC_BONUS_XP, 1.0f);
        return respecBonusXp;
    }

    public static int tier1SpCost() {
        return getInt(ArondightIds.SET_TIER1_SP_COST, 2);
    }

    public static float tier1BonusXp() {
        if (tier1BonusXp == null) tier1BonusXp = getFloat(ArondightIds.SET_TIER1_BONUS_XP, 0f);
        return tier1BonusXp;
    }

    public static int tier2SpCost() {
        return getInt(ArondightIds.SET_TIER2_SP_COST, 3);
    }

    public static float tier2BonusXp() {
        if (tier2BonusXp == null) tier2BonusXp = getFloat(ArondightIds.SET_TIER2_BONUS_XP, 0f);
        return tier2BonusXp;
    }

    @Deprecated
    public static int weaveCap() {
        return getInt(ArondightIds.SET_WEAVE_CAP, 12);
    }

    public static int weaveLogisticsStep() {
        return getInt(ArondightIds.SET_WEAVE_LOGISTICS_STEP, 2);
    }
}
