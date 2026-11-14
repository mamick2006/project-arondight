package arondight.balance;

public final class LatticeBalance {

  private LatticeBalance() {
  }

  public static final float SLOW_TICK_MIN = 0.4f;
  public static final float SLOW_TICK_MAX = 0.6f;

  public static final float MOVING_SPEED = 20f;

  private static final float MIN_HIT_OFFSET = 1f;

  public static float blinkHeading(float velX, float velY, float shipX, float shipY,
      float hitX, float hitY, float facingDeg) {
    if (velX * velX + velY * velY >= MOVING_SPEED * MOVING_SPEED) {
      return (float) Math.toDegrees(Math.atan2(velY, velX));
    }
    float awayX = shipX - hitX;
    float awayY = shipY - hitY;
    if (awayX * awayX + awayY * awayY >= MIN_HIT_OFFSET * MIN_HIT_OFFSET) {
      return (float) Math.toDegrees(Math.atan2(awayY, awayX));
    }
    return facingDeg;
  }

  public static float fullVentSeconds(float maxFlux, float dissipation) {
    return maxFlux / (2f * dissipation);
  }

  public static float arcsPerFullVent(float maxFlux, float dissipation) {
    return fullVentSeconds(maxFlux, dissipation) / ((SLOW_TICK_MIN + SLOW_TICK_MAX) * 0.5f);
  }

  public static float damagePerFullVent(float maxFlux, float dissipation, float arcDamage) {
    return arcsPerFullVent(maxFlux, dissipation) * arcDamage;
  }

  public static final float MAX_ARMOR_REDUCTION = 0.85f;

  public static float landedThroughArmor(float hit, float armor) {
    float reduction = Math.min(armor / (armor + hit), MAX_ARMOR_REDUCTION);
    return hit * (1f - reduction);
  }

  public static float landedPerFullVent(float maxFlux, float dissipation, float arcDamage,
      float armor) {
    return arcsPerFullVent(maxFlux, dissipation) * landedThroughArmor(arcDamage, armor);
  }
}
