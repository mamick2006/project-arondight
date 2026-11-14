package arondight.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

final class ArondightPips extends BaseCustomUIPanelPlugin {

  private final int invested;
  private final int cap;
  private PositionAPI pos;

  ArondightPips(int invested, int cap) {
    this.invested = invested;
    this.cap = cap;
  }

  static float width(int cap) {
    return ArondightFrame.pipsW(cap);
  }

  @Override
  public void positionChanged(PositionAPI position) {
    pos = position;
  }

  @Override
  public void render(float alphaMult) {
    if (pos == null || cap <= 0) {
      return;
    }
    float x = pos.getX();
    float y = pos.getY();
    float h = ArondightFrame.PIP_H;
    int lit = ArondightFrame.litOf(invested, cap);
    if (cap > ArondightFrame.PIPS_MAX) {
      float w = ArondightFrame.pipsW(ArondightFrame.PIPS_MAX);
      float litW = Math.round(w * lit / (float) cap);
      ArondightGl.fill(x, y, w, h, ArondightUi.dark(), 0.62f * alphaMult);
      ArondightGl.fill(x, y, litW, h, ArondightUi.story(), alphaMult);
      return;
    }
    float pitch = ArondightFrame.PIP_W + ArondightFrame.PIP_GAP;
    for (int i = 0; i < cap; i++) {
      if (i < lit) {
        ArondightGl.fill(x + i * pitch, y, ArondightFrame.PIP_W, h, ArondightUi.story(),
            alphaMult);
      } else {
        ArondightGl.fill(x + i * pitch, y, ArondightFrame.PIP_W, h, ArondightUi.dark(),
            0.62f * alphaMult);
      }
    }
  }
}
