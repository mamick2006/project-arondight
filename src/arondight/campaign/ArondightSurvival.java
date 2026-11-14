package arondight.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

import arondight.util.ArondightSettings;

public final class ArondightSurvival {

    private ArondightSurvival() {}

    public static boolean isSurvivalCampaign() {
        SectorAPI sector = Global.getSector();
        return sector != null && sector.isExtMode();
    }

    public static float watchFpMult() {
        return ArondightSettings.getFloat("arondight_survival_watch_fp_mult", 0.3f);
    }
}
