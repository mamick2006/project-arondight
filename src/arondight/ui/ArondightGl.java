package arondight.ui;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

final class ArondightGl {

  private ArondightGl() {
  }

  private static void begin(Color c, float alpha) {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4ub((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue(),
        (byte) Math.max(0, Math.min(255, Math.round(255f * alpha))));
  }

  private static void end() {
    GL11.glPopAttrib();
  }

  static void line(float x1, float y1, float x2, float y2, float width, Color c, float alpha) {
    begin(c, alpha);
    GL11.glLineWidth(width);
    GL11.glBegin(GL11.GL_LINES);
    GL11.glVertex2f(x1, y1);
    GL11.glVertex2f(x2, y2);
    GL11.glEnd();
    end();
  }

  static void frame(float x, float y, float w, float h, float width, Color c, float alpha) {
    begin(c, alpha);
    GL11.glLineWidth(width);
    GL11.glBegin(GL11.GL_LINE_LOOP);
    GL11.glVertex2f(x + 0.5f, y + 0.5f);
    GL11.glVertex2f(x + 0.5f, y + h - 0.5f);
    GL11.glVertex2f(x + w - 0.5f, y + h - 0.5f);
    GL11.glVertex2f(x + w - 0.5f, y + 0.5f);
    GL11.glEnd();
    end();
  }

  static void fill(float x, float y, float w, float h, Color c, float alpha) {
    begin(c, alpha);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x, y + h);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x + w, y);
    GL11.glEnd();
    end();
  }
}
