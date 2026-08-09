package arondight.weave;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;

import arondight.ArondightIds;
import arondight.state.ArondightState;
import arondight.util.ArondightLog;
import arondight.util.ArondightSettings;
import arondight.weave.nodes.ShoulderUnsealNode;

final class WeaveApplier {

    private WeaveApplier() {}

    static void apply(FleetMemberAPI member) {
        if (member == null || Global.getSector() == null) return;
        String memberId = member.getId();
        if (!ArondightState.isFrame(memberId)) return;
        ShipVariantAPI variant = member.getVariant();
        if (variant == null) return;

        ensurePrivateVariant(member, variant);
        variant = member.getVariant();

        String canonicalId = resolveCanonicalId(variant, ArondightState.getCurrentForm(memberId));
        ShipHullSpecAPI canonical;
        try {
            canonical = Global.getSettings().getHullSpec(canonicalId);
        } catch (Exception e) {
            canonical = null;
        }
        if (canonical == null) {
            ArondightLog.warnOnce("missing_hull_" + canonicalId, "hull spec '" + canonicalId
                    + "' is missing; the weave cannot apply to this member");
            return;
        }

        List<String> woven = new ArrayList<>(ArondightState.getWovenNodes(memberId));

        boolean fromCanonical = variant.getHullSpec() == null
                || !variant.getHullSpec().getTags().contains(ArondightIds.TAG_APPLIED);

        java.util.Map<String, String> preAssignShoulders = readShoulderSlots(variant);

        ShipHullSpecAPI clone = null;
        List<String> applied = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            clone = SpecMutators.cloneSpec(canonical);
            if (clone == null) break;
            applied = tryMutateAll(clone, variant, woven);
            if (applied != null) break;
        }

        if (clone == null) {
            variant.setHullSpecAPI(canonical);
            resealShoulders(memberId, variant, java.util.Collections.<String>emptySet(),
                    fromCanonical, preAssignShoulders);
            stripDanglingSlots(variant);
            stripSignatureModResidue(variant);
            clearAnimatedFitsOnCanonical(variant);
            member.setStatUpdateNeeded(true);
            return;
        }

        clone.addTag(ArondightIds.TAG_APPLIED);
        for (String nodeId : applied) {
            clone.addTag(ArondightIds.TAG_WEAVE_PREFIX + nodeId);
        }
        escalateLogistics(clone, applied.size());

        variant.setHullSpecAPI(clone);

        java.util.Set<String> unsealedNow = new java.util.HashSet<>();
        for (String nodeId : applied) {
            ArondightNode node = ArondightNodes.byId(nodeId);
            if (!(node instanceof ShoulderUnsealNode)) continue;
            String slotId = ((ShoulderUnsealNode) node).getSlotId();
            unsealedNow.add(slotId);

            String current = variant.getWeaponId(slotId);
            if (ArondightIds.WEAPON_SEALED_COVER.equals(current)) {
                variant.clearSlot(slotId);
                current = null;
            }

            if (current != null) {
                ArondightState.setShoulderFit(memberId, slotId, current);
            } else if (fromCanonical) {
                String recorded = ArondightState.getShoulderFit(memberId, slotId);
                if (recorded != null && weaponSpecExists(recorded)) {
                    variant.addWeapon(slotId, recorded);
                }
            } else {
                ArondightState.setShoulderFit(memberId, slotId, null);
            }
        }

        resealShoulders(memberId, variant, unsealedNow, fromCanonical, preAssignShoulders);

        stripDanglingSlots(variant);
        stripSignatureModResidue(variant);
        unhideAnimatedFits(clone, variant);
        member.setStatUpdateNeeded(true);
    }

    static final String[] FILLABLE_SLOTS = {
            ArondightIds.SLOT_ARM_L, ArondightIds.SLOT_SHOULDER_L, ArondightIds.SLOT_SHOULDER_R };

    static void unhideAnimatedFits(ShipHullSpecAPI spec, ShipVariantAPI variant) {
        if (spec == null || variant == null) return;
        for (String slotId : FILLABLE_SLOTS) {
            String weaponId = variant.getWeaponId(slotId);
            if (weaponId == null || weaponId.startsWith("arondight_")) continue;
            SpecMutators.unhideIfAnimated(spec, slotId, weaponId);
        }
    }

    private static void clearAnimatedFitsOnCanonical(ShipVariantAPI variant) {
        for (String slotId : FILLABLE_SLOTS) {
            String weaponId = variant.getWeaponId(slotId);
            if (weaponId == null || weaponId.startsWith("arondight_")) continue;
            if (!SpecMutators.isAnimatedWeapon(weaponId)) continue;
            variant.clearSlot(slotId);
            if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getCargo().addWeapons(weaponId, 1);
            }
            ArondightLog.warnOnce("degraded_mount_" + weaponId, "'" + weaponId
                    + "' is an animated weapon, and while the spec-clone capability is broken"
                    + " the frame's hidden arm/shoulder mounts cannot carry one without"
                    + " crashing at combat load; it was returned to cargo");
        }
    }

    private static void stripSignatureModResidue(ShipVariantAPI variant) {
        for (String modId : ArondightIds.FORM_HULLMODS) {
            variant.removePermaMod(modId);
            variant.removeMod(modId);
        }
        variant.removePermaMod(ArondightIds.HULLMOD_LATTICE);
        variant.removeMod(ArondightIds.HULLMOD_LATTICE);
    }

    private static void stripDanglingSlots(ShipVariantAPI variant) {
        ShipHullSpecAPI spec = variant.getHullSpec();
        if (spec == null) return;
        java.util.Set<String> valid = new java.util.HashSet<>();
        for (com.fs.starfarer.api.loading.WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            valid.add(slot.getId());
        }
        for (String slotId : new ArrayList<>(variant.getFittedWeaponSlots())) {
            if (valid.contains(slotId)) continue;
            String weaponId = variant.getWeaponId(slotId);
            variant.clearSlot(slotId);
            if (weaponId != null && !weaponId.startsWith("arondight_")
                    && Global.getSector() != null && Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getCargo().addWeapons(weaponId, 1);
            }
        }
    }

    private static final String[] SHOULDER_SLOTS =
            { ArondightIds.SLOT_SHOULDER_L, ArondightIds.SLOT_SHOULDER_R };

    private static java.util.Map<String, String> readShoulderSlots(ShipVariantAPI variant) {
        java.util.Map<String, String> fits = new java.util.HashMap<>();
        for (String slotId : SHOULDER_SLOTS) {
            String weaponId = variant.getWeaponId(slotId);
            if (ArondightIds.WEAPON_SEALED_COVER.equals(weaponId)) weaponId = null;
            fits.put(slotId, weaponId);
        }
        return fits;
    }

    private static void resealShoulders(String memberId, ShipVariantAPI variant,
            java.util.Set<String> unsealedNow, boolean fromCanonical,
            java.util.Map<String, String> preAssign) {
        for (String slotId : SHOULDER_SLOTS) {
            if (unsealedNow.contains(slotId)) continue;
            if (variant.getWeaponId(slotId) != null) {
                variant.clearSlot(slotId);
            }
            String had = preAssign.get(slotId);
            String recorded = ArondightState.getShoulderFit(memberId, slotId);
            String returned = null;
            if (had != null) {
                returned = had;
            } else if (fromCanonical && recorded != null && weaponSpecExists(recorded)) {
                returned = recorded;
            }
            if (returned != null && !returned.startsWith("arondight_")
                    && Global.getSector() != null
                    && Global.getSector().getPlayerFleet() != null) {
                Global.getSector().getPlayerFleet().getCargo().addWeapons(returned, 1);
            }
            if (recorded != null) {
                ArondightState.setShoulderFit(memberId, slotId, null);
            }
        }
    }

    private static boolean weaponSpecExists(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static void escalateLogistics(ShipHullSpecAPI clone, int appliedCount) {
        if (appliedCount <= 0) return;
        float add = appliedCount * (float) ArondightSettings.weaveLogisticsStep();
        clone.setSuppliesToRecover(clone.getSuppliesToRecover() + add);
        clone.setSuppliesPerMonth(clone.getSuppliesPerMonth() + add);
    }

    private static List<String> tryMutateAll(ShipHullSpecAPI clone, ShipVariantAPI variant, List<String> woven) {
        List<String> applied = new ArrayList<>();
        for (String nodeId : woven) {
            ArondightNode node = ArondightNodes.byId(nodeId);
            if (node == null) {
                ArondightLog.warnOnce("unknown_node_" + nodeId, "state names a weave node '" + nodeId
                        + "' this version does not know; it is kept in state and skipped");
                continue;
            }
            if (!node.isAvailable()) continue;
            try {
                node.mutate(clone, variant);
                applied.add(nodeId);
            } catch (SpecMutators.MutationFailed f) {
                return null;
            }
        }
        return applied;
    }

    private static void ensurePrivateVariant(FleetMemberAPI member, ShipVariantAPI variant) {
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            ShipVariantAPI priv = variant.clone();
            priv.setGoalVariant(false);
            priv.setSource(VariantSource.REFIT);
            member.setVariant(priv, false, true);
        }
    }

    private static String resolveCanonicalId(ShipVariantAPI variant, String formId) {
        String currentId = variant.getHullSpec() == null ? null : variant.getHullSpec().getHullId();
        if (formId == null) return currentId;
        if (currentId == null || formId.equals(currentId)) return formId;
        try {
            ShipHullSpecAPI current = Global.getSettings().getHullSpec(currentId);
            if (current != null
                    && (formId.equals(current.getDParentHullId()) || formId.equals(current.getBaseHullId()))) {
                return currentId;
            }
        } catch (Exception ignored) {
        }
        return formId;
    }

    static void applyNodesToVariant(ShipVariantAPI variant, java.util.Collection<String> nodeIds) {
        if (variant == null || variant.getHullSpec() == null) return;
        ShipHullSpecAPI canonical;
        try {
            canonical = Global.getSettings().getHullSpec(variant.getHullSpec().getHullId());
        } catch (Exception e) {
            return;
        }
        if (canonical == null) return;
        List<String> woven = new ArrayList<>(nodeIds);
        ShipHullSpecAPI clone = null;
        List<String> applied = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            clone = SpecMutators.cloneSpec(canonical);
            if (clone == null) return;
            applied = tryMutateAll(clone, variant, woven);
            if (applied != null) break;
        }
        if (applied == null) return;
        clone.addTag(ArondightIds.TAG_APPLIED);
        for (String nodeId : applied) {
            clone.addTag(ArondightIds.TAG_WEAVE_PREFIX + nodeId);
        }
        escalateLogistics(clone, applied.size());
        variant.setHullSpecAPI(clone);
        for (String nodeId : applied) {
            ArondightNode node = ArondightNodes.byId(nodeId);
            if (node instanceof ShoulderUnsealNode) {
                String slotId = ((ShoulderUnsealNode) node).getSlotId();
                if (ArondightIds.WEAPON_SEALED_COVER.equals(variant.getWeaponId(slotId))) {
                    variant.clearSlot(slotId);
                }
            }
        }
        stripDanglingSlots(variant);
        stripSignatureModResidue(variant);
        unhideAnimatedFits(clone, variant);
    }

    static void reapplyPlayerFleetIfNeeded() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return;
        if (!ArondightState.hasAnyFrame()) return;
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (!ArondightState.isFrame(member.getId())) continue;
            ShipVariantAPI variant = member.getVariant();
            if (variant == null) continue;
            ShipHullSpecAPI spec = variant.getHullSpec();
            if (spec != null && spec.getTags().contains(ArondightIds.TAG_APPLIED)) continue;
            apply(member);
        }
    }

    static void snapshotShoulderFits() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return;
        if (!ArondightState.hasAnyFrame()) return;
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            String memberId = member.getId();
            if (!ArondightState.isFrame(memberId) || member.getVariant() == null) continue;
            snapshotSlot(member, memberId, ArondightIds.NODE_SHOULDER_LEFT, ArondightIds.SLOT_SHOULDER_L);
            snapshotSlot(member, memberId, ArondightIds.NODE_SHOULDER_RIGHT, ArondightIds.SLOT_SHOULDER_R);
        }
    }

    private static void snapshotSlot(FleetMemberAPI member, String memberId, String nodeId, String slotId) {
        if (!ArondightState.isNodeWoven(memberId, nodeId)) return;
        String current = member.getVariant().getWeaponId(slotId);
        if (ArondightIds.WEAPON_SEALED_COVER.equals(current)) current = null;
        ArondightState.setShoulderFit(memberId, slotId, current);
    }
}
