package arondight.ui;

import java.awt.Color;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

final class ArondightDimLayer extends BaseCustomUIPanelPlugin {

  static final float LOCKED = 0.52f;
  static final float SEALED = 0.78f;

  private final float strength;
  private PositionAPI pos;

  ArondightDimLayer() {
    this(LOCKED);
  }

  ArondightDimLayer(float strength) {
    this.strength = strength;
  }

  @Override
  public void positionChanged(PositionAPI position) {
    pos = position;
  }

  @Override
  public void render(float alphaMult) {
    if (pos == null) {
      return;
    }
    ArondightGl.fill(pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
        Color.black, strength * alphaMult);
  }
}
