package arondight.hullmods;

import java.util.Set;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightIds;
import arondight.ui.ArondightUi;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightNode;
import arondight.weave.ArondightWeave;

public class MorphogenicLattice extends FormBaseHullMod {

    private static final String[] BLOCKED_CREW_MODS = {
            "additional_crew_quarters", "expanded_deck_crew"};

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null) return;
        ArondightWeave.refreshMounts(stats.getVariant());
        Set<String> nodes = ArondightWeave.readWovenTags(stats.getVariant().getHullSpec());
        if (nodes.isEmpty()) return;

        if (nodes.contains(ArondightIds.NODE_CHARGE_CELL)) {
            stats.getSystemUsesBonus().modifyFlat(id, 1f);
        }
        if (nodes.contains(ArondightIds.NODE_QUICK_CYCLE)) {
            stats.getSystemCooldownBonus().modifyMult(id, 0.75f);
            stats.getSystemRegenBonus().modifyPercent(id, 33f);
        }
        if (nodes.contains(ArondightIds.NODE_ZERO_POINT_IDLE)) {
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(id,
                    ArondightSettings.getFloat("arondight_zero_point_flux_level", 0.05f));
        }
        if (nodes.contains(ArondightIds.NODE_LATTICE_SIGHT)) {
            stats.getSensorStrength().modifyPercent(id, 25f);
            stats.getSensorProfile().modifyMult(id, 0.75f);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        Set<String> nodes = ArondightWeave.readWovenTags(ship.getHullSpec());
        if (LatticeCombatPlugin.wantsCombatPlugin(nodes)
                && ship.getListeners(LatticeCombatPlugin.class).isEmpty()) {
            ship.addListener(new LatticeCombatPlugin(ship, nodes));
        }

        if (ship.getHullSpec() != null) {
            String hullId = ship.getHullSpec().getHullId();
            if (arondight.form.mechanics.WardMechanics.of(ship) == null) {
                arondight.form.mechanics.WardMechanics.Variant wardVariant = null;
                if (hullId.startsWith(ArondightIds.FORM_DURENDAL)) {
                    wardVariant = arondight.form.mechanics.WardMechanics.Variant.DURENDAL;
                } else if (hullId.startsWith(ArondightIds.FORM_HAUTECLERE)) {
                    wardVariant = arondight.form.mechanics.WardMechanics.Variant.FLASH;
                } else if (hullId.startsWith(ArondightIds.FORM_JOYEUSE)) {
                    wardVariant = arondight.form.mechanics.WardMechanics.Variant.BULWARK;
                }
                if (wardVariant != null) {
                    ship.addListener(new arondight.form.mechanics.WardMechanics(ship, wardVariant));
                }
            }
            if (hullId.startsWith(ArondightIds.FORM_LONGINUS)
                    && ship.getListeners(LonginusFrame.RangeMult.class).isEmpty()) {
                ship.addListener(new LonginusFrame.RangeMult());
            }
            if (hullId.startsWith(ArondightIds.FORM_GAEBOLG)
                    && ship.getListeners(arondight.form.mechanics.GaeBolgPlugin.class).isEmpty()) {
                ship.addListener(new arondight.form.mechanics.GaeBolgPlugin(ship));
            }
            if ((hullId.startsWith(ArondightIds.FORM_GAEBOLG)
                    || hullId.startsWith(ArondightIds.FORM_RHONGOMYNIAD))
                    && ship.getListeners(RangeDoubler.class).isEmpty()) {
                ship.addListener(new RangeDoubler());
            }
            if (hullId.startsWith(ArondightIds.FORM_CLARENT)
                    && ship.getListeners(arondight.form.mechanics.ClarentDrainPlugin.class).isEmpty()) {
                ship.addListener(new arondight.form.mechanics.ClarentDrainPlugin(ship));
            }
            if (hullId.startsWith(ArondightIds.FORM_RHONGOMYNIAD)
                    && ship.getListeners(arondight.form.mechanics.RhongomyniadAnchor.class).isEmpty()) {
                ship.addListener(new arondight.form.mechanics.RhongomyniadAnchor(ship));
            }
        }

        if (ship.getVariant() != null) {
            for (String blocked : BLOCKED_CREW_MODS) {
                if (ship.getVariant().getNonBuiltInHullmods().contains(blocked)) {
                    ship.getVariant().removeMod(blocked);
                }
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize,
                                          ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null || tooltip == null) return;
        Set<String> nodes = ArondightWeave.readWovenTags(ship.getHullSpec());
        if (nodes.isEmpty()) {
            tooltip.addPara(arondight.ArondightStrings.hullmods("hm_nothing_woven"), 10f);
            return;
        }
        tooltip.addPara(arondight.ArondightStrings.hullmods("hm_woven_header_count"), 10f,
                ArondightUi.accent(), String.valueOf(nodes.size()));
        for (String nodeId : nodes) {
            ArondightNode node = ArondightWeave.getNode(nodeId);
            if (node == null) continue;
            String desc = node.getDescription();
            if (ArondightIds.NODE_CHARGE_CELL.equals(nodeId)
                    && ship.getHullSpec() != null
                    && ArondightIds.SYSTEM_SPOTTER.equals(ship.getHullSpec().getShipSystemId())
                    && !desc.contains("holds no charges")) {
                desc = desc + " The spotter lattice holds no charges.";
            }
            LabelAPI line = tooltip.addPara("   " + node.getName() + ": " + desc, 3f);
            line.setHighlightColor(ArondightUi.accent());
            line.highlightFirst(node.getName());
        }
    }
}
