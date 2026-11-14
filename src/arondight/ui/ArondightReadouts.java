package arondight.ui;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightBudget;
import arondight.state.ArondightState;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightWeave;

final class ArondightReadouts {

  static final String BTN_INVESTED = "arondight_btn_noop_readout_invested";
  static final String BTN_DP = "arondight_btn_noop_readout_dp";
  private static final String[] BTN_COST = { "arondight_btn_noop_readout_supplies",
      "arondight_btn_noop_readout_recovery", BTN_DP };

  static final String AWAY = "---";

  private static final float TIP_W = 320f;
  private static final float GRID_VALUE_W = 40f;
  private static final float GRID_VALUE_PAD = 10f;
  private static final float CAPTION_H = 16f;
  private static final float VALUE_H = 24f;

  private ArondightReadouts() {
  }

  static int storyPoints() {
    return Global.getSector().getPlayerStats().getStoryPoints();
  }

  static int invested(String memberId) {
    return memberId == null ? 0 : ArondightBudget.getInvestedSp(memberId);
  }

  static int cap() {
    return ArondightBudget.getBudget();
  }

  static String investedText(String memberId) {
    return invested(memberId) + " / " + cap();
  }

  static String costCaption(int kind) {
    switch (kind) {
      case ArondightFrame.COST_SUPPLIES:
        return ArondightStrings.console("console_supplies_caption");
      case ArondightFrame.COST_RECOVERY:
        return ArondightStrings.console("console_recovery_caption");
      default:
        return ArondightStrings.console("console_dp_caption");
    }
  }

  static float cost(FleetMemberAPI member, int kind) {
    MutableShipStatsAPI stats = member.getStats();
    switch (kind) {
      case ArondightFrame.COST_SUPPLIES:
        return stats.getSuppliesPerMonth().getModifiedValue();
      case ArondightFrame.COST_RECOVERY:
        return member.getDeploymentCostSupplies();
      default:
        return member.getDeploymentPointsCost();
    }
  }

  static float baseCost(FleetMemberAPI member, int kind) {
    MutableShipStatsAPI stats = member.getStats();
    return kind == ArondightFrame.COST_SUPPLIES ? stats.getSuppliesPerMonth().getBaseValue()
        : stats.getSuppliesToRecover().getBaseValue();
  }

  static String costText(float value) {
    return Misc.getRoundedValueMaxOneAfterDecimal(value);
  }

  static int appliedNodes(FleetMemberAPI member) {
    return ArondightWeave.readWovenTags(member.getHullSpec()).size();
  }

  static String selectedForms(String memberId) {
    StringBuilder names = new StringBuilder();
    for (String formId : ArondightTransformer.getAllForms()) {
      if (ArondightTransformer.getTierOf(formId) != 0
          && ArondightState.isFormUnlocked(memberId, formId)) {
        if (names.length() > 0) {
          names.append(", ");
        }
        names.append(ArondightFormPane.formName(formId));
      }
    }
    if (names.length() == 0) {
      return ArondightFormPane.formName(ArondightState.getCurrentForm(memberId));
    }
    return names.toString();
  }

  private static String nodesLabel(int count) {
    return ArondightStrings.console(count == 1 ? "console_nodes_one" : "console_nodes_many",
        count);
  }

  private static String liveMemberId(FleetMemberAPI member) {
    if (member != null) {
      return member.getId();
    }
    List<String> ids = ArondightState.getFrameMemberIds();
    return ids.isEmpty() ? null : ids.get(0);
  }

  static void add(CustomPanelAPI pane, ArondightFrame f, FleetMemberAPI member,
      String memberId) {
    float x = ArondightFrame.READOUT_X;
    float base = f.heroH;

    String spCaption = ArondightStrings.console("console_sp_caption");
    String spValue = "" + storyPoints();
    float spCaptionY = base - ArondightFrame.SP_CAPTION_UP;
    ArondightUi.line(pane, spCaption, ArondightUi.FONT_CAPTION, ArondightUi.base(), x,
        spCaptionY, false);
    LabelAPI spLabel = ArondightUi.line(pane, spValue, ArondightUi.FONT_VALUE,
        ArondightUi.story(), x, base - ArondightFrame.SP_VALUE_UP, false);

    String investedCaption = ArondightStrings.console("console_invested_caption");
    String invested = "" + invested(memberId);
    String investedValue = investedText(memberId);
    float lowCaptionY = base - ArondightFrame.LOW_CAPTION_UP;
    float lowValueY = base - ArondightFrame.LOW_VALUE_UP;
    ArondightUi.line(pane, investedCaption, ArondightUi.FONT_CAPTION, ArondightUi.base(), x,
        lowCaptionY, false);
    LabelAPI investedLabel = ArondightUi.line(pane, investedValue, ArondightUi.FONT_VALUE,
        ArondightUi.text(), x, lowValueY, false);
    investedLabel.setHighlight(invested);
    investedLabel.setHighlightColor(ArondightUi.story());
    int cap = cap();
    float pipsW = ArondightPips.width(cap);
    if (pipsW > 0f) {
      CustomPanelAPI pips = pane.createCustomPanel(pipsW, ArondightFrame.PIP_H,
          new ArondightPips(invested(memberId), cap));
      pane.addComponent(pips).inTL(x, base - ArondightFrame.PIPS_UP);
    }
    float investedW = Math.max(pipsW, Math.max(
        ArondightUi.textWidth(pane, ArondightUi.FONT_CAPTION, investedCaption),
        ArondightUi.textWidth(pane, ArondightUi.FONT_VALUE, investedValue)));
    float blockW = Math.max(investedW, Math.max(
        ArondightUi.textWidth(pane, ArondightUi.FONT_CAPTION, spCaption),
        ArondightUi.textWidth(pane, ArondightUi.FONT_VALUE, spValue)));
    addHover(pane, BTN_INVESTED, x, spCaptionY, blockW,
        ArondightFrame.SP_CAPTION_UP - ArondightFrame.PIPS_UP + ArondightFrame.PIP_H,
        investedTip());

    LabelAPI dpLabel = null;
    if (ArondightSettings.weaveLogisticsStep() > 0) {
      int n = ArondightFrame.COST_COUNT;
      String[] captions = new String[n];
      String[] values = new String[n];
      float[] widths = new float[n];
      for (int i = 0; i < n; i++) {
        captions[i] = costCaption(i);
        values[i] = member == null ? AWAY : costText(cost(member, i));
        widths[i] = Math.max(
            ArondightUi.textWidth(pane, ArondightUi.FONT_CAPTION, captions[i]),
            ArondightUi.textWidth(pane, ArondightUi.FONT_VALUE, values[i]));
      }
      float[] rights = ArondightFrame.costRights(f.costsRight(), widths);
      for (int i = 0; i < n; i++) {
        ArondightUi.line(pane, captions[i], ArondightUi.FONT_CAPTION, ArondightUi.base(),
            rights[i], lowCaptionY, true);
        LabelAPI value = ArondightUi.line(pane, values[i], ArondightUi.FONT_VALUE,
            ArondightUi.bright(), rights[i], lowValueY, true);
        if (i == ArondightFrame.COST_DP) {
          dpLabel = value;
        }
        addHover(pane, BTN_COST[i], rights[i] - widths[i], lowCaptionY, widths[i],
            CAPTION_H + VALUE_H, costTip(i));
      }
    }

    if (ArondightConsolePanel.hasLive()) {
      liveSp = spLabel;
      liveInvested = investedLabel;
      liveDp = dpLabel;
    } else {
      dropLive();
    }

    if (member == null) {
      ArondightUi.line(pane, ArondightStrings.console("console_not_with_fleet"), null,
          ArondightUi.warn(), ArondightFrame.READOUT_X, 12f, false);
    }
  }

  private static void addHover(CustomPanelAPI pane, String id, float x, float y, float w,
      float h, ArondightUi.Tip tip) {
    TooltipMakerAPI overlay = pane.createUIElement(w, h, false);
    ButtonAPI hit = ArondightUi.addInvisibleButton(overlay, id, w, h);
    ArondightUi.setEnabledSayingWhy(hit, false);
    overlay.addTooltipToPrevious(tip, TooltipMakerAPI.TooltipLocation.ABOVE, false);
    pane.addUIElement(overlay).inTL(x - ArondightUi.INSET, y);
  }

  static ArondightUi.Tip investedTip() {
    return new ArondightUi.Tip(TIP_W) {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        String memberId = liveMemberId(ArondightState.findPlayerFrame());
        t.addTitle(ArondightStrings.console("console_invested_title"));
        int invested = invested(memberId);
        int cap = cap();
        LabelAPI total = ArondightUi.addHlPara(t,
            ArondightStrings.weave("weave_invested", invested, cap), 10f,
            "" + invested, "" + cap);
        total.setHighlightColor(ArondightUi.story());
        int onForms = memberId == null ? 0 : ArondightState.getSpentFormSp(memberId);
        int onNodes = memberId == null ? 0 : ArondightState.getSpentNodeSp(memberId);
        if (onForms > 0 || onNodes > 0) {
          t.beginGridFlipped(TIP_W, 1, ArondightUi.text(), GRID_VALUE_W, GRID_VALUE_PAD);
          int row = 0;
          if (onForms > 0) {
            t.addToGrid(0, row++, selectedForms(memberId), "+" + onForms);
          }
          if (onNodes > 0) {
            t.addToGrid(0, row++, nodesLabel(ArondightState.getWovenCount(memberId)),
                "+" + onNodes);
          }
          t.addGrid(3f);
        }
        t.addPara(ArondightStrings.console("console_invest_cap", ArondightBudget.BASE),
            ArondightUi.text(), 10f);
      }
    };
  }

  static ArondightUi.Tip dpTip() {
    return costTip(ArondightFrame.COST_DP);
  }

  static ArondightUi.Tip costTip(final int kind) {
    return new ArondightUi.Tip(TIP_W) {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        int step = ArondightSettings.weaveLogisticsStep();
        String caption = costCaption(kind);
        t.addTitle(caption);
        if (member == null) {
          t.addPara(ArondightStrings.console("console_not_with_fleet"), ArondightUi.warn(),
              10f);
        } else {
          float total = cost(member, kind);
          int nodes = appliedNodes(member);
          int share = nodes * step;
          float form = baseCost(member, kind) - share;
          float other = total - form - share;
          String totalText = costText(total);
          ArondightUi.addHlPara(t, ArondightStrings.console("console_cost_total", caption,
              totalText), 10f, totalText);
          t.beginGridFlipped(TIP_W, 1, ArondightUi.text(), GRID_VALUE_W, GRID_VALUE_PAD);
          int row = 0;
          t.addToGrid(0, row++,
              ArondightFormPane.formName(ArondightState.getCurrentForm(member.getId())),
              costText(form));
          if (share > 0) {
            t.addToGrid(0, row++, nodes == 1 ? nodesLabel(nodes)
                : ArondightStrings.console("console_nodes_each", nodes, step), "+" + share);
          }
          if (Math.abs(other) >= 0.05f) {
            t.addToGrid(0, row++, ArondightStrings.console("console_dp_other"),
                (other > 0 ? "+" : "") + costText(other));
          }
          t.addGrid(3f);
        }
        if (step > 0) {
          t.addPara(ArondightStrings.weave("weave_header_logistics", step),
              ArondightUi.text(), 10f);
        }
      }
    };
  }

  private static LabelAPI liveSp;
  private static LabelAPI liveInvested;
  private static LabelAPI liveDp;

  public static String[] liveTextsForTest() {
    if (liveSp == null && liveInvested == null && liveDp == null) {
      return null;
    }
    return new String[] { textOf(liveSp), textOf(liveInvested), textOf(liveDp) };
  }

  private static String textOf(LabelAPI label) {
    return label == null ? null : label.getText();
  }

  static void dropLive() {
    liveSp = null;
    liveInvested = null;
    liveDp = null;
  }
}
