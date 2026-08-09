package arondight;

import java.util.Arrays;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.weave.ArondightWeave;

public final class ArondightDev {

    private ArondightDev() {}

    public static String grantFrame() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) {
            return say("No campaign.");
        }
        FleetMemberAPI existing = ArondightState.findPlayerFrame();
        if (existing != null) {
            return say("Frame already in fleet: " + existing.getShipName());
        }
        FleetMemberAPI member = ArondightTransformer.createFrameMember();
        if (member == null) {
            return say("createFrameMember failed (hulls not loaded?)");
        }
        Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
        member.getRepairTracker().setCR(0.7f);
        ArondightState.setQuestFlag(ArondightIds.QUEST_FRAME_RECOVERED, true);
        ArondightState.setQuestFlag(ArondightIds.QUEST_M1_COMPLETE, true);
        ArondightState.setQuestFlag(ArondightIds.QUEST_M2_COMPLETE, true);
        for (String tier1 : new String[] { ArondightIds.FORM_DURENDAL, ArondightIds.FORM_LONGINUS,
                ArondightIds.FORM_CURTANA }) {
            ArondightState.unlockForm(member.getId(), tier1);
        }
        arondight.ui.ArondightIntel.ensureExists();
        return say("Frame granted (base form; tier 1 unlocked; trees open).");
    }

    public static String unlockAll() {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        if (member == null) return say("No frame in fleet. grantFrame() first.");
        ArondightState.setQuestFlag(ArondightIds.QUEST_M2_COMPLETE, true);
        for (String formId : ArondightIds.ALL_FORMS) {
            ArondightState.unlockForm(member.getId(), formId);
        }
        return say("All ten forms unlocked.");
    }

    public static String setForm(String formId) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        if (member == null) return say("No frame in fleet. grantFrame() first.");
        ArondightState.unlockForm(member.getId(), formId);
        ArondightTransformer.TransformResult result =
                ArondightTransformer.transformUnchecked(member, formId);
        return say(result.success
                ? "Now " + ArondightTransformer.getFormDisplayName(formId)
                        + (result.weaponsMovedToCargo.isEmpty() ? "" : "; moved to cargo: " + result.weaponsMovedToCargo)
                : "Refused: " + result.refusalReason);
    }

    public static String weave(String nodeId) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        if (member == null) return say("No frame in fleet. grantFrame() first.");
        if (ArondightWeave.getNode(nodeId) == null) return say("Unknown node: " + nodeId);
        ArondightState.addWovenNode(member.getId(), nodeId);
        ArondightWeave.apply(member);
        return say("Wove " + nodeId + " (" + ArondightState.getWovenCount(member.getId()) + " woven).");
    }

    public static String respecFree() {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        if (member == null) return say("No frame in fleet.");
        ArondightState.clearWovenNodes(member.getId());
        ArondightState.clearShoulderFits(member.getId());
        ArondightState.clearSpentNodeSp(member.getId());
        ArondightWeave.apply(member);
        return say("Weave cleared.");
    }

    public static String status() {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        if (member == null) return say("No frame in fleet.");
        String id = member.getId();
        return say("form=" + ArondightState.getCurrentForm(id)
                + " unlocked=" + ArondightState.getUnlockedForms(id)
                + " woven=" + ArondightState.getWovenNodes(id)
                + " respecs=" + ArondightState.getRespecCount(id)
                + " flags[recovered/m1/m2]=" + Arrays.asList(
                        ArondightState.getQuestFlag(ArondightIds.QUEST_FRAME_RECOVERED),
                        ArondightState.getQuestFlag(ArondightIds.QUEST_M1_COMPLETE),
                        ArondightState.getQuestFlag(ArondightIds.QUEST_M2_COMPLETE)));
    }

    private static String say(String message) {
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage("Arondight: " + message);
        }
        return message;
    }
}
