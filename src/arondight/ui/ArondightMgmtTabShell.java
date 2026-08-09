package arondight.ui;

public class ArondightMgmtTabShell extends com.fs.starfarer.ui.interfacenew
    implements com.fs.starfarer.ui.newui.Y {

  public ArondightMgmtTabShell(float width, float height) {
    super(width, height);
    addComponent(ArondightMgmtScreen.stageForShell(this, width, height)).inTL(0f, 0f);
  }

  @Override
  public void tabClosed() {
    ArondightMgmtScreen.onTabClosed(this);
  }
}
