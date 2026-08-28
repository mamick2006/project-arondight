package arondight.weave;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.state.ArondightBudget;
import arondight.state.ArondightState;
import arondight.util.ArondightSettings;
import arondight.weave.nodes.ShoulderUnsealNode;

final class WeaveActions {

    private WeaveActions() {}

    static String getWeaveBlockReason(FleetMemberAPI member, String nodeId) {
        if (member == null || !ArondightState.isFrame(member.getId())) {
            return ArondightStrings.weave("weave_block_no_frame");
        }
        ArondightNode node = ArondightNodes.byId(nodeId);
        if (node == null) {
            return ArondightStrings.weave("weave_block_unknown");
        }
        if (ArondightIds.NODE_ROOT.equals(nodeId)) {
            return ArondightStrings.weave("weave_block_root");
        }
        String memberId = member.getId();
        if (ArondightState.isNodeWoven(memberId, nodeId)) {
            return ArondightStrings.weave("weave_block_already");
        }
        if (!prereqSatisfied(memberId, node)) {
            StringBuilder names = new StringBuilder();
            for (String prereq : node.getPrereqAnyOf()) {
                ArondightNode p = ArondightNodes.byId(prereq);
                if (names.length() > 0) names.append(" or ");
                names.append(p == null ? prereq : p.getName());
            }
            return ArondightStrings.weave("weave_block_requires", names);
        }
        if (!node.isAvailable()) {
            return ArondightStrings.weave("weave_block_capability");
        }
        int cost = ArondightSettings.nodeSpCost();
        if (playerStats() == null || playerStats().getStoryPoints() < cost) {
            return requiresSp(cost);
        }
        if (!ArondightBudget.fits(memberId, cost)) {
            return ArondightStrings.console("console_block_budget",
                    ArondightBudget.levelNeededFor(memberId, cost));
        }
        return null;
    }

    static boolean weaveNode(FleetMemberAPI member, String nodeId, TextPanelAPI textPanel) {
        if (getWeaveBlockReason(member, nodeId) != null) return false;
        ArondightNode node = ArondightNodes.byId(nodeId);
        int cost = ArondightSettings.nodeSpCost();
        playerStats().spendStoryPoints(cost, true, textPanel, true,
                ArondightSettings.nodeBonusXp(),
                ArondightStrings.weave("weave_ledger_wove", node.getName()));
        ArondightState.addSpentNodeSp(member.getId(), nodeId, cost);
        ArondightState.addWovenNode(member.getId(), nodeId);
        WeaveApplier.apply(member);
        return true;
    }

    static String getRespecBlockReason(FleetMemberAPI member) {
        if (member == null || !ArondightState.isFrame(member.getId())) {
            return ArondightStrings.weave("weave_block_no_frame");
        }
        if (ArondightState.getWovenCount(member.getId()) == 0
                && relockableForms(member.getId()).isEmpty()) {
            return ArondightStrings.weave("weave_block_nothing_to_unwind");
        }
        int cost = ArondightSettings.respecSpCost();
        if (playerStats() == null || playerStats().getStoryPoints() < cost) {
            return requiresSp(cost);
        }
        return null;
    }

    private static String requiresSp(int cost) {
        return cost == 1 ? ArondightStrings.weave("weave_block_requires_sp_one", cost)
                : ArondightStrings.weave("weave_block_requires_sp_many", cost);
    }

    static java.util.List<String> relockableForms(String memberId) {
        java.util.List<String> relock = new java.util.ArrayList<>();
        for (String formId : ArondightIds.ALL_FORMS) {
            if (arondight.form.ArondightTransformer.getTierOf(formId) != 0
                    && ArondightState.isFormUnlocked(memberId, formId)) {
                relock.add(formId);
            }
        }
        return relock;
    }

    static boolean respec(FleetMemberAPI member, TextPanelAPI textPanel) {
        if (getRespecBlockReason(member) != null) return false;
        String memberId = member.getId();

        ShipVariantAPI variant = member.getVariant();
        if (variant != null && Global.getSector().getPlayerFleet() != null) {
            for (String nodeId : ArondightState.getWovenNodes(memberId)) {
                ArondightNode node = ArondightNodes.byId(nodeId);
                if (!(node instanceof ShoulderUnsealNode)) continue;
                String slotId = ((ShoulderUnsealNode) node).getSlotId();
                String weaponId = variant.getWeaponId(slotId);
                if (weaponId != null && !ArondightIds.WEAPON_SEALED_COVER.equals(weaponId)) {
                    Global.getSector().getPlayerFleet().getCargo().addWeapons(weaponId, 1);
                    variant.clearSlot(slotId);
                }
            }
        }

        String current = ArondightState.getCurrentForm(memberId);
        if (current != null && !ArondightIds.FORM_BASE.equals(current)) {
            arondight.form.ArondightTransformer.transformUnchecked(member, ArondightIds.FORM_BASE);
        }

        playerStats().spendStoryPoints(ArondightSettings.respecSpCost(), true, textPanel, true,
                ArondightSettings.respecBonusXp(),
                ArondightStrings.weave("weave_ledger_unwove"));

        int refund = ArondightState.getSpentNodeSp(memberId)
                + ArondightState.getSpentFormSp(memberId);
        if (refund > 0) {
            if (textPanel != null) {
                playerStats().addStoryPoints(refund, textPanel, true);
            } else {
                playerStats().addStoryPoints(refund);
            }
        }
        ArondightState.clearSpentNodeSp(memberId);
        ArondightState.clearSpentFormSp(memberId);

        for (String formId : relockableForms(memberId)) {
            ArondightState.lockForm(memberId, formId);
        }

        ArondightState.clearWovenNodes(memberId);
        ArondightState.clearShoulderFits(memberId);
        ArondightState.incrementRespecCount(memberId);
        WeaveApplier.apply(member);
        arondight.form.ArondightTransformer.stripOpOverflowToCargo(member);
        return true;
    }

    private static boolean prereqSatisfied(String memberId, ArondightNode node) {
        return prereqSatisfiedIn(ArondightState.getWovenNodes(memberId), node);
    }

    private static boolean prereqSatisfiedIn(java.util.Set<String> woven, ArondightNode node) {
        if (node.getPrereqAnyOf().isEmpty()) return true;
        for (String prereq : node.getPrereqAnyOf()) {
            if (ArondightIds.NODE_ROOT.equals(prereq) || woven.contains(prereq)) {
                return true;
            }
        }
        return false;
    }

    static java.util.List<String> computeHealStrips(java.util.Collection<String> wovenNodes) {
        java.util.Set<String> survivors = new java.util.LinkedHashSet<>(wovenNodes);
        java.util.List<String> strips = new java.util.ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String nodeId : new java.util.ArrayList<>(survivors)) {
                ArondightNode node = ArondightNodes.byId(nodeId);
                boolean legal = node != null
                        && !ArondightIds.NODE_ROOT.equals(nodeId)
                        && prereqSatisfiedIn(survivors, node);
                if (legal) continue;
                survivors.remove(nodeId);
                strips.add(nodeId);
                changed = true;
            }
        }
        return strips;
    }

    static int healOnGameLoad() {
        int refund = 0;
        for (String memberId : ArondightState.getFrameMemberIds()) {
            for (String nodeId : computeHealStrips(ArondightState.getWovenNodes(memberId))) {
                ArondightState.removeWovenNode(memberId, nodeId);
                refund += ArondightState.removeSpentNodeSp(memberId, nodeId);
            }
        }
        if (refund > 0) {
            MutableCharacterStatsAPI stats = playerStats();
            if (stats != null) stats.addStoryPoints(refund);
            try {
                Global.getSector().getCampaignUI().addMessage(refund == 1
                        ? ArondightStrings.weave("weave_heal_refund_one", refund)
                        : ArondightStrings.weave("weave_heal_refund_many", refund));
            } catch (Exception ignored) {
            }
        }
        return refund;
    }

    private static MutableCharacterStatsAPI playerStats() {
        if (Global.getSector() == null || Global.getSector().getPlayerPerson() == null) return null;
        return Global.getSector().getPlayerPerson().getStats();
    }
}
