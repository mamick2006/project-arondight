package arondight.balance;

public final class WatchBudget {

    public static final float SMALL = 0.2f;
    public static final float MEDIUM = 0.35f;
    public static final float LARGE = 0.5f;
    public static final float MAXIMUM = 1.1f;

    public static final float SIZE_SCALE = 0.75f;

    public static final float MULT_MIN = 0.1f;
    public static final float MULT_MAX = 1f;

    private WatchBudget() {}

    public static float clampMult(float mult) {
        if (!(mult >= MULT_MIN)) {
            return MULT_MIN;
        }
        return mult > MULT_MAX ? MULT_MAX : mult;
    }

    public static float mediumFraction(float roll) {
        float min = MEDIUM - (MEDIUM - SMALL) / 2f;
        float max = MEDIUM + (LARGE - MEDIUM) / 2f;
        return min + (max - min) * roll;
    }

    public static float normalPoints(float roll, float doctrineMult, float maxPoints) {
        float fraction = mediumFraction(roll) * SIZE_SCALE * doctrineMult;
        if (fraction > MAXIMUM) {
            fraction = MAXIMUM;
        }
        return fraction * maxPoints;
    }

    public static float survivalPoints(float roll, float doctrineMult, float maxPoints, float mult) {
        return normalPoints(roll, doctrineMult, maxPoints) * clampMult(mult);
    }
}
