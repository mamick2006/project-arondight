package arondight.ui;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightBudget;
import arondight.state.ArondightState;
import arondight.util.ArondightLog;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightWeave;

public final class ArondightFormPane {

  public static final String BTN_TRANSFORM_PREFIX = "arondight_btn_transform_";

  private static final float PAD = 10f;
  private static final float GAP = 6f;
  private static final float TILE_W_MAX = 130f;
  private static final float TILE_INSET = 6f;
  private static final float NAME_GAP = 4f;
  private static final float NAME_BAND_H = 28f;
  private static final float SHIP_NUDGE_LEFT = 7f;
  private static final float BOT_PAD = 20f;
  private static final float ROW_GAP = 22f;
  private static final float SECTION_GAP = 10f;
  private static final float FORMS_HEAD_H = 30f;
  private static final float LINE_GAP = 3f;
  private static final float ILL_MIN_H = 60f;

  private ArondightFormPane() {
  }

  public static String formName(String formId) {
    return ArondightTransformer.getFormDisplayName(formId);
  }

  public static void add(CustomPanelAPI host, float x, float w, float h,
      FleetMemberAPI member, String memberId) {
    ArondightGraphLayer graph = new ArondightGraphLayer();
    CustomPanelAPI pane = host.createCustomPanel(w, h, graph);
    host.addComponent(pane).inTL(x, 0f);

    float innerW = w - 2f * PAD;

    float tileW = Math.min(TILE_W_MAX, (w - 2f * PAD - 5f * GAP) / 6f);
    float shipCell = tileW - 2f * TILE_INSET;
    float tileH = TILE_INSET + shipCell + NAME_GAP + NAME_BAND_H + TILE_INSET;
    float formsH = FORMS_HEAD_H + 3f * tileH + 2f * ROW_GAP + BOT_PAD;

    int step = ArondightSettings.weaveLogisticsStep();
    TooltipMakerAPI block = pane.createUIElement(innerW, 110f, false);
    block.addPara(ArondightStrings.forms("form_header_costs"), ArondightUi.text(), 0f);
    float meterRowY = block.getHeightSoFar() + LINE_GAP;
    block.addPara(ArondightStrings.weave("weave_header_cost"), ArondightUi.text(),
        LINE_GAP);
    block.addPara(ArondightStrings.weave("weave_header_logistics", step),
        ArondightUi.text(), LINE_GAP);
    block.addPara(ArondightStrings.console("console_invest_cap"), ArondightUi.text(),
        LINE_GAP);
    float blockH = block.getHeightSoFar();

    float yCursor = 4f;
    SpriteAPI ill = illustration();
    float illAvail = h - yCursor - blockH - formsH - 2f * SECTION_GAP;
    if (ill != null && ill.getHeight() > 0f && illAvail >= ILL_MIN_H) {
      float aspect = ill.getWidth() / ill.getHeight();
      float illH = Math.min(illAvail, innerW / aspect);
      float illW = illH * aspect;
      TooltipMakerAPI illEl = pane.createUIElement(illW, illH, false);
      illEl.addImage(ILLUSTRATION, illW, illH, 0f);
      pane.addUIElement(illEl).inTL((w - illW) / 2f, yCursor);
      yCursor += illH + SECTION_GAP;
    }

    pane.addUIElement(block).inTL(PAD, yCursor);

    int invested = memberId == null ? 0 : ArondightBudget.getInvestedSp(memberId);
    int budget = ArondightBudget.getBudget();
    TooltipMakerAPI meter = pane.createUIElement(w * 0.4f, 20f, false);
    LabelAPI meterLbl = ArondightUi.addHlPara(meter,
        ArondightStrings.weave("weave_invested", invested, budget), 0f,
        "" + invested, "" + budget);
    meterLbl.setAlignment(Alignment.RMID);
    meterLbl.setHighlightColor(ArondightUi.story());
    pane.addUIElement(meter).inTR(PAD, yCursor + meterRowY);
    yCursor += blockH + SECTION_GAP;

    TooltipMakerAPI head = pane.createUIElement(innerW, 24f, false);
    head.addSectionHeading("Forms", Alignment.MID, 0f);
    pane.addUIElement(head).inTL(PAD, yCursor);
    String current = memberId == null ? null : ArondightState.getCurrentForm(memberId);

    float y0 = yCursor + FORMS_HEAD_H;
    float rowGap = ROW_GAP;
    float gridW = 6f * tileW + 5f * GAP;
    float x0 = Math.max(PAD, (w - gridW) / 2f);
    List<String> forms = ArondightTransformer.getAllForms();

    float[] xs = new float[10];
    float[] ys = new float[10];
    xs[0] = (w - tileW) / 2f;
    ys[0] = y0;
    float y1 = y0 + tileH + rowGap;
    for (int i = 0; i < 3; i++) {
      float pairLeft = x0 + (2 * i) * (tileW + GAP);
      float pairW = 2f * tileW + GAP;
      xs[1 + i] = pairLeft + (pairW - tileW) / 2f;
      ys[1 + i] = y1;
    }
    float y2 = y1 + tileH + rowGap;
    for (int i = 0; i < 6; i++) {
      xs[4 + i] = x0 + i * (tileW + GAP);
      ys[4 + i] = y2;
    }

    for (int parent = 0; parent < 4; parent++) {
      List<Integer> kids = new java.util.ArrayList<Integer>();
      for (int child = 1; child < 10; child++) {
        if (parent == forms.indexOf(ArondightTransformer.getParentForm(forms.get(child)))) {
          kids.add(child);
        }
      }
      int k = kids.size();
      for (int j = 0; j < k; j++) {
        int child = kids.get(j);
        float exitX = xs[parent] + tileW / 2f
            + (k > 1 ? (j / (float) (k - 1) - 0.5f) * tileW / 2f : 0f);
        graph.addElbow(exitX, ys[parent] + tileH,
            xs[child] + tileW / 2f, ys[child], ys[child] - rowGap / 2f,
            edgeColor(forms.get(child), member, current));
      }
    }
    for (int i = 0; i < 10; i++) {
      addTile(pane, xs[i], ys[i], tileW, tileH, shipCell, forms.get(i),
          member, memberId, current);
    }
  }

  private static final String ILLUSTRATION =
      "graphics/arondight/illustrations/frame_hangar.jpg";
  private static boolean illTried = false;
  private static SpriteAPI illSprite = null;

  private static SpriteAPI illustration() {
    if (!illTried) {
      illTried = true;
      try {
        Global.getSettings().loadTexture(ILLUSTRATION);
        illSprite = Global.getSettings().getSprite(ILLUSTRATION);
      } catch (Throwable t) {
        ArondightLog.warnOnce("form_pane_illustration",
            "console illustration failed to load (" + ILLUSTRATION
                + "): " + t + "; the console renders without it");
      }
    }
    return illSprite;
  }

  private static Color edgeColor(String formId, FleetMemberAPI member,
      String currentFormId) {
    if (formId.equals(currentFormId)) {
      return ArondightUi.good();
    }
    if (ArondightTransformer.getFormSelectState(member, formId)
        == ArondightTransformer.FormSelectState.SELECT_OK) {
      return ArondightUi.selectWhite();
    }
    return new Color(70, 78, 82);
  }

  private static void addTile(CustomPanelAPI pane, float x, float y, float tw, float th,
      float shipCell, String formId, FleetMemberAPI member, String memberId,
      String currentFormId) {
    boolean isCurrent = formId.equals(currentFormId);

    ArondightTransformer.FormSelectState sel =
        ArondightTransformer.getFormSelectState(member, formId);
    String verbData = sel == ArondightTransformer.FormSelectState.SELECT_OK
        ? BTN_TRANSFORM_PREFIX + formId : null;
    boolean actionable = verbData != null;

    ArondightNodeChrome.State state = isCurrent ? ArondightNodeChrome.State.CURRENT
        : actionable ? ArondightNodeChrome.State.ACTIVATABLE
            : ArondightNodeChrome.State.LOCKED;
    boolean dimmed = !isCurrent && !actionable;
    CustomPanelAPI tile = pane.createCustomPanel(tw, th, new ArondightNodeChrome(state));
    pane.addComponent(tile).inTL(x, y);

    String name = formName(formId);

    addShipRender(tile, TILE_INSET - SHIP_NUDGE_LEFT, TILE_INSET, shipCell, formId);

    TooltipMakerAPI nameEl = tile.createUIElement(tw, NAME_BAND_H, false);
    nameEl.setParaFont("graphics/fonts/victor10.fnt");
    LabelAPI nameLbl = nameEl.addPara(name, 2f);
    nameLbl.setAlignment(Alignment.MID);
    nameLbl.setColor(isCurrent ? ArondightUi.good()
        : dimmed ? ArondightUi.gray() : ArondightUi.text());
    if (name.indexOf(' ') < 0) {
      fitLabelToOneLine(nameLbl, name, tw - 14f);
    }
    nameEl.setParaFontDefault();
    tile.addUIElement(nameEl).inTL(0f, TILE_INSET + shipCell + NAME_GAP);

    if (dimmed) {
      CustomPanelAPI dim = tile.createCustomPanel(tw, th, new ArondightDimLayer());
      tile.addComponent(dim).inTL(0f, 0f);
    }

    TooltipMakerAPI overlay = tile.createUIElement(tw, th, false);
    ButtonAPI hit = overlay.addButton("",
        verbData != null ? verbData : "arondight_btn_noop_" + formId,
        ArondightUi.CLEAR, ArondightUi.CLEAR, Alignment.MID, null, tw, th, 0f);
    ArondightUi.setEnabledSayingWhy(hit, actionable);
    FleetMemberAPI tipMember = isCurrent && member != null ? member
        : tooltipMember(formId, memberId);
    boolean attached = false;
    if (tipMember != null) {
      attached = ArondightShipTooltips.attachExpanded(hit, tipMember,
          commanderStats(member), tipNote(formId));
    }
    if (!attached) {
      overlay.addTooltipToPrevious(fallbackTip(formId, tipMember),
          TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
    tile.addUIElement(overlay).inTL(0f, 0f);
  }

  private static ArondightShipTooltips.TipNote tipNote(final String formId) {
    return new ArondightShipTooltips.TipNote() {
      @Override
      public String text() {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        String memberId = member != null ? member.getId() : null;
        if (memberId == null) {
          List<String> ids = ArondightState.getFrameMemberIds();
          memberId = ids.isEmpty() ? null : ids.get(0);
        }
        String current = memberId == null ? null : ArondightState.getCurrentForm(memberId);
        if (formId.equals(current)) {
          return ArondightStrings.forms("form_block_current");
        }
        if (ArondightTransformer.getFormSelectState(member, formId)
            == ArondightTransformer.FormSelectState.SELECT_OK) {
          return ArondightStrings.forms("form_hover_transform");
        }
        return ArondightTransformer.getSelectBlockReason(member, formId);
      }

      @Override
      public boolean isWarn() {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        String memberId = member != null ? member.getId() : null;
        if (memberId == null) {
          List<String> ids = ArondightState.getFrameMemberIds();
          memberId = ids.isEmpty() ? null : ids.get(0);
        }
        String current = memberId == null ? null : ArondightState.getCurrentForm(memberId);
        if (formId.equals(current)) {
          return false;
        }
        return ArondightTransformer.getFormSelectState(member, formId)
            != ArondightTransformer.FormSelectState.SELECT_OK;
      }
    };
  }

  private static MutableCharacterStatsAPI commanderStats(FleetMemberAPI member) {
    if (member != null && member.getCaptain() != null && !member.getCaptain().isDefault()) {
      return member.getCaptain().getStats();
    }
    return Global.getSector() == null ? null : Global.getSector().getPlayerStats();
  }

  private static void fitLabelToOneLine(LabelAPI label, String full, float maxWidth) {
    try {
      if (label.computeTextWidth(full) <= maxWidth) {
        return;
      }
      String s = full;
      while (s.length() > 3 && label.computeTextWidth(s + "…") > maxWidth) {
        s = s.substring(0, s.length() - 1);
      }
      label.setText(s + "…");
    } catch (Throwable ignore) {
    }
  }

  private static final Map<String, FleetMemberAPI> DISPLAY_MEMBERS =
      new HashMap<String, FleetMemberAPI>();

  private static final Map<String, FleetMemberAPI> TOOLTIP_MEMBERS =
      new HashMap<String, FleetMemberAPI>();

  static void resetStaticsForLoad() {
    DISPLAY_MEMBERS.clear();
    TOOLTIP_MEMBERS.clear();
  }

  private static FleetMemberAPI tooltipMember(String formId, String memberId) {
    FleetMemberAPI m = TOOLTIP_MEMBERS.get(formId);
    if (m == null) {
      try {
        m = Global.getFactory().createFleetMember(
            com.fs.starfarer.api.fleet.FleetMemberType.SHIP, formId + "_Standard");
      } catch (Throwable noStandard) {
        try {
          m = Global.getFactory().createFleetMember(
              com.fs.starfarer.api.fleet.FleetMemberType.SHIP, formId + "_Hull");
        } catch (Throwable noHull) {
          m = displayMember(formId);
        }
      }
      if (m == null) {
        return null;
      }
      TOOLTIP_MEMBERS.put(formId, m);
    }
    try {
      java.util.Set<String> woven = memberId == null
          ? java.util.Collections.<String>emptySet()
          : ArondightState.getWovenNodes(memberId);
      if (m.getVariant() != null) {
        ArondightWeave.applyNodesToVariant(m.getVariant(), woven);
      }
      m.updateStats();
    } catch (Throwable ignore) {
    }
    return m;
  }

  private static FleetMemberAPI displayMember(String formId) {
    FleetMemberAPI cached = DISPLAY_MEMBERS.get(formId);
    if (cached != null) {
      return cached;
    }
    try {
      ShipHullSpecAPI spec = Global.getSettings().getHullSpec(formId);
      if (spec == null) {
        return null;
      }
      ShipVariantAPI variant = Global.getSettings().createEmptyVariant(
          "arondight_display_" + formId, spec);
      FleetMemberAPI m = Global.getFactory().createFleetMember(
          com.fs.starfarer.api.fleet.FleetMemberType.SHIP, variant);
      DISPLAY_MEMBERS.put(formId, m);
      return m;
    } catch (Throwable notRegistered) {
      return null;
    }
  }

  private static void addShipRender(CustomPanelAPI tile, float x, float y, float cell,
      String formId) {
    float renderCell = cell >= 96f ? 96f : cell >= 48f ? 48f : cell;
    x += (cell - renderCell) / 2f;
    y += (cell - renderCell) / 2f;
    CustomPanelAPI box = tile.createCustomPanel(renderCell, renderCell, null);
    FleetMemberAPI m = displayMember(formId);
    if (m != null) {
      java.util.List<FleetMemberAPI> one = new java.util.ArrayList<FleetMemberAPI>(1);
      one.add(m);
      TooltipMakerAPI ship = box.createUIElement(renderCell, renderCell, false);
      ship.addShipList(1, 1, renderCell, ArondightUi.alpha(ArondightUi.dark(), 0), one,
          0f);
      box.addUIElement(ship).inTL(0f, 0f);
    } else {
      TooltipMakerAPI ph = box.createUIElement(renderCell, renderCell, false);
      LabelAPI l = ph.addPara("[ hull", ArondightUi.gray(),
          Math.max(0f, renderCell / 2f - 18f));
      l.setAlignment(Alignment.MID);
      LabelAPI l2 = ph.addPara("pending ]", ArondightUi.gray(), 1f);
      l2.setAlignment(Alignment.MID);
      box.addUIElement(ph).inTL(0f, 0f);
    }
    tile.addComponent(box).inTL(x, y);
  }

  private static ArondightUi.Tip fallbackTip(final String formId,
      final FleetMemberAPI tipMember) {
    final ArondightShipTooltips.TipNote note = tipNote(formId);
    return new ArondightUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        t.addTitle(formName(formId));
        String line = note.text();
        if (line != null) {
          t.addPara(line, note.isWarn() ? ArondightUi.warn() : ArondightUi.gray(), 10f);
        }
        if (tipMember != null) {
          try {
            t.setCodexEntryFleetMember(tipMember);
          } catch (Throwable ignore) {
          }
        }
      }
    };
  }

}
