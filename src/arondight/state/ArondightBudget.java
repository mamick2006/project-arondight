package arondight.state;

import com.fs.starfarer.api.Global;

public final class ArondightBudget {

    private ArondightBudget() {}

    public static int getInvestedSp(String memberId) {
        return ArondightState.getSpentNodeSp(memberId)
                + ArondightState.getSpentFormSp(memberId);
    }

    public static int getBudget() {
        if (Global.getSector() == null || Global.getSector().getPlayerStats() == null) {
            return 0;
        }
        return Global.getSector().getPlayerStats().getLevel();
    }

    public static boolean fits(String memberId, int cost) {
        return getInvestedSp(memberId) + cost <= getBudget();
    }

    public static int levelNeededFor(String memberId, int cost) {
        return getInvestedSp(memberId) + cost;
    }
}
