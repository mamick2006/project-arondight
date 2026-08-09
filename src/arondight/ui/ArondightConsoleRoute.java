package arondight.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;

final class ArondightConsoleRoute extends BaseCustomUIPanelPlugin {

  static final ArondightConsoleRoute INSTANCE = new ArondightConsoleRoute();

  private ArondightConsoleRoute() {
  }

  @Override
  public void buttonPressed(Object buttonId) {
    ArondightConsolePanel.routeButton(buttonId);
  }
}
