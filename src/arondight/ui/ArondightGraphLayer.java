package arondight.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

final class ArondightGraphLayer extends BaseCustomUIPanelPlugin {

  private static final float LINE_W = 1.4f;

  private final List<float[]> lines = new ArrayList<float[]>();
  private final List<Color> colors = new ArrayList<Color>();
  private PositionAPI pos;

  void addLine(float x1, float y1, float x2, float y2, Color c) {
    lines.add(new float[] { x1, y1, x2, y2 });
    colors.add(c);
  }

  void addElbow(float x1, float y1, float x2, float y2, float midY, Color c) {
    if (x1 == x2) {
      addLine(x1, y1, x2, y2, c);
      return;
    }
    addLine(x1, y1, x1, midY, c);
    addLine(Math.min(x1, x2) - LINE_W / 2f, midY, Math.max(x1, x2) + LINE_W / 2f, midY, c);
    addLine(x2, midY, x2, y2, c);
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
  public void renderBelow(float alphaMult) {
    if (pos == null || lines.isEmpty()) {
      return;
    }
    float px = pos.getX();
    float top = pos.getY() + pos.getHeight();
    for (int i = 0; i < lines.size(); i++) {
      float[] s = lines.get(i);
      ArondightGl.line(px + s[0], top - s[1], px + s[2], top - s[3], LINE_W,
          colors.get(i), 0.85f * alphaMult);
    }
  }
}
