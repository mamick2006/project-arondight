package arondight.ui;

public final class ArondightFrame {

  static final float PICTURE_SHORT = 60f;
  static final float PICTURE_TALL = 96f;
  static final float TALL_FROM = 800f;

  static final float LEFT_MIN = 626f;
  static final float LEFT_SHARE = 0.46f;
  static final float NARROW_BELOW = 1160f;
  static final float COLUMN_GAP = 10f;

  static final float TILE_GAP = 10f;
  static final float TILE_W_MAX = 130f;
  static final float TILE_TOP = 4f;
  static final float NAME_H = 18f;
  static final float ROW_GAP = 22f;
  static final float SHIP_CELL = 96f;
  static final float SHIP_CELL_HALF = 48f;

  static final float HEAD_H = 18f;
  static final float FORMS_HEAD_DROP = 10f;
  static final float FORMS_COST_DROP = 33f;
  static final float FORMS_ROW_DROP = 58f;
  static final float BOTTOM_PAD = 6f;
  static final float HERO_MIN = 120f;

  static final float READOUT_X = 16f;
  static final float SP_CAPTION_UP = 112f;
  static final float SP_VALUE_UP = 96f;
  static final float LOW_CAPTION_UP = 62f;
  static final float LOW_VALUE_UP = 46f;
  static final float PIPS_UP = 16f;
  static final float PIP_W = 8f;
  static final float PIP_H = 6f;
  static final float PIP_GAP = 2f;
  static final int PIPS_MAX = 20;

  static final int COST_COUNT = 3;
  static final int COST_SUPPLIES = 0;
  static final int COST_RECOVERY = 1;
  static final int COST_DP = 2;
  static final float COST_GAP = 24f;

  static final float SEALED_H = 20f;

  static final float CONTROL_W = 100f;
  static final float CONTROL_H = 20f;
  static final float CONTROL_GAP = 6f;
  static final float WEAVE_HEAD_Y = 28f;
  static final float WEAVE_COST_Y = 51f;
  static final float WEAVE_LINE_STEP = 18f;
  static final float WEAVE_VIEW_Y = WEAVE_COST_Y + 2f * WEAVE_LINE_STEP + 5f;
  static final float WEAVE_PAD = 10f;
  static final float SCROLL_GUTTER = 16f;

  static final float ROOT_BOX = 48f;
  static final float ENTRY_GAP = 22f;
  static final float NODE = 44f;
  static final float LABEL_BAND = 28f;
  static final float RING_STEP_MIN = NODE + 8f;
  static final float RING_STEP_MAX = NODE + 44f;

  public final float width;
  public final float height;

  public final float leftW;
  public final float rightX;
  public final float rightW;

  public final float picture;
  public final float tileW;
  public final float tileH;
  public final float shipCell;
  public final float gridX;
  public final float gridW;

  public final float heroH;
  public final float formsHeadY;
  public final float formsCostY;
  private final float[] rowY = new float[3];

  public final float controlsX;
  public final float weaveViewX;
  public final float weaveViewW;
  public final float weaveViewH;

  public ArondightFrame(float width, float height) {
    this.width = width;
    this.height = height;
    leftW = width < NARROW_BELOW ? Math.round(width / 2f)
        : Math.max(LEFT_MIN, Math.round(LEFT_SHARE * width));
    rightX = leftW + COLUMN_GAP;
    rightW = width - rightX;

    picture = height < TALL_FROM ? PICTURE_SHORT : PICTURE_TALL;
    tileW = Math.min(TILE_W_MAX, (float) Math.floor((leftW - 5f * TILE_GAP) / 6f));
    tileH = TILE_TOP + picture + NAME_H;
    shipCell = tileW >= SHIP_CELL ? SHIP_CELL : SHIP_CELL_HALF;
    gridW = 6f * tileW + 5f * TILE_GAP;
    gridX = (float) Math.floor((leftW - gridW) / 2f);

    float below = FORMS_ROW_DROP + 3f * tileH + 2f * ROW_GAP + BOTTOM_PAD;
    heroH = Math.max(HERO_MIN, height - below);
    formsHeadY = heroH + FORMS_HEAD_DROP;
    formsCostY = heroH + FORMS_COST_DROP;
    rowY[0] = heroH + FORMS_ROW_DROP;
    rowY[1] = rowY[0] + tileH + ROW_GAP;
    rowY[2] = rowY[1] + tileH + ROW_GAP;

    controlsX = width - controlsRowW();
    weaveViewX = rightX + WEAVE_PAD;
    weaveViewW = rightW - 2f * WEAVE_PAD - SCROLL_GUTTER;
    weaveViewH = height - WEAVE_VIEW_Y;
  }

  static float controlsRowW() {
    return 3f * CONTROL_W + 2f * CONTROL_GAP;
  }

  public float rowY(int row) {
    return rowY[row];
  }

  public static int rowOf(int index) {
    return index == 0 ? 0 : index <= 3 ? 1 : 2;
  }

  public float tileX(int index) {
    float pitch = tileW + TILE_GAP;
    if (index == 0) {
      return (float) Math.floor((leftW - tileW) / 2f);
    }
    if (index <= 3) {
      return gridX + 2f * (index - 1) * pitch + pitch / 2f;
    }
    return gridX + (index - 4) * pitch;
  }

  public float tileY(int index) {
    return rowY[rowOf(index)];
  }

  public float[] pictureRect(float aspect) {
    float w;
    float h;
    if (heroH * aspect <= leftW) {
      w = Math.round(heroH * aspect);
      h = heroH;
    } else {
      w = leftW;
      h = Math.round(leftW / aspect);
    }
    return new float[] { (float) Math.floor((leftW - w) / 2f),
        (float) Math.floor((heroH - h) / 2f), w, h };
  }

  public float sealedY() {
    return rowY[2] + TILE_TOP + picture / 2f - SEALED_H / 2f;
  }

  public static float ringStep(float viewH, int maxDepth) {
    if (maxDepth <= 0) {
      return 0f;
    }
    float half = viewH / 2f - LABEL_BAND;
    return Math.max(RING_STEP_MIN, Math.min(RING_STEP_MAX, (half - ringFixed()) / maxDepth));
  }

  static float ringFixed() {
    return ROOT_BOX / 2f + ENTRY_GAP + NODE;
  }

  public static float weaveInkH(float step, int maxDepth) {
    return 2f * (ringFixed() + maxDepth * step + LABEL_BAND);
  }

  public static int litOf(int invested, int cap) {
    return Math.max(0, Math.min(invested, cap));
  }

  public static float[] costRights(float right, float[] widths) {
    float[] rights = new float[widths.length];
    float edge = right;
    for (int i = widths.length - 1; i >= 0; i--) {
      rights[i] = edge;
      edge -= widths[i] + COST_GAP;
    }
    return rights;
  }

  public float costsRight() {
    return leftW - READOUT_X;
  }

  public static float investedMaxRight() {
    return READOUT_X + pipsW(PIPS_MAX);
  }

  public static float pipsW(int cap) {
    int n = Math.min(Math.max(cap, 0), PIPS_MAX);
    return n <= 0 ? 0f : n * PIP_W + (n - 1) * PIP_GAP;
  }
}
