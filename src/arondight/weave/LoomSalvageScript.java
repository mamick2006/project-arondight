package arondight.weave;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import arondight.ArondightIds;
import arondight.state.ArondightState;
import arondight.util.ArondightSettings;

public class LoomSalvageScript implements EveryFrameScript {

    private static final String MOD_ID = "arondight_loom_salvage";

    private float sinceCheck = 10f;
    private boolean applied = false;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float days) {
        sinceCheck += days;
        if (sinceCheck < 0.1f) return;
        sinceCheck = 0f;

        CampaignFleetAPI fleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (fleet == null) return;

        boolean wantsBonus = false;
        if (ArondightState.hasAnyFrame()) {
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                if (ArondightState.isFrame(member.getId())
                        && ArondightState.isNodeWoven(member.getId(), ArondightIds.NODE_LOOM_SALVAGE)) {
                    wantsBonus = true;
                    break;
                }
            }
        }

        if (wantsBonus && !applied) {
            float bonus = ArondightSettings.getFloat("arondight_salvage_bonus", 0.2f);
            fleet.getStats().getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
                    .modifyFlat(MOD_ID, bonus);
            applied = true;
        } else if (!wantsBonus && applied) {
            fleet.getStats().getDynamic().getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)
                    .unmodifyFlat(MOD_ID);
            applied = false;
        }
    }
}
