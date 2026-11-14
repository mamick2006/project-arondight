package arondight.balance;

public final class InvestCap {

    public static final int BASE = 5;

    private InvestCap() {}

    public static int capAt(int level) {
        return BASE + level;
    }

    public static boolean fits(int invested, int cost, int level) {
        return invested + cost <= capAt(level);
    }

    public static int levelNeeded(int invested, int cost) {
        return invested + cost - BASE;
    }

    public static boolean pastLevelCap(int invested, int cost, int maxLevel) {
        return levelNeeded(invested, cost) > maxLevel;
    }
}
