package arondight.balance;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BossConstants {

  public interface FloatSource {
    float getFloat(String key, float fallback);
  }

  public float stage2Hull;

  public float regenWindowSeconds;
  public float reviveGraceSeconds;
  public float reviveFraction;
  public float reviveFraction2;

  public float wallGraceSeconds;
  public float radialInterval;
  public float aimedInterval;
  public float fanInterval;
  public float swarmInterval;
  public float telegraphSeconds;

  public float cageInterval;
  public float cageRadius;
  public float cageSpeed;
  public float cageGap;

  public float figureInterval;

  public float holdSeconds;
  public float figureRadius;
  public float driftInterval;

  public float rainInterval;
  public float rainSpacing;
  public float rainPrecess;

  public float guidedCap;
  public float guidedHeavyCap;

  public float ringRadius;
  public float damageMult;

  public static BossConstants build(FloatSource src) {
    BossConstants k = new BossConstants();

    k.stage2Hull = src.getFloat("arondight_boss_stage2_hull", 0.50f);
    k.regenWindowSeconds = src.getFloat("arondight_boss_regen_window_seconds", 6f);
    k.reviveGraceSeconds = src.getFloat("arondight_boss_revive_grace_seconds", 3f);
    k.reviveFraction = src.getFloat("arondight_boss_regen_hull_fraction", 0.40f);
    k.reviveFraction2 = src.getFloat("arondight_boss_regen_hull_fraction_2", 0.25f);

    k.wallGraceSeconds = src.getFloat("arondight_boss_wall_grace_seconds", 6.5f);
    k.radialInterval = src.getFloat("arondight_boss_radial_interval", 2.5f);
    k.aimedInterval = src.getFloat("arondight_boss_aimed_interval", 0.9f);
    k.fanInterval = src.getFloat("arondight_boss_fan_interval", 6f);
    k.swarmInterval = src.getFloat("arondight_boss_swarm_interval", 5f);
    k.telegraphSeconds = src.getFloat("arondight_boss_telegraph_seconds", 1.2f);

    k.cageInterval = src.getFloat("arondight_boss_cage_interval", 8f);
    k.cageRadius = src.getFloat("arondight_boss_cage_radius", 1400f);
    k.cageSpeed = src.getFloat("arondight_boss_cage_speed", 220f);
    k.cageGap = src.getFloat("arondight_boss_cage_gap", 40f);

    k.figureInterval = src.getFloat("arondight_boss_figure_interval", 3.5f);

    k.holdSeconds = src.getFloat("arondight_boss_hold_seconds", 1.2f);
    k.figureRadius = src.getFloat("arondight_boss_figure_radius", 600f);
    k.driftInterval = src.getFloat("arondight_boss_drift_interval", 2f);

    k.rainInterval = src.getFloat("arondight_boss_rain_interval", 1.0f);
    k.rainSpacing = src.getFloat("arondight_boss_rain_spacing", 200f);
    k.rainPrecess = src.getFloat("arondight_boss_rain_precess", 0.8f);

    k.guidedCap = src.getFloat("arondight_boss_guided_cap", 8f);
    k.guidedHeavyCap = src.getFloat("arondight_boss_guided_heavy_cap", 1f);

    k.ringRadius = src.getFloat("arondight_boss_ring_radius", 5200f);
    k.damageMult = src.getFloat("arondight_boss_damage_mult", 0.5f);

    return k;
  }

  public static BossConstants fallbacks() {
    return build(new FloatSource() {
      @Override
      public float getFloat(String key, float fallback) {
        return fallback;
      }
    });
  }

  public Map<String, Float> flatten() {
    Map<String, Float> m = new LinkedHashMap<String, Float>();
    m.put("arondight_boss_stage2_hull", stage2Hull);
    m.put("arondight_boss_regen_window_seconds", regenWindowSeconds);
    m.put("arondight_boss_revive_grace_seconds", reviveGraceSeconds);
    m.put("arondight_boss_regen_hull_fraction", reviveFraction);
    m.put("arondight_boss_regen_hull_fraction_2", reviveFraction2);
    m.put("arondight_boss_wall_grace_seconds", wallGraceSeconds);
    m.put("arondight_boss_radial_interval", radialInterval);
    m.put("arondight_boss_aimed_interval", aimedInterval);
    m.put("arondight_boss_fan_interval", fanInterval);
    m.put("arondight_boss_swarm_interval", swarmInterval);
    m.put("arondight_boss_telegraph_seconds", telegraphSeconds);
    m.put("arondight_boss_cage_interval", cageInterval);
    m.put("arondight_boss_cage_radius", cageRadius);
    m.put("arondight_boss_cage_speed", cageSpeed);
    m.put("arondight_boss_cage_gap", cageGap);
    m.put("arondight_boss_figure_interval", figureInterval);
    m.put("arondight_boss_hold_seconds", holdSeconds);
    m.put("arondight_boss_figure_radius", figureRadius);
    m.put("arondight_boss_rain_interval", rainInterval);
    m.put("arondight_boss_rain_spacing", rainSpacing);
    m.put("arondight_boss_rain_precess", rainPrecess);
    m.put("arondight_boss_drift_interval", driftInterval);
    m.put("arondight_boss_guided_cap", guidedCap);
    m.put("arondight_boss_guided_heavy_cap", guidedHeavyCap);
    m.put("arondight_boss_ring_radius", ringRadius);
    m.put("arondight_boss_damage_mult", damageMult);
    return m;
  }

  public float pool(float coreHull) {
    return coreHull * (1f + reviveFraction + reviveFraction2);
  }

  public float[] stageShares() {
    float total = 1f + reviveFraction + reviveFraction2;
    return new float[] {
        0f,
        stage2Hull / total,
        (1f - stage2Hull) / total,
        reviveFraction / total,
        reviveFraction2 / total,
    };
  }
}
