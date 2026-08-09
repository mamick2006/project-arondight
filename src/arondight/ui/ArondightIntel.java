package arondight.ui;

import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.weave.ArondightWeave;

public class ArondightIntel extends BaseIntelPlugin {

  public static final String TAG = "Arondight";

  private static final float FORM_SHARE = 0.45f;
  private static final float GUTTER = 10f;
  private static final float BAND_H = 54f;
  private static final float RESPEC_W = 90f;

  public static ArondightIntel get() {
    if (Global.getSector() == null) {
      return null;
    }
    return (ArondightIntel) Global.getSector().getIntelManager()
        .getFirstIntel(ArondightIntel.class);
  }

  public static void ensureExists() {
    if (Global.getSector() == null || !ArondightState.exists()) {
      return;
    }
    IntelManagerAPI im = Global.getSector().getIntelManager();
    if (get() == null) {
      im.addIntel(new ArondightIntel(), true);
    }
  }

  @Override
  public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
    info.addPara(getName(), getTitleColor(mode), 0f);
    String memberId = resolveMemberId();
    if (memberId == null) {
      return;
    }
    String form = ArondightFormPane.formName(ArondightState.getCurrentForm(memberId));
    int woven = ArondightState.getWovenCount(memberId);
    info.addPara("%s, %s woven", 3f, ArondightUi.hl(), form, "" + woven);
  }

  @Override
  public boolean hasLargeDescription() {
    return true;
  }

  @Override
  public boolean hasSmallDescription() {
    return false;
  }

  @Override
  public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
    if (!ArondightState.exists()) {
      TooltipMakerAPI e = panel.createUIElement(width, height, false);
      e.addPara(ArondightStrings.console("console_not_recovered"), ArondightUi.gray(), 0f);
      panel.addUIElement(e).inTL(0f, 0f);
      return;
    }
    try {
      FleetMemberAPI member = ArondightState.findPlayerFrame();
      String memberId = resolveMemberId(member);

      addStatusBand(panel, width, member, memberId);

      float paneH = height - BAND_H;
      float leftW = Math.round(width * FORM_SHARE) - GUTTER / 2f;
      float rightW = width - leftW - GUTTER;
      CustomPanelAPI panes = panel.createCustomPanel(width, paneH, ArondightUi.consoleRow());
      panel.addComponent(panes).inTL(0f, BAND_H);
      ArondightFormPane.add(panes, 0f, leftW, paneH, member, memberId);
      ArondightWeavePane.add(panes, leftW + GUTTER, rightW, paneH, member, memberId);
    } catch (Throwable t) {
      buildFailWarn(t);
      TooltipMakerAPI e = panel.createUIElement(width, height, false);
      e.addPara("The Arondight console failed to build.", ArondightUi.gray(), 0f);
      panel.addUIElement(e).inTL(0f, 0f);
    }
  }

  private void addStatusBand(CustomPanelAPI panel, float width, FleetMemberAPI member,
      String memberId) {
    float reserved = RESPEC_W + 18f;
    TooltipMakerAPI band = panel.createUIElement(width - 20f - reserved, BAND_H, false);
    String form = memberId == null ? ArondightStrings.console("console_form_none")
        : ArondightFormPane.formName(ArondightState.getCurrentForm(memberId));
    String where = member != null ? ArondightStrings.console("console_where_with")
        : ArondightStrings.console("console_where_without");
    LabelAPI l1 = band.addPara(ArondightStrings.console("console_frame_band", form, where),
        ArondightUi.text(), 2f);
    l1.setHighlight(form);
    l1.setHighlightColor(ArondightUi.accent());
    int sp = Global.getSector().getPlayerStats().getStoryPoints();
    LabelAPI l2 = band.addPara(ArondightStrings.console("console_sp_available", sp),
        ArondightUi.text(), 2f);
    l2.setHighlight("" + sp);
    l2.setHighlightColor(ArondightUi.story());
    if (!ArondightState.areTreesUnlocked()) {
      band.addPara(ArondightStrings.console("console_sealed_band"),
          ArondightUi.warn(), 2f);
    }
    panel.addUIElement(band).inTL(10f, 2f);
    addRespecControl(panel, width, member);
  }

  private void addRespecControl(CustomPanelAPI panel, float width, FleetMemberAPI member) {
    CustomPanelAPI holder = panel.createCustomPanel(RESPEC_W, BAND_H - 6f,
        ArondightUi.consoleRow());
    TooltipMakerAPI el = holder.createUIElement(RESPEC_W, BAND_H - 6f, false);
    ButtonAPI b = el.addButton("Respec", ArondightWeavePane.BTN_RESPEC, ArondightUi.base(),
        ArondightUi.dark(), Alignment.MID, CutStyle.ALL, RESPEC_W, 20f, 5f);
    ArondightUi.setEnabledSayingWhy(b, ArondightWeave.getRespecBlockReason(member) == null);
    el.addTooltipToPrevious(ArondightWeavePane.respecTip(),
        TooltipMakerAPI.TooltipLocation.BELOW, false);
    holder.addUIElement(el).inTL(0f, 0f);
    panel.addComponent(holder).inTL(width - 10f - RESPEC_W, 4f);
  }

  private static volatile boolean buildFailWarned;

  private static void buildFailWarn(Throwable t) {
    if (buildFailWarned) {
      return;
    }
    buildFailWarned = true;
    Global.getLogger(ArondightIntel.class).warn("Project Arondight: the Arondight console failed"
        + " to build (likely an API mismatch or bad settings data). The pane shows a notice"
        + " instead; the tab and the intel entry stay usable after the cause is fixed.", t);
  }

  private static String resolveMemberId() {
    return resolveMemberId(ArondightState.findPlayerFrame());
  }

  private static String resolveMemberId(FleetMemberAPI member) {
    if (member != null) {
      return member.getId();
    }
    List<String> ids = ArondightState.getFrameMemberIds();
    return ids.isEmpty() ? null : ids.get(0);
  }

  public boolean handleButton(Object buttonId, IntelUIAPI ui) {
    if (!(buttonId instanceof String) || !ArondightState.exists()) {
      return false;
    }
    String id = (String) buttonId;
    FleetMemberAPI member = ArondightState.findPlayerFrame();

    if (id.startsWith(ArondightFormPane.BTN_TRANSFORM_PREFIX)) {
      runTransform(id.substring(ArondightFormPane.BTN_TRANSFORM_PREFIX.length()), member);
      return true;
    }
    if (id.startsWith(ArondightWeavePane.BTN_NODE_PREFIX)) {
      if (member != null) {
        ArondightWeave.weaveNode(member,
            id.substring(ArondightWeavePane.BTN_NODE_PREFIX.length()), null);
      }
      return true;
    }
    if (ArondightWeavePane.BTN_RESPEC.equals(id)) {
      if (member != null) {
        ArondightWeave.respec(member, null);
      }
      return true;
    }
    return false;
  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    if (handleButton(buttonId, ui) && ui != null) {
      ui.updateUIForItem(this);
    }
  }

  private void runTransform(String formId, FleetMemberAPI member) {
    if (member == null) {
      return;
    }
    ArondightTransformer.TransformResult r = ArondightTransformer.transform(member, formId);
    if (!r.success || r.weaponsMovedToCargo.isEmpty()) {
      return;
    }
    StringBuilder names = new StringBuilder();
    for (int i = 0; i < r.weaponsMovedToCargo.size(); i++) {
      if (i > 0) {
        names.append(", ");
      }
      names.append(r.weaponsMovedToCargo.get(i));
    }
    try {
      Global.getSector().getCampaignUI().addMessage(
          ArondightStrings.console("console_moved_to_cargo", names));
    } catch (Exception ignored) {
    }
  }

  @Override
  public String getName() {
    return "Project Arondight";
  }

  @Override
  public String getSmallDescriptionTitle() {
    return getName();
  }

  @Override
  public String getIcon() {
    String own = ArondightUi.sprite("icons", "arondight_intel");
    if (own != null) {
      return own;
    }
    return Global.getSettings().getSpriteName("intel", "fleet_log");
  }

  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = super.getIntelTags(map);
    tags.add(TAG);
    return tags;
  }

  @Override
  public String getSortString() {
    return getName();
  }

  @Override
  public boolean shouldRemoveIntel() {
    return false;
  }

  @Override
  public IntelSortTier getSortTier() {
    return IntelSortTier.TIER_2;
  }

  @Override
  public boolean isHidden() {
    return arondight.topbar.ArondightMgmtInjector.isAlive();
  }
}
