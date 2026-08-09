package arondight.ui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.misc.MapMarkerIntel;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.state.ArondightState;

public final class ArondightConsolePanel implements CustomUIPanelPlugin {

  private static final float CONTENT_PAD = 10f;

  private static ArondightConsolePanel live;

  private final CustomPanelAPI root;
  private final float width;
  private final float height;
  private CustomPanelAPI content;
  private PositionAPI pos;

  public ArondightConsolePanel(float width, float height) {
    this.width = width;
    this.height = height;
    this.root = Global.getSettings().createCustom(width, height, this);
    live = this;
    rebuild();
  }

  public static void routeButton(Object buttonId) {
    if (live != null) {
      live.handleButton(buttonId);
    }
  }

  public static boolean hasLive() {
    return live != null;
  }

  public static void dropLive(ArondightConsolePanel console) {
    if (console != null && live == console) {
      live = null;
      ArondightWeavePane.dropLiveScroller();
    }
  }

  public static void resetStaticsForLoad() {
    live = null;
    ArondightFormPane.resetStaticsForLoad();
    ArondightWeavePane.dropLiveScroller();
  }

  public CustomPanelAPI getPanel() {
    return root;
  }

  private void rebuild() {
    if (content != null) {
      try {
        root.removeComponent(content);
      } catch (Throwable ignore) {
      }
    }
    float cw = width - 2f * CONTENT_PAD;
    float ch = height - 2f * CONTENT_PAD;
    content = root.createCustomPanel(cw, ch, ArondightUi.consoleRow());
    root.addComponent(content).inTL(CONTENT_PAD, CONTENT_PAD);
    ArondightIntel.ensureExists();
    ArondightIntel intel = ArondightIntel.get();
    if (intel != null) {
      try {
        intel.createLargeDescription(content, cw, ch);
      } catch (Throwable ignore) {
      }
    } else {
      TooltipMakerAPI e = content.createUIElement(cw, ch, false);
      e.addPara(arondight.ArondightStrings.console("console_not_recovered"),
          ArondightUi.gray(), 0f);
      content.addUIElement(e).inTL(0f, 0f);
    }
  }

  @Override
  public void buttonPressed(Object buttonId) {
    handleButton(buttonId);
  }

  private void handleButton(Object buttonId) {
    if (!ArondightState.exists()) {
      return;
    }
    ArondightIntel intel = ArondightIntel.get();
    if (intel == null) {
      return;
    }
    if (intel.handleButton(buttonId, new RefreshShim())) {
      rebuild();
    }
  }

  @Override
  public void positionChanged(PositionAPI position) {
    this.pos = position;
  }

  @Override
  public void renderBelow(float alphaMult) {
    if (pos == null) {
      return;
    }
    float x = pos.getX();
    float y = pos.getY();
    float w = pos.getWidth();
    float h = pos.getHeight();
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4ub((byte) 4, (byte) 6, (byte) 9, (byte) (255f * alphaMult));
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    GL11.glPopAttrib();
  }

  @Override
  public void render(float alphaMult) {
  }

  @Override
  public void advance(float amount) {
  }

  @Override
  public void processInput(List<InputEventAPI> events) {
  }

  private final class RefreshShim implements IntelUIAPI {
    @Override
    public void updateUIForItem(IntelInfoPlugin plugin) {
      rebuild();
    }

    @Override
    public void recreateIntelUI() {
      rebuild();
    }

    @Override
    public void showDialog(SectorEntityToken target, String trigger) {
    }

    @Override
    public void showDialog(SectorEntityToken target, InteractionDialogPlugin plugin) {
    }

    @Override
    public void showEditIntelMarkerDialog(MapMarkerIntel intel) {
    }

    @Override
    public void updateIntelList() {
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection) {
    }

    @Override
    public void updateIntelList(boolean retainCurrentSelection, List<IntelInfoPlugin> show) {
    }

    @Override
    public void selectItem(IntelInfoPlugin plugin) {
    }

    @Override
    public boolean isShowingCustomIntelSubset() {
      return false;
    }

    @Override
    public void showOnMap(SectorEntityToken token) {
    }

    @Override
    public void saveSmallDescScrollState() {
    }

    @Override
    public void restoreSmallDescScrollState() {
    }
  }
}
