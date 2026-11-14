package arondight.weave;

import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.state.ArondightState;

public final class ArondightResets {

    private ArondightResets() {}

    public static String getResetWeaveBlockReason(FleetMemberAPI member) {
        return WeaveActions.getResetWeaveBlockReason(member);
    }

    public static String getResetFormBlockReason(FleetMemberAPI member) {
        return WeaveActions.getResetFormBlockReason(member);
    }

    public static int getResetWeaveRefund(FleetMemberAPI member) {
        return member == null ? 0 : ArondightState.getSpentNodeSp(member.getId());
    }

    public static int getResetFormRefund(FleetMemberAPI member) {
        return member == null ? 0 : ArondightState.getSpentFormSp(member.getId());
    }

    public static List<String> getResetFormRelockForms(FleetMemberAPI member) {
        if (member == null) return Collections.emptyList();
        return WeaveActions.relockableForms(member.getId());
    }

    public static String getResetFormLandingForm(FleetMemberAPI member) {
        return member == null ? null : WeaveActions.getResetLandingForm(member.getId());
    }

    public static boolean resetWeave(FleetMemberAPI member, TextPanelAPI textPanel) {
        return WeaveActions.resetWeave(member, textPanel);
    }

    public static boolean resetForm(FleetMemberAPI member, TextPanelAPI textPanel) {
        return WeaveActions.resetForm(member, textPanel);
    }
}
