package arondight.topbar;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.state.ArondightState;
import arondight.ui.ArondightConsolePanel;
import arondight.ui.ArondightMgmtScreen;

public class ArondightMgmtInjector implements EveryFrameScript {

  static final String HANDSHAKE_KEY = "$mgmt_bar";
  static final int CONTRACT_VERSION = 2;
  private static final String DK_HOST = "host";
  private static final String DK_VERSION = "version";
  private static final String DK_SEAM = "seam";

  private static final String MOD_ID = "arondight";
  private static final String SEAM_CLASS = "arondight.topbar.ArondightMgmtInjector";
  private static final String SHELL_CLASS = "arondight.ui.ArondightMgmtTabShell";
  private static final String BUTTON_LABEL = "Manage (Y)";
  private static final String BUTTON_LABEL_NO_KEY = "Manage";
  private static final Object BUTTON_TAB_ID = "arondight_mgmt_button";
  private static final String ATTENTION_KEY = "$arondight_mgmt_attention";
  private static final String SELECT_TAB_KEY = "$arondight_select_tab";

  private static final float BAR_BTN_W_MIN = 110f;
  private static final float BAR_BTN_W_MAX = 134f;
  private static final float BAR_BTN_H_MIN = 17f;
  private static final float BAR_BTN_H_MAX = 25f;
  private static final int MIN_BAR_BUTTONS = 5;
  private static final int SEARCH_DEPTH = 8;
  private static final int MAX_LOCATE_FAILS = 60;
  private static final float BUTTON_PAD = 6f;
  private static final float PLATE_PERIOD_W = 128f;
  private static final float PLATE_EDGE_W = 11f;
  private static final float PLATE_W = PLATE_PERIOD_W + PLATE_EDGE_W;
  private static final float PLATE_H = 35f;
  private static final float PLATE_SRC_W = 914f;
  private static final float PLATE_SRC_H = 35f;
  private static final float PLATE_SRC_PATTERN_X = PLATE_SRC_W - PLATE_W;
  private static final float PLATE_SRC_EDGE_X = PLATE_SRC_W - PLATE_EDGE_W;

  private static final Set<String> MGMT_SCOPE_BINDINGS = new HashSet<String>(Arrays.asList(
      "GENERAL_PAUSE", "GENERAL_SCREENSHOT", "GENERAL_ZOOM_IN", "GENERAL_ZOOM_OUT",
      "GENERAL_TOGGLE_UI", "GENERAL_EXPAND_TOOLTIP", "GENERAL_CODEX", "QUICK_SAVE", "QUICK_LOAD",
      "FAST_FORWARD", "GO_SLOW", "EXIT",
      "CORE_ABILITY_1", "CORE_ABILITY_2", "CORE_ABILITY_3", "CORE_ABILITY_4", "CORE_ABILITY_5",
      "CORE_ABILITY_6", "CORE_ABILITY_7", "CORE_ABILITY_8", "CORE_ABILITY_9", "CORE_ABILITY_10",
      "CORE_ABILITY_BAR_1", "CORE_ABILITY_BAR_2", "CORE_ABILITY_BAR_3", "CORE_ABILITY_BAR_4",
      "CORE_ABILITY_BAR_5",
      "CORE_CHARACTER", "CORE_FLEET", "CORE_REFIT", "CORE_CARGO", "CORE_INTEL", "CORE_OUTPOSTS",
      "CORE_OFFICERS", "CORE_MAP",
      "CORE_REPAIR", "CORE_CONFIRM", "CORE_RESET", "CORE_TAKEALL", "CORE_BUY_ACTION",
      "CORE_SELL_ACTION", "CORE_MOTHBALL_ACTION", "CORE_SCUTTLE_ACTION", "CORE_SUSPEND_ACTION",
      "CORE_LOGISTICS_ACTION", "CORE_MORE_INFO_ACTION", "CORE_REFIT_ACTION",
      "REFIT_ADD_MOD", "REFIT_MANAGE_VARIANTS", "REFIT_NEXT_UNSPENT", "REFIT_RESTORE",
      "REFIT_RUNSIM", "REFIT_SAVE", "REFIT_STRIP", "REFIT_UNDO", "REFIT_WEAPON_GROUPS",
      "SUBTAB_1", "SUBTAB_2", "SUBTAB_3", "SUBTAB_4",
      "TAB_0", "TAB_1", "TAB_2", "TAB_3", "TAB_4", "TAB_5", "TAB_6", "TAB_7", "TAB_8", "TAB_9",
      "TRADE_TAB_MARKET_INFO", "MV_SAVE", "MISSION_REFIT"));

  public static final class Tenant {
    public final String modId;
    public final String label;
    public final String factoryClass;
    public final String attentionKey;
    public final Color color;

    Tenant(String modId, String label, String factoryClass, String attentionKey, Color color) {
      this.modId = modId;
      this.label = label;
      this.factoryClass = factoryClass;
      this.attentionKey = attentionKey;
      this.color = color;
    }
  }

  private static final Map<String, Tenant> TENANTS = new LinkedHashMap<String, Tenant>();

  public static synchronized void registerTenant(String modId, String label, String factoryClass,
      String attentionKey, int r, int g, int b) {
    if (modId == null || label == null || factoryClass == null) {
      return;
    }
    if (!TENANTS.containsKey(modId)) {
      TENANTS.put(modId, new Tenant(modId, label, factoryClass, attentionKey, new Color(r, g, b, 255)));
    }
  }

  public static synchronized List<Tenant> tenants() {
    return new ArrayList<Tenant>(TENANTS.values());
  }

  public static synchronized List<String> presentTenantIds() {
    return new ArrayList<String>(TENANTS.keySet());
  }

  private static ArondightConsolePanel arondightConsole;

  public static CustomPanelAPI buildMgmtConsole(float width, float height) {
    releaseMgmtConsole();
    arondightConsole = new ArondightConsolePanel(width, height);
    return arondightConsole.getPanel();
  }

  public static void releaseMgmtConsole() {
    if (arondightConsole != null) {
      ArondightConsolePanel.dropLive(arondightConsole);
      arondightConsole = null;
    }
  }

  public static CustomPanelAPI buildTenantConsole(String modId, float width, float height)
      throws Exception {
    if (MOD_ID.equals(modId)) {
      return buildMgmtConsole(width, height);
    }
    Tenant t;
    synchronized (ArondightMgmtInjector.class) {
      t = TENANTS.get(modId);
    }
    if (t == null) {
      throw new IllegalStateException("arondight: no registered tenant " + modId);
    }
    Class<?> cls = Class.forName(t.factoryClass);
    Object m = ArondightCoreReflect.methodNamed(cls, "buildMgmtConsole", 2);
    if (m == null) {
      throw new NoSuchMethodException("arondight: no buildMgmtConsole(w, h) on " + t.factoryClass);
    }
    Object panel = ArondightCoreReflect.invokeMethod(m, null,
        new Object[] { Float.valueOf(width), Float.valueOf(height) });
    return (CustomPanelAPI) panel;
  }

  public static void releaseTenantConsole(String modId) {
    try {
      if (MOD_ID.equals(modId)) {
        releaseMgmtConsole();
        return;
      }
      Tenant t;
      synchronized (ArondightMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      if (t == null) {
        return;
      }
      Object m = ArondightCoreReflect.methodNamed(Class.forName(t.factoryClass),
          "releaseMgmtConsole", 0);
      if (m != null) {
        ArondightCoreReflect.invokeMethod(m, null, new Object[0]);
      }
    } catch (Throwable ignore) {
    }
  }

  public static boolean attentionActive(String modId) {
    try {
      Tenant t;
      synchronized (ArondightMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      if (t == null || t.attentionKey == null || Global.getSector() == null) {
        return false;
      }
      return Boolean.TRUE.equals(Global.getSector().getMemoryWithoutUpdate().get(t.attentionKey));
    } catch (Throwable ignore) {
      return false;
    }
  }

  public static void noteTabSelected(String modId) {
    try {
      Tenant t;
      synchronized (ArondightMgmtInjector.class) {
        t = TENANTS.get(modId);
      }
      MemoryAPI mem = Global.getSector() == null ? null : Global.getSector().getMemoryWithoutUpdate();
      if (mem == null) {
        return;
      }
      if (t != null && t.attentionKey != null) {
        mem.set(t.attentionKey, Boolean.FALSE);
      }
      if (MOD_ID.equals(modId) && ArondightState.exists()) {
        ArondightState.noteMgmtOpened();
      }
    } catch (Throwable ignore) {
    }
  }

  private static ArondightMgmtInjector live;
  private static volatile boolean forceFailForTest;
  private static volatile boolean forceNoMatchForTest;
  private static volatile Throwable lastErrorForTest;
  private static volatile String queuedSelectModId;
  public static volatile int mgmtDispatchCount;

  public static boolean isAlive() {
    ArondightMgmtInjector l = live;
    if (l == null || l.dead) {
      return false;
    }
    return l.hosting || l.hostSeamAlive;
  }

  public static boolean isInjected() {
    ArondightMgmtInjector l = live;
    if (l == null || l.dead || !l.hosting) {
      return false;
    }
    BarSlot s = l.activeSlotQuiet();
    return s != null && s.bar != null && s.button != null;
  }

  public static boolean isSelected() {
    return ArondightMgmtScreen.isOpen() && MOD_ID.equals(ArondightMgmtScreen.selectedTenant());
  }

  public static boolean isHost() {
    ArondightMgmtInjector l = live;
    return l != null && !l.dead && l.hosting;
  }

  public static boolean isAttached() {
    ArondightMgmtInjector l = live;
    return l != null && !l.dead && (l.hosting || l.registeredWithHost);
  }

  public static void requestSelect() {
    requestTabSelect(MOD_ID);
  }

  public static void requestTabSelect(String modId) {
    ArondightMgmtInjector l = live;
    if (l != null && !l.hosting && handshakePresentQuiet()) {
      try {
        Object desc = Global.getSector().getMemoryWithoutUpdate().get(HANDSHAKE_KEY);
        Object seam = ((Map<?, ?>) desc).get(DK_SEAM);
        Object m = ArondightCoreReflect.methodNamed(Class.forName((String) seam),
            "requestTabSelect", 1);
        if (m != null) {
          ArondightCoreReflect.invokeMethod(m, null, new Object[] { modId });
          return;
        }
      } catch (Throwable ignore) {
      }
    }
    queuedSelectModId = modId;
  }

  public static String consumeQueuedSelectTab() {
    String id = queuedSelectModId;
    queuedSelectModId = null;
    return id;
  }

  public static void injectReflectionFailureForTest() {
    forceFailForTest = true;
  }

  public static void injectNoMatchForTest(boolean on) {
    forceNoMatchForTest = on;
  }

  public static String lastErrorForTest() {
    Throwable t = lastErrorForTest;
    if (t == null) {
      return "none";
    }
    StackTraceElement[] st = t.getStackTrace();
    return t + (st.length > 0 ? " @ " + st[0] : "");
  }

  public static float[] plateExtRect() {
    ArondightMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    if (s == null || s.plate == null || s.plate.getPosition() == null) {
      return null;
    }
    PositionAPI p = s.plate.getPosition();
    return new float[] { p.getX(), p.getY(), p.getWidth(), p.getHeight() };
  }

  public static float[] buttonRect() {
    ArondightMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    if (s == null || s.button == null || s.button.getPosition() == null) {
      return null;
    }
    PositionAPI p = s.button.getPosition();
    return new float[] { p.getX(), p.getY(), p.getWidth(), p.getHeight() };
  }

  public static boolean isButtonFlashing() {
    ArondightMgmtInjector l = live;
    BarSlot s = l == null ? null : l.activeSlotQuiet();
    return s != null && s.flashOn;
  }

  public static boolean openManagementScreen() {
    mgmtDispatchCount++;
    ArondightMgmtInjector l = live;
    if (l != null && !l.hosting && handshakePresentQuiet()) {
      try {
        Object desc = Global.getSector().getMemoryWithoutUpdate().get(HANDSHAKE_KEY);
        Object seam = ((Map<?, ?>) desc).get(DK_SEAM);
        Object m = ArondightCoreReflect.methodNamed(Class.forName((String) seam),
            "openManagementScreen", 0);
        if (m != null) {
          return Boolean.TRUE.equals(ArondightCoreReflect.invokeMethod(m, null, new Object[0]));
        }
      } catch (Throwable ignore) {
        return false;
      }
      return false;
    }
    return openOwnScreen();
  }

  private static boolean openOwnScreen() {
    ArondightMgmtInjector l = live;
    if (l == null || l.dead) {
      return false;
    }
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      if (ui == null) {
        return false;
      }
      boolean docked = ui.getCurrentInteractionDialog() != null;
      UIPanelAPI activeL = docked ? l.dialogCore() : ArondightCoreReflect.getCore();
      if (activeL == null) {
        return false;
      }
      BarSlot slot = docked ? l.dockedSlot : l.campaignSlot;
      if (slot.button == null || slot.hostL != activeL) {
        return false;
      }
      Object shell = ArondightMgmtScreen.liveShell();
      if (shell != null && ArondightCoreReflect.getCurrentTab(activeL) == shell) {
        return true;
      }
      if (ui.isShowingMenu()) {
        return false;
      }
      if (!docked && ui.isShowingDialog() && ui.getCurrentCoreTab() == null
          && ArondightMgmtScreen.liveShell() == null) {
        return false;
      }
      if (tenants().isEmpty()) {
        return false;
      }
      float w = ((Number) ArondightCoreReflect.invokeOneArg(
          "computePanelWidth", activeL, Boolean.TRUE)).floatValue();
      float h = ((Number) ArondightCoreReflect.invokeOneArg(
          "computePanelHeight", activeL, Boolean.TRUE)).floatValue();
      float capW = 1600f;
      float capH = 980f;
      try {
        capW = (float) MethodHandles.lookup()
            .findStaticGetter(activeL.getClass(), "MAX_INTEL_WIDTH", float.class).invoke();
        capH = (float) MethodHandles.lookup()
            .findStaticGetter(activeL.getClass(), "MAX_INTEL_HEIGHT", float.class).invoke();
      } catch (Throwable ignore) {
        capW = 1600f;
        capH = 980f;
      }
      w = Math.min(w, capW);
      h = Math.min(h, capH);
      Class<?> shellCls = Class.forName(SHELL_CLASS);
      Object newShell = MethodHandles.lookup()
          .findConstructor(shellCls, MethodType.methodType(void.class, float.class, float.class))
          .invoke(w, h);
      Object m = ArondightCoreReflect.methodNamed(activeL.getClass(), "showPanelAsDialog", 4);
      if (m == null) {
        throw new NoSuchMethodException("arondight: no showPanelAsDialog(button, panel, bl, bl) on"
            + " the active core UI");
      }
      ArondightCoreReflect.invokeMethod(m, activeL,
          new Object[] { slot.button, newShell, Boolean.TRUE, Boolean.FALSE });
      if (ArondightCoreReflect.getCurrentTab(activeL) == newShell) {
        ArondightMgmtScreen.promoteStaged(newShell, activeL);
        return true;
      }
      ArondightMgmtScreen.discardStaged(newShell);
      return false;
    } catch (Throwable t) {
      l.fail(t);
      return false;
    }
  }

  public static boolean dismissCore(Object coreL) {
    if (coreL == null) {
      return false;
    }
    try {
      Object m = ArondightCoreReflect.methodNamed(coreL.getClass(), "dismiss", 1);
      if (m == null) {
        return false;
      }
      ArondightCoreReflect.invokeMethod(m, coreL, new Object[] { Integer.valueOf(0) });
      return true;
    } catch (Throwable ignore) {
      return false;
    }
  }

  public static void onBarButtonClick(Object button, Object event) {
    try {
      ArondightMgmtInjector l = live;
      if (l != null) {
        if (l.campaignSlot.button != null) {
          l.campaignSlot.button.setChecked(false);
        }
        if (l.dockedSlot.button != null) {
          l.dockedSlot.button.setChecked(false);
        }
        if (!l.dead && l.hosting) {
          UIPanelAPI activeL = l.resolveActiveLQuiet();
          Object shell = ArondightMgmtScreen.liveShell();
          if (activeL != null && shell != null
              && ArondightCoreReflect.getCurrentTab(activeL) == shell) {
            mgmtDispatchCount++;
            dismissCore(activeL);
            return;
          }
        }
      }
      openManagementScreen();
    } catch (Throwable ignore) {
    }
  }

  public static void resetStaticsForLoad() {
    ArondightMgmtInjector l = live;
    if (l != null) {
      hideSlot(l.campaignSlot);
      hideSlot(l.dockedSlot);
    }
    synchronized (ArondightMgmtInjector.class) {
      TENANTS.clear();
    }
    releaseMgmtConsole();
    queuedSelectModId = null;
    mgmtDispatchCount = 0;
    live = null;
  }

  private static final class BarSlot {
    UIPanelAPI hostL;
    UIPanelAPI bar;
    ButtonAPI button;
    CustomPanelAPI plate;
    boolean proxyFailed;
    boolean flashOn;
  }

  private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
  private boolean dead;

  private final BarSlot campaignSlot = new BarSlot();
  private final BarSlot dockedSlot = new BarSlot();
  private boolean hosting;
  private boolean registeredWithHost;
  private boolean hostSeamAlive;
  private int locateFails;

  public ArondightMgmtInjector() {
    live = this;
    queuedSelectModId = null;
    if (Global.getSector() != null) {
      Global.getSector().getMemoryWithoutUpdate().unset(SELECT_TAB_KEY);
      Global.getSector().getMemoryWithoutUpdate().unset(HANDSHAKE_KEY);
    }
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean runWhilePaused() {
    return true;
  }

  @Override
  public void advance(float amount) {
    if (dead) {
      return;
    }
    pollDegradedButton(campaignSlot);
    pollDegradedButton(dockedSlot);
    if (dead) {
      return;
    }
    interval.advance(amount);
    if (!interval.intervalElapsed()) {
      return;
    }
    try {
      tick();
    } catch (Throwable t) {
      fail(t);
    }
  }

  private void pollDegradedButton(BarSlot slot) {
    if (!slot.proxyFailed || slot.button == null) {
      return;
    }
    try {
      if (slot.button.isChecked()) {
        slot.button.setChecked(false);
        openManagementScreen();
      }
    } catch (Throwable t) {
      fail(t);
    }
  }

  private void tick() throws Exception {
    if (forceFailForTest) {
      forceFailForTest = false;
      throw new RuntimeException("arondight: injected management-bar reflection failure (test hook)");
    }
    if (!ArondightState.exists()) {
      locateFails = 0;
      return;
    }
    maintainOwnAttentionKey();

    MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
    Object desc = mem.get(HANDSHAKE_KEY);
    if (desc instanceof Map && !MOD_ID.equals(((Map<?, ?>) desc).get(DK_HOST))) {
      arriveTick((Map<?, ?>) desc);
      return;
    }
    hostTick(mem);
  }

  private void hostTick(MemoryAPI mem) throws Exception {
    boolean docked = Global.getSector().getCampaignUI() != null
        && Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null;
    UIPanelAPI activeL = docked ? dialogCore() : ArondightCoreReflect.getCore();
    if (activeL == null) {
      locateFails = 0;
      return;
    }
    registerTenant(MOD_ID, "Arondight", SEAM_CLASS, ATTENTION_KEY, 130, 205, 180);

    BarSlot slot = docked ? dockedSlot : campaignSlot;
    if (slot.hostL != null && slot.hostL != activeL) {
      clearSlot(slot);
    }
    boolean present = slot.bar != null && slot.button != null && slot.hostL == activeL
        && ArondightCoreReflect.isChild(slot.bar, slot.button);
    if (!present) {
      clearSlot(slot);
      UIPanelAPI found = locateBar(activeL);
      if (found == null) {
        giveUpIfStuck();
        return;
      }
      inject(slot, activeL, found);
      if (slot.button == null) {
        giveUpIfStuck();
        return;
      }
      hosting = true;
      publishHandshake(mem);
    }
    locateFails = 0;

    if (mem.get(SELECT_TAB_KEY) != null) {
      mem.unset(SELECT_TAB_KEY);
      requestSelect();
    }
    updateButtonFlash(slot);
  }

  private void arriveTick(Map<?, ?> desc) {
    hosting = false;
    Object v = desc.get(DK_VERSION);
    int version = v instanceof Integer ? ((Integer) v).intValue() : -1;
    if (version > CONTRACT_VERSION) {
      if (!declinedLogged) {
        declinedLogged = true;
        Global.getLogger(ArondightMgmtInjector.class).warn("Project Arondight: the management-bar"
            + " host publishes contract version " + version + " (this mod knows " + CONTRACT_VERSION
            + "); not attaching. The standalone Project Arondight intel entry covers management.");
      }
      hostSeamAlive = false;
      return;
    }
    Object seam = desc.get(DK_SEAM);
    if (!(seam instanceof String)) {
      hostSeamAlive = false;
      giveUpIfStuck();
      return;
    }
    try {
      Class<?> seamCls = Class.forName((String) seam);
      Object aliveM = ArondightCoreReflect.methodNamed(seamCls, "isAlive", 0);
      hostSeamAlive = aliveM != null
          && Boolean.TRUE.equals(ArondightCoreReflect.invokeMethod(aliveM, null, new Object[0]));
      if (!registeredWithHost) {
        Object regM = ArondightCoreReflect.methodNamed(seamCls, "registerTenant", 7);
        if (regM == null) {
          throw new NoSuchMethodException("arondight: no registerTenant(..) on " + seam);
        }
        ArondightCoreReflect.invokeMethod(regM, null, new Object[] { MOD_ID, "Arondight",
            SEAM_CLASS, ATTENTION_KEY, Integer.valueOf(130), Integer.valueOf(205),
            Integer.valueOf(180) });
        registeredWithHost = true;
      }
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      if (mem.get(SELECT_TAB_KEY) != null) {
        mem.unset(SELECT_TAB_KEY);
        requestSelect();
      }
      locateFails = 0;
    } catch (Throwable t) {
      hostSeamAlive = false;
      giveUpIfStuck();
    }
  }

  private transient boolean declinedLogged;

  private UIPanelAPI dialogCore() throws Exception {
    CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
    if (ui == null || ui.getCurrentInteractionDialog() == null) {
      return null;
    }
    Object m = ArondightCoreReflect.methodNamed(ui.getClass(), "getEncounterDialog", 0);
    Object dlg = m == null ? null : ArondightCoreReflect.invokeMethod(m, ui, new Object[0]);
    if (dlg == null) {
      return null;
    }
    Object m2 = ArondightCoreReflect.methodNamed(dlg.getClass(), "getCoreUI", 0);
    Object core = m2 == null ? null : ArondightCoreReflect.invokeMethod(m2, dlg, new Object[0]);
    return core instanceof UIPanelAPI ? (UIPanelAPI) core : null;
  }

  private UIPanelAPI resolveActiveLQuiet() {
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      if (ui == null) {
        return null;
      }
      return ui.getCurrentInteractionDialog() != null ? dialogCore() : ArondightCoreReflect.getCore();
    } catch (Throwable t) {
      return null;
    }
  }

  private boolean activeContextLive() {
    return resolveActiveLQuiet() != null;
  }

  private BarSlot activeSlotQuiet() {
    try {
      CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
      if (ui == null) {
        return null;
      }
      return ui.getCurrentInteractionDialog() != null ? dockedSlot : campaignSlot;
    } catch (Throwable t) {
      return null;
    }
  }

  private UIPanelAPI locateBar(UIPanelAPI root) throws Exception {
    if (forceNoMatchForTest) {
      return null;
    }
    if (root == null) {
      return null;
    }
    return searchBar(root, SEARCH_DEPTH);
  }

  private UIPanelAPI searchBar(UIPanelAPI panel, int depth) throws Exception {
    if (panel == null || depth < 0) {
      return null;
    }
    if (looksLikeBar(panel)
        && ArondightCoreReflect.methodNamed(panel.getClass(), "addButton", 4) != null) {
      return panel;
    }
    for (Object child : ArondightCoreReflect.childrenList(panel)) {
      if (child instanceof UIPanelAPI) {
        UIPanelAPI found = searchBar((UIPanelAPI) child, depth - 1);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private boolean looksLikeBar(UIPanelAPI p) throws Exception {
    int n = 0;
    for (Object child : ArondightCoreReflect.childrenList(p)) {
      if (!(child instanceof ButtonAPI) || !(child instanceof UIComponentAPI)) {
        continue;
      }
      PositionAPI pos = ((UIComponentAPI) child).getPosition();
      if (pos == null) {
        continue;
      }
      float w = pos.getWidth();
      float h = pos.getHeight();
      if (w >= BAR_BTN_W_MIN && w <= BAR_BTN_W_MAX && h >= BAR_BTN_H_MIN && h <= BAR_BTN_H_MAX) {
        n++;
      }
    }
    return n >= MIN_BAR_BUTTONS;
  }

  private static float rightmostButtonRightLocal(UIPanelAPI bar) throws Exception {
    PositionAPI barPos = bar.getPosition();
    float best = -1f;
    for (Object child : ArondightCoreReflect.childrenList(bar)) {
      if (!(child instanceof ButtonAPI) || !(child instanceof UIComponentAPI)) {
        continue;
      }
      PositionAPI pos = ((UIComponentAPI) child).getPosition();
      if (pos == null) {
        continue;
      }
      float w = pos.getWidth();
      float h = pos.getHeight();
      if (w >= BAR_BTN_W_MIN && w <= BAR_BTN_W_MAX && h >= BAR_BTN_H_MIN && h <= BAR_BTN_H_MAX) {
        best = Math.max(best, pos.getX() + w);
      }
    }
    if (best < 0f || barPos == null) {
      throw new IllegalStateException("arondight: the bar lost its buttons between match and inject");
    }
    return best - barPos.getX();
  }

  public static volatile int injectCount;

  private void inject(BarSlot slot, UIPanelAPI activeL, UIPanelAPI foundBar) throws Exception {
    injectCount++;
    float prevRight = rightmostButtonRightLocal(foundBar);

    slot.plate = Global.getSettings().createCustom(PLATE_W, PLATE_H, new PlatePlugin());
    foundBar.addComponent(slot.plate).inBL(prevRight, 0f);

    Object m = ArondightCoreReflect.methodNamed(foundBar.getClass(), "addButton", 4);
    if (m == null) {
      throw new NoSuchMethodException("arondight: bar lost its addButton shape between match and"
          + " inject");
    }
    boolean keyCollides = mgmtScopeKeyInUse(m, Keyboard.KEY_Y);
    String label = keyCollides ? BUTTON_LABEL_NO_KEY : BUTTON_LABEL;
    Class<?>[] pts = ArondightCoreReflect.paramTypes(m);
    Object listener;
    try {
      listener = ArondightCoreReflect.samProxy(pts[3], MethodHandles.lookup().findStatic(
          ArondightMgmtInjector.class, "onBarButtonClick",
          MethodType.methodType(void.class, Object.class, Object.class)));
      slot.proxyFailed = false;
    } catch (Throwable t) {
      listener = null;
      slot.proxyFailed = true;
    }
    ArondightCoreReflect.invokeMethod(m, foundBar,
        new Object[] { label, BUTTON_TAB_ID, null, listener });
    Object btn = ArondightCoreReflect.invokeOneArg("getButton", foundBar, BUTTON_TAB_ID);
    if (!(btn instanceof ButtonAPI)) {
      throw new IllegalStateException("arondight: the bar's addButton did not register the Manage"
          + " button under its tab id");
    }
    slot.button = (ButtonAPI) btn;
    slot.bar = foundBar;
    slot.hostL = activeL;

    if (keyCollides) {
      Global.getLogger(ArondightMgmtInjector.class).info("Project Arondight: the player keymap"
          + " binds Y to a campaign- or core-screen-scope control, so the Manage button ships"
          + " without its shortcut this session; the button stays fully clickable.");
    } else {
      slot.button.setShortcut(Keyboard.KEY_Y, false);
    }

    if (slot.button.getPosition() != null) {
      slot.button.getPosition().inBL(prevRight + BUTTON_PAD, 4f);
    }
  }

  private static boolean mgmtScopeKeyInUse(Object addButtonMethod, int keyCode) {
    try {
      Class<?>[] pts = ArondightCoreReflect.paramTypes(addButtonMethod);
      Object[] constants = pts[2].getEnumConstants();
      if (constants == null) {
        return false;
      }
      for (Object c : constants) {
        if (!(c instanceof Enum) || !MGMT_SCOPE_BINDINGS.contains(((Enum<?>) c).name())) {
          continue;
        }
        for (Integer v : ArondightCoreReflect.instanceIntFieldValues(c)) {
          if (v != null && v.intValue() == keyCode) {
            return true;
          }
        }
      }
      return false;
    } catch (Throwable t) {
      return false;
    }
  }

  private static final class PlatePlugin extends BaseCustomUIPanelPlugin {
    private PositionAPI pos;
    private SpriteAPI sprite;
    private boolean spriteUnusable;

    @Override
    public void positionChanged(PositionAPI position) {
      pos = position;
    }

    @Override
    public void render(float alphaMult) {
      if (pos == null || spriteUnusable) {
        return;
      }
      if (sprite == null) {
        try {
          sprite = Global.getSettings().getSprite("ui", "tripad_button_holder");
        } catch (Throwable t) {
          spriteUnusable = true;
          return;
        }
        if (sprite == null) {
          spriteUnusable = true;
          return;
        }
      }
      if (Math.round(sprite.getWidth()) != (int) PLATE_SRC_W
          || Math.round(sprite.getHeight()) != (int) PLATE_SRC_H) {
        return;
      }
      sprite.setAlphaMult(alphaMult);
      float x = pos.getX();
      float y = pos.getY();
      sprite.renderRegion(x - PLATE_SRC_PATTERN_X, y,
          PLATE_SRC_PATTERN_X / PLATE_SRC_W, 0f, PLATE_PERIOD_W / PLATE_SRC_W, 1f);
      sprite.renderRegion(x + PLATE_PERIOD_W - PLATE_SRC_EDGE_X, y,
          PLATE_SRC_EDGE_X / PLATE_SRC_W, 0f, PLATE_EDGE_W / PLATE_SRC_W, 1f);
    }
  }

  private void maintainOwnAttentionKey() {
    try {
      MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
      boolean opened = ArondightState.isMgmtOpened();
      Object key = mem.get(ATTENTION_KEY);
      if (key == null) {
        mem.set(ATTENTION_KEY, Boolean.valueOf(!opened));
      } else if (Boolean.FALSE.equals(key) && !opened) {
        ArondightState.noteMgmtOpened();
      } else if (Boolean.TRUE.equals(key) && opened) {
        mem.set(ATTENTION_KEY, Boolean.FALSE);
      }
    } catch (Throwable ignore) {
    }
  }

  private void updateButtonFlash(BarSlot slot) {
    if (slot.button == null) {
      return;
    }
    boolean want = false;
    for (Tenant t : tenants()) {
      if (t.attentionKey != null && attentionActive(t.modId)) {
        want = true;
        break;
      }
    }
    try {
      if (want && !slot.flashOn) {
        slot.button.flash(false, 0.25f, 0.25f);
        Object fader = ArondightCoreReflect.invokeNoArg("getFlashFader", slot.button);
        if (fader != null) {
          ArondightCoreReflect.invokeOneArg("setBounceUp", fader, Boolean.TRUE);
        }
        slot.flashOn = true;
      } else if (!want && slot.flashOn) {
        Object fader = ArondightCoreReflect.invokeNoArg("getFlashFader", slot.button);
        if (fader != null) {
          ArondightCoreReflect.invokeOneArg("setBounceUp", fader, Boolean.FALSE);
        }
        slot.flashOn = false;
      }
    } catch (Throwable ignore) {
      slot.flashOn = want;
    }
  }

  private void clearSlot(BarSlot slot) {
    slot.hostL = null;
    slot.bar = null;
    slot.button = null;
    slot.plate = null;
    slot.proxyFailed = false;
    slot.flashOn = false;
  }

  private void giveUpIfStuck() {
    try {
      if (hostingRoleActive() ? !activeContextLive() : !handshakePresent()) {
        return;
      }
    } catch (Throwable t) {
      return;
    }
    if (++locateFails > MAX_LOCATE_FAILS) {
      String context;
      try {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        context = "tab " + ui.getCurrentCoreTab() + ", docked "
            + (ui.getCurrentInteractionDialog() != null) + ", role "
            + (hostingRoleActive() ? "host" : "arriver");
      } catch (Throwable t) {
        context = "context unavailable";
      }
      Global.getLogger(ArondightMgmtInjector.class).warn("Project Arondight: could not attach the"
          + " shared Manage button after " + MAX_LOCATE_FAILS + " attempts (" + context + ");"
          + " giving up for this session. Likely cause: another mod restructured the campaign"
          + " bottom bar, or a game update changed it. The standalone Project Arondight intel"
          + " entry appears in the intel list instead, so nothing is lost.");
      lastErrorForTest = new RuntimeException("arondight: management bar never attached after "
          + MAX_LOCATE_FAILS + " in-context intervals; self-disabling so the fallback shows");
      disableForSession();
    }
  }

  private boolean hostingRoleActive() {
    if (hosting) {
      return true;
    }
    return !handshakePresentQuiet();
  }

  private static boolean handshakePresent() {
    MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
    Object desc = mem.get(HANDSHAKE_KEY);
    return desc instanceof Map && !MOD_ID.equals(((Map<?, ?>) desc).get(DK_HOST));
  }

  private static boolean handshakePresentQuiet() {
    try {
      return handshakePresent();
    } catch (Throwable t) {
      return false;
    }
  }

  private void publishHandshake(MemoryAPI mem) {
    HashMap<String, Object> desc = new HashMap<String, Object>();
    desc.put(DK_HOST, MOD_ID);
    desc.put(DK_VERSION, Integer.valueOf(CONTRACT_VERSION));
    desc.put(DK_SEAM, SEAM_CLASS);
    mem.set(HANDSHAKE_KEY, desc);
  }

  private void fail(Throwable t) {
    lastErrorForTest = t;
    Global.getLogger(ArondightMgmtInjector.class).warn("Project Arondight: the management-bar"
        + " injector hit a reflection error (" + t + ") and disabled itself for the session."
        + " Likely cause: a game version change. The standalone Project Arondight intel entry"
        + " appears in the intel list instead, so nothing is lost.");
    disableForSession();
  }

  private void disableForSession() {
    dead = true;
    try {
      arondight.ui.ArondightIntel.ensureExists();
    } catch (Throwable ignore) {
    }
    hideSlot(campaignSlot);
    hideSlot(dockedSlot);
    clearSlot(campaignSlot);
    clearSlot(dockedSlot);
  }

  private static void hideSlot(BarSlot slot) {
    try {
      if (slot.button != null) {
        slot.button.setOpacity(0f);
        if (slot.button.getPosition() != null) {
          slot.button.getPosition().setSize(0f, slot.button.getPosition().getHeight());
        }
      }
    } catch (Throwable ignore) {
    }
    try {
      if (slot.plate != null) {
        slot.plate.setOpacity(0f);
      }
    } catch (Throwable ignore) {
    }
  }
}
