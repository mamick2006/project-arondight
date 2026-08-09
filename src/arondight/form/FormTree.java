package arondight.form;

import java.util.HashMap;
import java.util.Map;

import arondight.ArondightIds;
import arondight.util.ArondightSettings;

final class FormTree {

    private static final Map<String, String> PARENT = new HashMap<>();
    private static final Map<String, Integer> TIER = new HashMap<>();

    static {
        TIER.put(ArondightIds.FORM_BASE, 0);
        tier1(ArondightIds.FORM_DURENDAL);
        tier1(ArondightIds.FORM_LONGINUS);
        tier1(ArondightIds.FORM_CURTANA);
        tier2(ArondightIds.FORM_HAUTECLERE, ArondightIds.FORM_DURENDAL);
        tier2(ArondightIds.FORM_JOYEUSE, ArondightIds.FORM_DURENDAL);
        tier2(ArondightIds.FORM_GAEBOLG, ArondightIds.FORM_LONGINUS);
        tier2(ArondightIds.FORM_RHONGOMYNIAD, ArondightIds.FORM_LONGINUS);
        tier2(ArondightIds.FORM_CLARENT, ArondightIds.FORM_CURTANA);
        tier2(ArondightIds.FORM_FRAGARACH, ArondightIds.FORM_CURTANA);
    }

    private static void tier1(String formId) {
        PARENT.put(formId, ArondightIds.FORM_BASE);
        TIER.put(formId, 1);
    }

    private static void tier2(String formId, String parent) {
        PARENT.put(formId, parent);
        TIER.put(formId, 2);
    }

    private FormTree() {}

    static boolean isForm(String formId) {
        return TIER.containsKey(formId);
    }

    static String parentOf(String formId) {
        return PARENT.get(formId);
    }

    static int tierOf(String formId) {
        Integer t = TIER.get(formId);
        return t == null ? -1 : t;
    }

    static String displayName(String formId) {
        try {
            com.fs.starfarer.api.combat.ShipHullSpecAPI spec =
                    com.fs.starfarer.api.Global.getSettings().getHullSpec(formId);
            if (spec != null && spec.getHullName() != null) return spec.getHullName();
        } catch (Exception ignored) {
        }
        Integer known = TIER.get(formId);
        if (known == null) return formId;
        String tail = formId.substring(formId.indexOf('_') + 1);
        return Character.toUpperCase(tail.charAt(0)) + tail.substring(1);
    }

    static int selectSpCost(String formId) {
        switch (tierOf(formId)) {
            case 1: return ArondightSettings.tier1SpCost();
            case 2: return ArondightSettings.tier2SpCost();
            default: return 0;
        }
    }

    static float selectBonusXp(String formId) {
        switch (tierOf(formId)) {
            case 1: return ArondightSettings.tier1BonusXp();
            case 2: return ArondightSettings.tier2BonusXp();
            default: return 0f;
        }
    }
}
