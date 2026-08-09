package arondight;

import com.fs.starfarer.api.Global;

public final class ArondightStrings {

    public static final String BAR = "arondight_bar";
    public static final String QUEST = "arondight_quest";
    public static final String CONSOLE = "arondight_console";
    public static final String FORMS = "arondight_forms";
    public static final String WEAVE = "arondight_weave";
    public static final String HULLMODS = "arondight_hullmods";
    public static final String COMBAT = "arondight_combat";

    private ArondightStrings() {}

    public static String get(String category, String key) {
        String value;
        try {
            value = Global.getSettings().getString(category, key);
        } catch (Exception e) {
            throw new IllegalStateException("Project Arondight: missing strings.json entry "
                    + category + ":" + key, e);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Project Arondight: missing strings.json entry "
                    + category + ":" + key);
        }
        return value;
    }

    public static String get(String category, String key, Object... args) {
        return fill(category, key, get(category, key), args);
    }

    public static String bar(String key) { return get(BAR, key); }
    public static String bar(String key, Object... args) { return get(BAR, key, args); }
    public static String quest(String key) { return get(QUEST, key); }
    public static String quest(String key, Object... args) { return get(QUEST, key, args); }
    public static String console(String key) { return get(CONSOLE, key); }
    public static String console(String key, Object... args) { return get(CONSOLE, key, args); }
    public static String forms(String key) { return get(FORMS, key); }
    public static String forms(String key, Object... args) { return get(FORMS, key, args); }
    public static String weave(String key) { return get(WEAVE, key); }
    public static String weave(String key, Object... args) { return get(WEAVE, key, args); }
    public static String hullmods(String key) { return get(HULLMODS, key); }
    public static String combat(String key) { return get(COMBAT, key); }

    private static String fill(String category, String key, String template, Object[] args) {
        int slots = 0;
        for (int at = template.indexOf("%s"); at >= 0; at = template.indexOf("%s", at + 2)) {
            slots++;
        }
        if (slots != args.length) {
            throw new IllegalStateException("Project Arondight: strings.json entry "
                    + category + ":" + key + " has " + slots + " %s slot(s) but "
                    + args.length + " arg(s)");
        }
        StringBuilder out = new StringBuilder(template.length() + 32);
        int from = 0;
        for (Object arg : args) {
            int at = template.indexOf("%s", from);
            out.append(template, from, at).append(arg);
            from = at + 2;
        }
        out.append(template, from, template.length());
        return out.toString();
    }
}
