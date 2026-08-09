package arondight.ui;

import java.awt.Color;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;

final class ArondightNodeChrome extends BaseCustomUIPanelPlugin {

  enum State { LOCKED, ACTIVATABLE, ACTIVATED, CURRENT }

  private static final Color LOCKED_FRAME = new Color(70, 78, 82);

  private final State state;
  private final SpriteAPI icon;
  private final float iconSize;
  private PositionAPI pos;

  ArondightNodeChrome(State state) {
    this(state, null, null, 0f);
  }

  ArondightNodeChrome(State state, String category, String spriteId, float iconSize) {
    this.state = state;
    this.icon = category == null ? null : ArondightUi.spriteObj(category, spriteId);
    this.iconSize = iconSize;
  }

  @Override
  public void positionChanged(PositionAPI position) {
    pos = position;
  }

  @Override
  public void buttonPressed(Object buttonId) {
    ArondightConsolePanel.routeButton(buttonId);
  }

  @Override
  public void render(float alphaMult) {
    if (pos == null) {
      return;
    }
    float x = pos.getX();
    float y = pos.getY();
    float w = pos.getWidth();
    float h = pos.getHeight();
    if (icon != null) {
      icon.setSize(iconSize, iconSize);
      icon.setAlphaMult(alphaMult);
      icon.renderAtCenter(x + w / 2f, y + h / 2f);
    }
    switch (state) {
      case LOCKED:
        ArondightGl.frame(x, y, w, h, 1f, LOCKED_FRAME, alphaMult);
        break;
      case ACTIVATABLE:
        ArondightGl.frame(x, y, w, h, 1f, ArondightUi.selectWhite(), alphaMult);
        break;
      case ACTIVATED:
      case CURRENT:
        ArondightGl.frame(x, y, w, h, 1f, ArondightUi.good(), alphaMult);
        ArondightGl.frame(x + 3f, y + 3f, w - 6f, h - 6f, 1f, ArondightUi.good(),
            alphaMult);
        break;
      default:
        break;
    }
  }
}
