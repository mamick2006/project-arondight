package arondight.form;

public final class DescentNote {

  public static final String ROOT = "form_block_root";
  public static final String LEAF = "form_block_leaf";
  public static final String DESCENT = "form_block_descent";
  public static final String PARENT = "form_block_parent_first";

  private DescentNote() {
  }

  public static String key(int targetTier, int currentTier) {
    if (targetTier == 0) {
      return ROOT;
    }
    if (currentTier == 0) {
      return PARENT;
    }
    if (currentTier == 2) {
      return LEAF;
    }
    return DESCENT;
  }
}
