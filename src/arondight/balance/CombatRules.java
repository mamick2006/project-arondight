package arondight.balance;

public final class CombatRules {

  private CombatRules() {
  }

  public static boolean countsAsKill(boolean fighter, boolean drone) {
    return !fighter && !drone;
  }

  public static float wardReflectFraction(boolean flash, boolean inPerfectWindow, float held,
      float perfect, float sloppy) {
    if (!flash) {
      return held;
    }
    return inPerfectWindow ? perfect : sloppy;
  }
}
