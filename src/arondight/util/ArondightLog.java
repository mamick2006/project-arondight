package arondight.util;

import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;

public final class ArondightLog {

    private static final Set<String> warned = new HashSet<>();

    private ArondightLog() {}

    public static void warnOnce(String key, String message) {
        if (warned.add(key)) {
            Global.getLogger(ArondightLog.class).warn("Project Arondight: " + message);
        }
    }

    public static boolean hasWarned(String key) {
        return warned.contains(key);
    }
}
