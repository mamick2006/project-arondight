package arondight.weave;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;

public final class ConsoleContext {

    private ConsoleContext() {}

    public static boolean inBattle() {
        if (Global.getCurrentState() == GameState.COMBAT) return true;
        return Global.getSector() != null
                && Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().getBattle() != null;
    }

    public static boolean inRefit() {
        if (Global.getSector() == null) return false;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        return ui != null && ui.getCurrentCoreTab() == CoreUITabId.REFIT;
    }
}
