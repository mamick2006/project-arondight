package arondight.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.form.ArondightTransformer.TransformResult;
import arondight.state.ArondightBudget;
import arondight.state.ArondightState;
import arondight.weave.ArondightUndo;
import arondight.weave.ArondightWeave;
import arondight.weave.ConsoleContext;

final class FormActions {

    private FormActions() {}

    static String getTransformBlockReason(FleetMemberAPI member, String targetFormId) {
        String logical = getLogicalBlockReason(member, targetFormId);
        if (logical != null) return logical;
        return getSelectBlockReason(member, targetFormId);
    }

    private static String getLogicalBlockReason(FleetMemberAPI member, String targetFormId) {
        if (member == null || !ArondightState.isFrame(member.getId())) {
            return ArondightStrings.forms("form_block_no_frame");
        }
        if (!FormTree.isForm(targetFormId)) {
            return ArondightStrings.forms("form_block_unknown");
        }
        if (targetFormId.equals(ArondightState.getCurrentForm(member.getId()))) {
            return ArondightStrings.forms("form_block_already_worn");
        }
        return null;
    }

    static ArondightTransformer.FormSelectState getSelectState(FleetMemberAPI member,
            String formId) {
        if (member == null || !ArondightState.isFrame(member.getId())) {
            return ArondightTransformer.FormSelectState.NO_FRAME;
        }
        if (!FormTree.isForm(formId)) {
            return ArondightTransformer.FormSelectState.UNKNOWN;
        }
        String memberId = member.getId();
        String current = ArondightState.getCurrentForm(memberId);
        if (formId.equals(current)) {
            return ArondightTransformer.FormSelectState.CURRENT;
        }
        if (!current.equals(FormTree.parentOf(formId))) {
            return ArondightTransformer.FormSelectState.LOCKED_DESCENT;
        }
        if (FormTree.tierOf(formId) == 2 && !ArondightState.areTreesUnlocked()) {
            return ArondightTransformer.FormSelectState.LOCKED_SEALED;
        }
        if (getContextBlockReason() != null) {
            return ArondightTransformer.FormSelectState.LOCKED_CONTEXT;
        }
        if (getEconomyBlockReason(member, formId) != null) {
            return ArondightTransformer.FormSelectState.LOCKED_ECONOMY;
        }
        return ArondightTransformer.FormSelectState.SELECT_OK;
    }

    static String getSelectBlockReason(FleetMemberAPI member, String formId) {
        ArondightTransformer.FormSelectState state = getSelectState(member, formId);
        switch (state) {
            case NO_FRAME:
                return ArondightStrings.forms("form_block_no_frame");
            case UNKNOWN:
                return ArondightStrings.forms("form_block_unknown");
            case CURRENT:
                return ArondightStrings.forms("form_block_current");
            case LOCKED_SEALED:
                return ArondightStrings.forms("form_block_sealed");
            case LOCKED_DESCENT: {
                String current = ArondightState.getCurrentForm(member.getId());
                String key = DescentNote.key(FormTree.tierOf(formId), FormTree.tierOf(current));
                if (DescentNote.ROOT.equals(key)) {
                    return ArondightStrings.forms("form_block_root");
                }
                if (DescentNote.LEAF.equals(key)) {
                    return ArondightStrings.forms("form_block_leaf");
                }
                if (DescentNote.PARENT.equals(key)) {
                    return ArondightStrings.forms("form_block_parent_first",
                            ArondightTransformer.getFormDisplayName(FormTree.parentOf(formId)));
                }
                return ArondightStrings.forms("form_block_descent",
                        ArondightTransformer.getFormDisplayName(current));
            }
            case LOCKED_CONTEXT:
                return getContextBlockReason();
            case LOCKED_ECONOMY:
                return getEconomyBlockReason(member, formId);
            case SELECT_OK:
            default:
                return null;
        }
    }

    private static String getEconomyBlockReason(FleetMemberAPI member, String formId) {
        int cost = FormTree.selectSpCost(formId);
        if (cost <= 0) return null;
        MutableCharacterStatsAPI stats = playerStats();
        if (stats == null || stats.getStoryPoints() < cost) {
            return cost == 1 ? ArondightStrings.forms("form_block_requires_sp_one", cost)
                    : ArondightStrings.forms("form_block_requires_sp_many", cost);
        }
        if (!ArondightBudget.fits(member.getId(), cost)) {
            return ArondightBudget.blockReason(member.getId(), cost);
        }
        return null;
    }

    private static String getContextBlockReason() {
        if (ConsoleContext.inBattle()) {
            return ArondightStrings.forms("form_block_battle");
        }
        if (ConsoleContext.inRefit()) {
            return ArondightStrings.forms("form_block_refit");
        }
        return null;
    }

    static TransformResult transform(FleetMemberAPI member, String targetFormId, boolean skipGuards) {
        String logical = getLogicalBlockReason(member, targetFormId);
        if (logical != null) return TransformResult.refused(logical);
        int charge = 0;
        if (!skipGuards) {
            String current = ArondightState.getCurrentForm(member.getId());
            if (!current.equals(FormTree.parentOf(targetFormId))) {
                String reason = getSelectBlockReason(member, targetFormId);
                return TransformResult.refused(reason != null ? reason
                        : ArondightStrings.forms("form_block_tree"));
            }
            String context = getContextBlockReason();
            if (context != null) return TransformResult.refused(context);
            String economy = getEconomyBlockReason(member, targetFormId);
            if (economy != null) return TransformResult.refused(economy);
            charge = FormTree.selectSpCost(targetFormId);
        }

        ShipVariantAPI variant = member.getVariant();
        if (variant == null) {
            return TransformResult.refused(ArondightStrings.forms("form_block_no_variant"));
        }

        ShipHullSpecAPI target;
        try {
            target = Global.getSettings().getHullSpec(targetFormId);
        } catch (Exception e) {
            target = null;
        }
        if (target == null) {
            return TransformResult.refused(ArondightStrings.forms("form_block_unknown"));
        }

        String fromForm = ArondightState.getCurrentForm(member.getId());
        boolean wasUnlocked = ArondightState.isFormUnlocked(member.getId(), targetFormId);
        long bonusAdded = 0L;
        long deferredAdded = 0L;
        if (charge > 0) {
            MutableCharacterStatsAPI stats = playerStats();
            long bonusBefore = stats.getBonusXp();
            long deferredBefore = stats.getDeferredBonusXp();
            stats.spendStoryPoints(charge, true, null, true,
                    FormTree.selectBonusXp(targetFormId),
                    ArondightStrings.forms("form_ledger_transformed",
                            ArondightTransformer.getFormDisplayName(targetFormId)));
            bonusAdded = stats.getBonusXp() - bonusBefore;
            deferredAdded = stats.getDeferredBonusXp() - deferredBefore;
            ArondightState.addSpentFormSp(member.getId(), charge);
            ArondightState.unlockForm(member.getId(), targetFormId);
        }

        List<String> moved = new ArrayList<>();

        for (String slotId : new ArrayList<>(variant.getNonBuiltInWeaponSlots())) {
            String weaponId = variant.getWeaponId(slotId);
            if (weaponId == null || ArondightIds.WEAPON_SEALED_COVER.equals(weaponId)) continue;
            if (isShoulderKeptByWeave(member, slotId)) continue;
            WeaponSlotAPI targetSlot = findSlot(target, slotId);
            WeaponSpecAPI weaponSpec = getWeaponSpec(weaponId);
            boolean fits = targetSlot != null
                    && !targetSlot.isDecorative()
                    && !targetSlot.isBuiltIn()
                    && !targetSlot.isSystemSlot()
                    && weaponSpec != null
                    && targetSlot.weaponFits(weaponSpec);
            if (!fits) {
                moveToCargo(weaponId, weaponSpec, moved);
                variant.clearSlot(slotId);
                clearShoulderRecordOnStrip(member, slotId);
            }
        }

        ArondightState.setCurrentForm(member.getId(), targetFormId);
        ArondightWeave.apply(member);

        moved.addAll(stripOpOverflowToCargo(member));

        member.setStatUpdateNeeded(true);
        if (charge > 0) {
            ArondightUndo.recordFormSelect(member.getId(), targetFormId, fromForm, wasUnlocked,
                    charge, bonusAdded, deferredAdded);
        }
        return TransformResult.ok(moved);
    }

    static List<String> stripOpOverflowToCargo(FleetMemberAPI member) {
        List<String> moved = new ArrayList<>();
        ShipVariantAPI variant = member.getVariant();
        MutableCharacterStatsAPI stats = playerStats();
        if (variant == null || stats == null) return moved;
        int safety = 64;
        while (variant.getUnusedOP(stats) < 0 && safety-- > 0) {
            String worstSlot = null;
            WeaponSpecAPI worstSpec = null;
            for (String slotId : new ArrayList<>(variant.getNonBuiltInWeaponSlots())) {
                String weaponId = variant.getWeaponId(slotId);
                if (weaponId == null || ArondightIds.WEAPON_SEALED_COVER.equals(weaponId)) continue;
                WeaponSpecAPI spec = getWeaponSpec(weaponId);
                if (spec == null) continue;
                if (worstSpec == null || spec.getBaseValue() < worstSpec.getBaseValue()) {
                    worstSpec = spec;
                    worstSlot = slotId;
                }
            }
            if (worstSlot == null) {
                if (variant.getNumFluxCapacitors() > 0 || variant.getNumFluxVents() > 0) {
                    variant.setNumFluxCapacitors(0);
                    variant.setNumFluxVents(0);
                    moved.add(ArondightStrings.forms("form_moved_flux"));
                    continue;
                }
                break;
            }
            moveToCargo(variant.getWeaponId(worstSlot), worstSpec, moved);
            variant.clearSlot(worstSlot);
            clearShoulderRecordOnStrip(member, worstSlot);
        }
        if (!moved.isEmpty()) {
            member.setStatUpdateNeeded(true);
        }
        return moved;
    }

    static FleetMemberAPI createFrameMember() {
        FleetMemberAPI member = null;
        try {
            member = Global.getFactory().createFleetMember(FleetMemberType.SHIP,
                    ArondightIds.FORM_BASE + "_Standard");
        } catch (Exception e) {
            try {
                member = Global.getFactory().createFleetMember(FleetMemberType.SHIP,
                        ArondightIds.FORM_BASE + "_Hull");
            } catch (Exception e2) {
                return null;
            }
        }
        FleetEncounterContext.prepareShipForRecovery(member, true, true, false, 1f, 1f, new Random());
        ArondightState.createFrameState(member.getId());
        ArondightWeave.apply(member);
        return member;
    }

    private static void clearShoulderRecordOnStrip(FleetMemberAPI member, String slotId) {
        if (ArondightIds.SLOT_SHOULDER_L.equals(slotId)
                || ArondightIds.SLOT_SHOULDER_R.equals(slotId)) {
            ArondightState.setShoulderFit(member.getId(), slotId, null);
        }
    }

    private static boolean isShoulderKeptByWeave(FleetMemberAPI member, String slotId) {
        String nodeId = null;
        if (ArondightIds.SLOT_SHOULDER_L.equals(slotId)) nodeId = ArondightIds.NODE_SHOULDER_LEFT;
        if (ArondightIds.SLOT_SHOULDER_R.equals(slotId)) nodeId = ArondightIds.NODE_SHOULDER_RIGHT;
        return nodeId != null && ArondightState.isNodeWoven(member.getId(), nodeId);
    }

    private static WeaponSlotAPI findSlot(ShipHullSpecAPI spec, String slotId) {
        for (WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            if (slotId.equals(slot.getId())) return slot;
        }
        return null;
    }

    private static WeaponSpecAPI getWeaponSpec(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId);
        } catch (Exception e) {
            return null;
        }
    }

    private static void moveToCargo(String weaponId, WeaponSpecAPI spec, List<String> moved) {
        if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
            Global.getSector().getPlayerFleet().getCargo().addWeapons(weaponId, 1);
        }
        moved.add(spec != null ? spec.getWeaponName() : weaponId);
    }

    private static MutableCharacterStatsAPI playerStats() {
        if (Global.getSector() == null || Global.getSector().getPlayerPerson() == null) return null;
        return Global.getSector().getPlayerPerson().getStats();
    }
}
