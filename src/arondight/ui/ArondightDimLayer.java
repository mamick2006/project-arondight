package arondight.ui;

import java.awt.Color;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

final class ArondightDimLayer extends BaseCustomUIPanelPlugin {

  private PositionAPI pos;

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
        Color.black, 0.52f * alphaMult);
  }
}
