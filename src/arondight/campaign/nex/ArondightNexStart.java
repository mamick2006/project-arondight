package arondight.campaign.nex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.NGCAddStartingShipsByFleetType;

import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;

import arondight.ArondightIds;
import arondight.campaign.ArondightQuest;
import arondight.util.ArondightLog;
import arondight.weave.ArondightWeave;

public class ArondightNexStart extends CustomStart {

    private static final String FRAME_VARIANT = ArondightIds.FORM_BASE + "_Standard";

    private static final String TENDER_VARIANT = "shepherd_Starting";

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data =
                (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        ExerelinSetupData.getInstance().freeStart = true;

        List<String> ships = new ArrayList<>();
        ships.add(FRAME_VARIANT);
        ships.add(TENDER_VARIANT);
        NGCAddStartingShipsByFleetType.generateFleetFromVariantIds(dialog, data, null, ships);

        data.addScript(new Script() {
            @Override
            public void run() {
                adoptFrame();
            }
        });

        FireBest.fire(null, dialog, memoryMap, "ExerelinNGCStep4");
    }

    private static void adoptFrame() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        FleetMemberAPI frame = null;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (ArondightIds.FORM_BASE.equals(member.getHullId())) {
                frame = member;
                break;
            }
        }
        if (frame == null) {
            ArondightLog.warnOnce("nex_start_no_frame", "the Nexerelin start could not build "
                    + FRAME_VARIANT + " (hull or variant not loadable); the game begins with "
                    + "no frame and the mod's trees stay out of reach");
            return;
        }
        ArondightQuest.isolateFrameVariant(frame);
        frame.setShipName("Arondight");
        ArondightQuest.completeMission1(frame, null);
        ArondightWeave.apply(frame);
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }
}
