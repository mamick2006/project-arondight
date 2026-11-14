package arondight.ui;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public final class ArondightUi {

  private ArondightUi() {
  }

  public static Color base() {
    return Misc.getBasePlayerColor();
  }

  public static Color dark() {
    return Misc.getDarkPlayerColor();
  }

  public static Color bright() {
    return Misc.getBrightPlayerColor();
  }

  public static Color text() {
    return Misc.getTextColor();
  }

  public static Color gray() {
    return Misc.getGrayColor();
  }

  public static Color hl() {
    return Misc.getHighlightColor();
  }

  public static Color good() {
    return Misc.getPositiveHighlightColor();
  }

  public static Color bad() {
    return Misc.getNegativeHighlightColor();
  }

  public static Color warn() {
    return new Color(255, 160, 60);
  }

  public static Color story() {
    return Misc.getStoryOptionColor();
  }

  public static Color selectWhite() {
    return new Color(235, 235, 235);
  }

  public static Color accent() {
    return new Color(130, 205, 180);
  }

  public static CustomUIPanelPlugin consoleRow() {
    return ArondightConsoleRoute.INSTANCE;
  }

  public static String i(float v) {
    return "" + Math.round(v);
  }

  public static String pct(float frac) {
    return Math.round(frac * 100f) + "%";
  }

  public static String sprite(String category, String id) {
    try {
      String s = Global.getSettings().getSpriteName(category, id);
      return s == null || s.isEmpty() ? null : s;
    } catch (Exception e) {
      return null;
    }
  }

  public static SpriteAPI spriteObj(String category, String id) {
    if (sprite(category, id) == null) {
      return null;
    }
    try {
      return Global.getSettings().getSprite(category, id);
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean image(CustomPanelAPI outer, TooltipMakerAPI el, float w, float h,
      String category, String spriteId, float pad) {
    String sprite = sprite(category, spriteId);
    if (sprite == null) {
      return false;
    }
    CustomPanelAPI holder = outer.createCustomPanel(w, h, null);
    TooltipMakerAPI img = holder.createUIElement(w, h, false);
    img.addImage(sprite, w, h, 0f);
    holder.addUIElement(img).inTL(0f, 0f);
    el.addCustom(holder, pad);
    return true;
  }

  public static final String FONT_CAPTION = "graphics/fonts/orbitron12condensed.fnt";
  public static final String FONT_VALUE = "graphics/fonts/insignia25LTaa.fnt";

  public static final float INSET = 5f;

  public static final float LINE_SLACK = 4f;

  public static float textWidth(CustomPanelAPI host, String font, String text) {
    TooltipMakerAPI scratch = host.createUIElement(400f, 20f, false);
    if (font != null) {
      scratch.setParaFont(font);
    }
    return scratch.createLabel(text, Color.white).computeTextWidth(text);
  }

  public static LabelAPI line(CustomPanelAPI host, String text, String font, Color color,
      float x, float y, boolean rightAligned) {
    float ink = textWidth(host, font, text);
    float w = (float) Math.ceil(ink) + 2f * INSET + LINE_SLACK;
    TooltipMakerAPI el = host.createUIElement(w, 20f, false);
    if (font != null) {
      el.setParaFont(font);
    }
    LabelAPI label = el.addPara(text, 0f);
    label.setColor(color);
    if (font != null) {
      el.setParaFontDefault();
    }
    host.addUIElement(el).inTL(rightAligned ? x - ink - INSET : x - INSET, y);
    return label;
  }

  public static final Color CLEAR = new Color(0, 0, 0, 0);

  private static final Color INVISIBLE = new Color(0, 0, 0, 255);

  public static ButtonAPI addInvisibleButton(TooltipMakerAPI el, Object data, float w,
      float h) {
    return el.addButton("", data, INVISIBLE, INVISIBLE, Alignment.MID, null, w, h, 0f);
  }

  public static Color alpha(Color c, int a) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
  }

  public static LabelAPI addHlPara(TooltipMakerAPI el, String text, float pad, String... highlights) {
    LabelAPI l = el.addPara(text, text(), pad);
    if (highlights != null && highlights.length > 0) {
      l.setHighlight(highlights);
      l.setHighlightColor(hl());
    }
    return l;
  }

  public abstract static class Tip implements TooltipMakerAPI.TooltipCreator {

    public static final float BG_ALPHA = 1f;

    private final float width;

    public Tip() {
      this(360f);
    }

    public Tip(float width) {
      this.width = width;
    }

    @Override
    public boolean isTooltipExpandable(Object param) {
      return false;
    }

    @Override
    public float getTooltipWidth(Object param) {
      return width;
    }

    @Override
    public final void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
      t.setBgAlpha(BG_ALPHA);
      fill(t, expanded, param);
    }

    protected abstract void fill(TooltipMakerAPI t, boolean expanded, Object param);
  }

  public static Tip tip(final String text) {
    return new Tip(300f) {
      @Override
      public void fill(TooltipMakerAPI t, boolean expanded, Object param) {
        t.addPara(text, text(), 0f);
      }
    };
  }

  public static void setEnabledSayingWhy(ButtonAPI button, boolean enabled) {
    button.setEnabled(enabled);
    button.setShowTooltipWhileInactive(true);
  }
}
