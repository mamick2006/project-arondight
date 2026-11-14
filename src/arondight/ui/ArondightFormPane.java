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
import arondight.state.ArondightState;
import arondight.util.ArondightLog;
import arondight.util.ArondightReflect;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightWeave;

public final class ArondightFormPane {

  public static final String BTN_TRANSFORM_PREFIX = "arondight_btn_transform_";

  private static final float SHIP_NUDGE_LEFT = 7f;
  private static final float NAME_OVERHANG = 3f;
  private static final float NAME_RAISE = 3f;

  private ArondightFormPane() {
  }

  public static String formName(String formId) {
    return ArondightTransformer.getFormDisplayName(formId);
  }

  public static void add(CustomPanelAPI host, ArondightFrame f, FleetMemberAPI member,
      String memberId) {
    ArondightGraphLayer graph = new ArondightGraphLayer();
    CustomPanelAPI pane = host.createCustomPanel(f.leftW, f.height, graph);
    host.addComponent(pane).inTL(0f, 0f);

    SpriteAPI ill = illustration();
    if (ill != null && ill.getHeight() > 0f) {
      float[] r = f.pictureRect(ill.getWidth() / ill.getHeight());
      TooltipMakerAPI illEl = pane.createUIElement(r[2], r[3], false);
      illEl.addImage(ILLUSTRATION, r[2], r[3], 0f);
      pane.addUIElement(illEl).inTL(r[0] - ArondightUi.INSET, r[1]);
    }
    ArondightReadouts.add(pane, f, member, memberId);

    TooltipMakerAPI head = pane.createUIElement(f.leftW, ArondightFrame.HEAD_H, false);
    head.addSectionHeading("Forms", Alignment.MID, 0f);
    pane.addUIElement(head).inTL(0f, f.formsHeadY);
    int tier1 = ArondightSettings.tier1SpCost();
    int tier2 = ArondightSettings.tier2SpCost();
    if (tier1 > 0 || tier2 > 0) {
      TooltipMakerAPI cost = pane.createUIElement(f.leftW + 2f * ArondightUi.INSET, 18f,
          false);
      cost.addPara(formCostLine(tier1, tier2), ArondightUi.text(), 0f);
      pane.addUIElement(cost).inTL(-ArondightUi.INSET, f.formsCostY);
    }

    String current = memberId == null ? null : ArondightState.getCurrentForm(memberId);
    float tileW = f.tileW;
    float tileH = f.tileH;
    float rowGap = ArondightFrame.ROW_GAP;
    List<String> forms = ArondightTransformer.getAllForms();

    float[] xs = new float[10];
    float[] ys = new float[10];
    for (int i = 0; i < 10; i++) {
      xs[i] = f.tileX(i);
      ys[i] = f.tileY(i);
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
      addTile(pane, xs[i], ys[i], f, forms.get(i), member, memberId, current);
    }

    if (!ArondightState.areTreesUnlocked()) {
      CustomPanelAPI ground = pane.createCustomPanel(f.gridW, ArondightFrame.SEALED_H,
          new ArondightDimLayer(ArondightDimLayer.SEALED));
      pane.addComponent(ground).inTL(f.gridX, f.sealedY());
      TooltipMakerAPI line = pane.createUIElement(f.gridW, ArondightFrame.SEALED_H, false);
      LabelAPI sentence = line.addPara(ArondightStrings.console("console_sealed_band"),
          ArondightUi.warn(), 2f);
      sentence.setAlignment(Alignment.MID);
      pane.addUIElement(line).inTL(f.gridX, f.sealedY());
    }
  }

  private static String formCostLine(int tier1, int tier2) {
    if (tier1 == 1 && tier2 == 1) {
      return ArondightStrings.forms("form_header_costs_one_one", tier1, tier2);
    }
    if (tier1 == 1) {
      return ArondightStrings.forms("form_header_costs_one_many", tier1, tier2);
    }
    if (tier2 == 1) {
      return ArondightStrings.forms("form_header_costs_many_one", tier1, tier2);
    }
    return ArondightStrings.forms("form_header_costs_many_many", tier1, tier2);
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

  private static void addTile(CustomPanelAPI pane, float x, float y, ArondightFrame f,
      String formId, FleetMemberAPI member, String memberId, String currentFormId) {
    float tw = f.tileW;
    float th = f.tileH;
    float bandH = ArondightFrame.TILE_TOP + f.picture;
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

    float cell = f.shipCell;
    addShipRender(tile, (tw - cell) / 2f - SHIP_NUDGE_LEFT,
        ArondightFrame.TILE_TOP + f.picture / 2f - cell / 2f, cell, formId);

    float nameMax = tw + 2f * NAME_OVERHANG;
    float nameInk = Math.min(nameMax, ArondightUi.textWidth(tile, null, name));
    float nameW = Math.max(tw, (float) Math.ceil(nameInk) + 2f * ArondightUi.INSET
        + ArondightUi.LINE_SLACK);
    TooltipMakerAPI nameEl = tile.createUIElement(nameW, ArondightFrame.NAME_H, false);
    LabelAPI nameLbl = nameEl.addPara(name, 1f);
    nameLbl.setAlignment(Alignment.MID);
    nameLbl.setColor(isCurrent ? ArondightUi.good()
        : dimmed ? ArondightUi.gray() : ArondightUi.text());
    fitLabelToOneLine(nameLbl, name, nameMax);
    tile.addUIElement(nameEl).inTL((tw - nameW) / 2f, bandH - NAME_RAISE);

    if (dimmed) {
      CustomPanelAPI dim = tile.createCustomPanel(tw, bandH, new ArondightDimLayer());
      tile.addComponent(dim).inTL(0f, 0f);
    }

    TooltipMakerAPI overlay = tile.createUIElement(tw, th, false);
    ButtonAPI hit = ArondightUi.addInvisibleButton(overlay,
        verbData != null ? verbData : "arondight_btn_noop_" + formId, tw, th);
    ArondightUi.setEnabledSayingWhy(hit, actionable);
    FleetMemberAPI tipMember = isCurrent && member != null ? member
        : tooltipMember(formId, memberId, member);
    boolean attached = false;
    if (tipMember != null) {
      attached = ArondightShipTooltips.attachExpanded(hit, tipMember,
          commanderStats(member), tipNote(formId));
    }
    if (!attached) {
      overlay.addTooltipToPrevious(fallbackTip(formId, tipMember),
          TooltipMakerAPI.TooltipLocation.BELOW, false);
    }
    tile.addUIElement(overlay).inTL(-ArondightUi.INSET, 0f);
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

  private static FleetMemberAPI tooltipMember(String formId, String memberId,
      FleetMemberAPI live) {
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
      crewPreview(m, live);
    } catch (Throwable ignore) {
    }
    return m;
  }

  private static void crewPreview(FleetMemberAPI m, FleetMemberAPI live) {
    try {
      m.getCrewComposition().setCrew(Math.max(1f, m.getMinCrew()));
      float cr = live != null ? live.getRepairTracker().getCR()
          : m.getRepairTracker().getMaxCR();
      m.getRepairTracker().setCR(cr);
      m.updateStats();
    } catch (Throwable ignore) {
    }
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
    CustomPanelAPI box = tile.createCustomPanel(cell, cell, null);
    FleetMemberAPI m = displayMember(formId);
    if (m != null) {
      java.util.List<FleetMemberAPI> one = new java.util.ArrayList<FleetMemberAPI>(1);
      one.add(m);
      TooltipMakerAPI ship = box.createUIElement(cell, cell, false);
      ship.addShipList(1, 1, cell, ArondightUi.alpha(ArondightUi.dark(), 0), one, 0f);
      silenceShipCell(ship);
      box.addUIElement(ship).inTL(0f, 0f);
    } else {
      TooltipMakerAPI ph = box.createUIElement(cell, cell, false);
      LabelAPI l = ph.addPara("[ hull", ArondightUi.gray(), Math.max(0f, cell / 2f - 18f));
      l.setAlignment(Alignment.MID);
      LabelAPI l2 = ph.addPara("pending ]", ArondightUi.gray(), 1f);
      l2.setAlignment(Alignment.MID);
      box.addUIElement(ph).inTL(0f, 0f);
    }
    tile.addComponent(box).inTL(x, y);
  }

  private static void silenceShipCell(TooltipMakerAPI ship) {
    try {
      Object list = ArondightReflect.invoke(ship.getPrev(), "getList");
      Object items = ArondightReflect.invoke(list, "getItems");
      for (Object item : (Iterable<?>) items) {
        ArondightReflect.invoke(item, "setTooltip", 0f, null);
      }
    } catch (Throwable t) {
      ArondightLog.warnOnce("form_tile_cell_tooltip", "the form tiles' ship cells kept"
          + " the engine's own tooltip (" + t + "), likely a rename in a game update; a"
          + " tile's ship picture may open a plain ship tooltip just outside its frame");
    }
  }

  private static ArondightUi.Tip fallbackTip(final String formId,
      final FleetMemberAPI tipMember) {
    final ArondightShipTooltips.TipNote note = tipNote(formId);
    return new ArondightUi.Tip() {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
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
