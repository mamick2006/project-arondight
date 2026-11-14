package arondight.form.mechanics;

public final class ArmKick {

    private ArmKick() {
    }

    public static float decay(float kick, float amount, float settleSeconds) {
        kick -= amount / settleSeconds;
        return kick < 0f ? 0f : kick;
    }

    public static boolean beamLights(float charge, boolean wasOn, boolean returning) {
        return !returning && !wasOn && charge > 0f;
    }

    public static float shove(float kick) {
        return kick * kick * (3f - 2f * kick);
    }
}
