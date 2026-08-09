package arondight.combat.loom;

import com.fs.starfarer.api.Global;

final class LoomZoomLock {

	private static final String KEY = "maxCombatZoom";
	private static final float CLAMP = 1f;

	private static Float saved = null;

	private LoomZoomLock() {}

	static void engage() {
		if (saved == null) {
			saved = Global.getSettings().getFloat(KEY);
		} else {
			Global.getLogger(LoomZoomLock.class).warn(
					"Project Arondight: stale combat zoom clamp found at trial start; keeping the original saved value");
		}
		Global.getSettings().setFloat(KEY, CLAMP);
	}

	static void restore() {
		if (saved == null) return;
		Global.getSettings().setFloat(KEY, saved);
		saved = null;
	}
}
