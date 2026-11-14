package arondight.hullmods;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightIds;
import arondight.ui.ArondightUi;
import arondight.ui.ArondightWeavePane;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightNode;
import arondight.weave.ArondightWeave;

public class MorphogenicLattice extends FormBaseHullMod {

    private static final String[] BLOCKED_CREW_MODS = {
            "additional_crew_quarters", "expanded_deck_crew"};

    private static final float ROW_ICON = 24f;
    private static final float ROW_GAP = 3f;
    private static final boolean ROW_ICON_CENTRED = true;
    private static final float ROW_TEXT_PAD = ROW_ICON_CENTRED ? 0f : 4f;
    private static final float BELOW_LIST = 100f;
    private static final float REST_GAP = 10f;

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
                    ArondightSettings.getFloat("arondight_zero_point_flux_level", 0.15f));
        }
        if (nodes.contains(ArondightIds.NODE_FARADAY_MESH)) {
            stats.getEmpDamageTakenMult().modifyMult(id,
                    ArondightSettings.getFloat("arondight_faraday_emp_mult", 0.5f));
        }
        if (nodes.contains(ArondightIds.NODE_DOCK_CYCLE)) {
            stats.getBaseCRRecoveryRatePercentPerDay().modifyPercent(id,
                    ArondightSettings.getFloat("arondight_dock_cr_recovery_pct", 100f));
        }
        if (nodes.contains(ArondightIds.NODE_PURGE_VALVES)) {
            stats.getVentRateMult().modifyPercent(id,
                    ArondightSettings.getFloat("arondight_purge_vent_pct", 25f));
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
        List<ArondightNode> woven = new ArrayList<>();
        for (ArondightNode node : ArondightWeave.getAllNodes()) {
            if (nodes.contains(node.getId())) woven.add(node);
        }
        float room = Global.getSettings().getScreenHeight() - BELOW_LIST
                - tooltip.getHeightSoFar();
        boolean rows = rowsFit(tooltip, ship, woven, width, room);
        int listed = rows ? woven.size() : plainLinesThatFit(tooltip, ship, woven, width, room);
        for (ArondightNode node : woven.subList(0, listed)) {
            String icon = rows
                    ? ArondightUi.sprite(ArondightWeavePane.ICON_CATEGORY, node.getId()) : null;
            LabelAPI line;
            if (icon != null) {
                line = beginRow(tooltip, icon, width)
                        .addPara(node.getName() + ": " + effect(node, ship), ROW_TEXT_PAD);
                tooltip.addImageWithText(ROW_GAP);
            } else {
                line = tooltip.addPara(plainLine(node, ship), rows ? ROW_GAP : 0f);
            }
            line.setHighlightColor(ArondightUi.accent());
            line.highlightFirst(node.getName());
        }
        if (listed < woven.size()) {
            String[] names = namesFrom(woven, listed);
            LabelAPI rest = tooltip.addPara(restLine(names), REST_GAP);
            rest.setHighlightColor(ArondightUi.accent());
            rest.setHighlight(names);
        }
    }

    private static String plainLine(ArondightNode node, ShipAPI ship) {
        return "   " + node.getName() + ": " + effect(node, ship);
    }

    private static String[] namesFrom(List<ArondightNode> woven, int from) {
        String[] names = new String[woven.size() - from];
        for (int i = 0; i < names.length; i++) {
            names[i] = woven.get(from + i).getName();
        }
        return names;
    }

    private static String restLine(String[] names) {
        return arondight.ArondightStrings.hullmods("hm_woven_rest", String.join(", ", names));
    }

    private static String effect(ArondightNode node, ShipAPI ship) {
        String desc = node.getDescription();
        if (ArondightIds.NODE_CHARGE_CELL.equals(node.getId())
                && ship.getHullSpec() != null
                && ArondightIds.SYSTEM_SPOTTER.equals(ship.getHullSpec().getShipSystemId())
                && !desc.contains("holds no charges")) {
            desc = desc + " The spotter lattice holds no charges.";
        }
        return desc;
    }

    private static TooltipMakerAPI beginRow(TooltipMakerAPI tooltip, String icon, float width) {
        return tooltip.beginImageWithText(icon, ROW_ICON, width, ROW_ICON_CENTRED);
    }

    private static boolean rowsFit(TooltipMakerAPI tooltip, ShipAPI ship,
                                   List<ArondightNode> woven, float width, float room) {
        float list = 0f;
        for (ArondightNode node : woven) {
            String icon = ArondightUi.sprite(ArondightWeavePane.ICON_CATEGORY, node.getId());
            float text = 0f;
            if (icon != null) {
                TooltipMakerAPI row = beginRow(tooltip, icon, width);
                row.addPara(node.getName() + ": " + effect(node, ship), ROW_TEXT_PAD);
                text = row.getHeightSoFar();
            }
            list += Math.max(ROW_ICON, text) + ROW_GAP;
        }
        return list <= room;
    }

    private static int plainLinesThatFit(TooltipMakerAPI tooltip, ShipAPI ship,
                                         List<ArondightNode> woven, float width, float room) {
        float[] lines = new float[woven.size()];
        float list = 0f;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = paraHeight(tooltip, plainLine(woven.get(i), ship), width);
            list += lines[i];
        }
        if (list <= room) return lines.length;
        for (int listed = lines.length - 1; listed > 0; listed--) {
            list -= lines[listed];
            float rest = REST_GAP + paraHeight(tooltip, restLine(namesFrom(woven, listed)), width);
            if (list + rest <= room) return listed;
        }
        return 0;
    }

    private static float paraHeight(TooltipMakerAPI tooltip, String text, float width) {
        return tooltip.createLabel(text, ArondightUi.text()).autoSizeToWidth(width).getHeight();
    }
}
