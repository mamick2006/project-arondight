package arondight.state;

import com.fs.starfarer.api.Global;

import arondight.ArondightStrings;
import arondight.balance.InvestCap;

public final class ArondightBudget {

    public static final int BASE = InvestCap.BASE;

    private ArondightBudget() {}

    public static int getInvestedSp(String memberId) {
        return ArondightState.getSpentNodeSp(memberId)
                + ArondightState.getSpentFormSp(memberId);
    }

    public static int getBudget() {
        if (Global.getSector() == null || Global.getSector().getPlayerStats() == null) {
            return 0;
        }
        return InvestCap.capAt(Global.getSector().getPlayerStats().getLevel());
    }

    public static boolean fits(String memberId, int cost) {
        return getInvestedSp(memberId) + cost <= getBudget();
    }

    public static int levelNeededFor(String memberId, int cost) {
        return InvestCap.levelNeeded(getInvestedSp(memberId), cost);
    }

    public static String blockReason(String memberId, int cost) {
        if (InvestCap.pastLevelCap(getInvestedSp(memberId), cost, maxLevel())) {
            return ArondightStrings.console("console_block_budget_max", BASE);
        }
        return ArondightStrings.console("console_block_budget", BASE,
                levelNeededFor(memberId, cost));
    }

    static int maxLevel() {
        return Global.getSettings().getInt("playerMaxLevel");
    }
}
