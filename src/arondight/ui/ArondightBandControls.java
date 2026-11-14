package arondight.ui;

import java.util.List;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightResets;
import arondight.weave.ArondightUndo;

public final class ArondightBandControls {

  public static final String BTN_UNDO = "arondight_btn_undo";
  public static final String BTN_RESET_FORM = "arondight_btn_reset_form";
  public static final String BTN_RESET_WEAVE = "arondight_btn_reset_weave";

  private static final float CONTROL_W = ArondightFrame.CONTROL_W;
  private static final float CONTROL_H = ArondightFrame.CONTROL_H;
  private static final float GAP = ArondightFrame.CONTROL_GAP;
  private static final String[] IDS = { BTN_UNDO, BTN_RESET_FORM, BTN_RESET_WEAVE };

  private ArondightBandControls() {
  }

  static void add(CustomPanelAPI panel, float x0, float top, FleetMemberAPI member) {
    float w = ArondightFrame.controlsRowW();
    CustomPanelAPI holder = panel.createCustomPanel(w, CONTROL_H, ArondightUi.consoleRow());
    float x = 0f;
    for (String id : IDS) {
      TooltipMakerAPI el = holder.createUIElement(CONTROL_W, CONTROL_H, false);
      ButtonAPI b = el.addButton(label(id), id, ArondightUi.base(), ArondightUi.dark(),
          Alignment.MID, CutStyle.ALL, CONTROL_W, CONTROL_H, 0f);
      ArondightUi.setEnabledSayingWhy(b, blockReason(id, member) == null);
      el.addTooltipToPrevious(tip(id), TooltipMakerAPI.TooltipLocation.BELOW, false);
      holder.addUIElement(el).inTL(x - ArondightUi.INSET, 0f);
      x += CONTROL_W + GAP;
    }
    panel.addComponent(holder).inTL(x0, top);
  }

  private static String label(String id) {
    if (BTN_UNDO.equals(id)) {
      return "Undo";
    }
    return BTN_RESET_FORM.equals(id) ? "Reset Form" : "Reset Weave";
  }

  private static String blockReason(String id, FleetMemberAPI member) {
    if (BTN_UNDO.equals(id)) {
      return ArondightUndo.getUndoBlockReason(member);
    }
    return BTN_RESET_FORM.equals(id) ? ArondightResets.getResetFormBlockReason(member)
        : ArondightResets.getResetWeaveBlockReason(member);
  }

  private static ArondightUi.Tip tip(String id) {
    if (BTN_UNDO.equals(id)) {
      return undoTip();
    }
    return BTN_RESET_FORM.equals(id) ? resetFormTip() : resetWeaveTip();
  }

  static ArondightUi.Tip undoTip() {
    return new ArondightUi.Tip() {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        t.addTitle("Undo");
        String target = ArondightUndo.getUndoTargetName(member);
        if (target != null) {
          int points = ArondightUndo.getUndoRefund(member);
          if (points == 0) {
            ArondightUi.addHlPara(t, ArondightStrings.weave("undo_hover_zero", target), 10f,
                target);
          } else {
            String returned = points + (points == 1 ? " story point" : " story points");
            ArondightUi.addHlPara(t, points == 1
                ? ArondightStrings.weave("undo_hover_one", target, points)
                : ArondightStrings.weave("undo_hover_many", target, points),
                10f, target, returned);
          }
          String landing = ArondightUndo.getUndoLandingForm(member);
          if (landing != null) {
            String landingName = ArondightTransformer.getFormDisplayName(landing);
            ArondightUi.addHlPara(t, ArondightStrings.console("respec_landing", landingName),
                6f, landingName);
          }
        }
        String reason = ArondightUndo.getUndoBlockReason(member);
        if (reason != null) {
          t.addPara(reason, ArondightUi.warn(), 10f);
        }
      }
    };
  }

  static ArondightUi.Tip resetWeaveTip() {
    return new ArondightUi.Tip() {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        t.addTitle("Reset Weave");
        t.addPara(ArondightStrings.weave("respec_hover_unwind"), ArondightUi.text(), 10f);
        addRefundLine(t, ArondightResets.getResetWeaveRefund(member),
            ArondightStrings.weave("respec_spent_on_nodes"));
        int woven = member == null ? 0 : ArondightState.getWovenCount(member.getId());
        if (woven > 0) {
          t.addPara(ArondightStrings.weave("respec_hover_shoulders"), ArondightUi.text(), 6f);
          t.addPara(ArondightStrings.weave("respec_hover_op_strip_as_well"),
              ArondightUi.text(), 6f);
        }
        addReasonAndCost(t, ArondightResets.getResetWeaveBlockReason(member));
      }
    };
  }

  static ArondightUi.Tip resetFormTip() {
    return new ArondightUi.Tip() {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        t.addTitle("Reset Form");
        List<String> relock = ArondightResets.getResetFormRelockForms(member);
        float pad = 10f;
        if (!relock.isEmpty()) {
          StringBuilder names = new StringBuilder();
          for (int i = 0; i < relock.size(); i++) {
            if (i > 0) {
              names.append(", ");
            }
            names.append(ArondightTransformer.getFormDisplayName(relock.get(i)));
          }
          t.addPara(ArondightStrings.weave("respec_hover_relock", names), ArondightUi.text(),
              pad);
          pad = 6f;
        }
        String landing = ArondightResets.getResetFormLandingForm(member);
        if (landing != null) {
          String landingName = ArondightTransformer.getFormDisplayName(landing);
          ArondightUi.addHlPara(t, ArondightStrings.console("respec_landing", landingName),
              pad, landingName);
          pad = 6f;
        }
        addRefundLine(t, ArondightResets.getResetFormRefund(member), pad,
            ArondightStrings.weave("respec_spent_on_forms"));
        if (landing != null) {
          t.addPara(ArondightStrings.weave("respec_hover_op_strip"), ArondightUi.text(), 6f);
        }
        addReasonAndCost(t, ArondightResets.getResetFormBlockReason(member));
      }
    };
  }

  private static void addRefundLine(TooltipMakerAPI t, int refund, String spentOn) {
    addRefundLine(t, refund, 6f, spentOn);
  }

  private static void addRefundLine(TooltipMakerAPI t, int refund, float pad, String spentOn) {
    if (refund > 0) {
      ArondightUi.addHlPara(t, refund == 1
          ? ArondightStrings.weave("respec_hover_refund_one", refund, spentOn)
          : ArondightStrings.weave("respec_hover_refund_many", refund, spentOn),
          pad, "" + refund);
    } else {
      t.addPara(ArondightStrings.weave("respec_hover_refund_zero", spentOn),
          ArondightUi.text(), pad);
    }
  }

  private static void addReasonAndCost(TooltipMakerAPI t, String reason) {
    if (reason != null) {
      t.addPara(reason, ArondightUi.warn(), 10f);
    }
    t.addStoryPointUseInfo(10f, ArondightSettings.respecSpCost(),
        ArondightSettings.respecBonusXp(), true);
  }
}
