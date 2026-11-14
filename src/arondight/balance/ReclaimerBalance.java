package arondight.balance;

public final class ReclaimerBalance {

  private ReclaimerBalance() {
  }

  public static final float K_ARMOR_ENERGY = 1.0f;
  public static final float K_ARMOR_KINETIC = 0.5f;
  public static final float K_ARMOR_HIGH_EXPLOSIVE = 2.0f;
  public static final float K_ARMOR_FRAGMENTATION = 0.25f;

  public static final float MIN_ARMOR_FRACTION = 0.05f;
  public static final float ARMOR_CELLS_PER_HIT = 15f;

  public static final String KINETIC = "KINETIC";
  public static final String HIGH_EXPLOSIVE = "HIGH_EXPLOSIVE";
  public static final String ENERGY = "ENERGY";
  public static final String FRAGMENTATION = "FRAGMENTATION";

  public static float kArmor(String damageType) {
    if (KINETIC.equals(damageType)) {
      return K_ARMOR_KINETIC;
    }
    if (HIGH_EXPLOSIVE.equals(damageType)) {
      return K_ARMOR_HIGH_EXPLOSIVE;
    }
    if (FRAGMENTATION.equals(damageType)) {
      return K_ARMOR_FRAGMENTATION;
    }
    return K_ARMOR_ENERGY;
  }

  public static float hitStrength(float damagePerShot, String damageType) {
    return damagePerShot * kArmor(damageType);
  }

  public static float effectiveArmor(float armorRating, boolean healthyGrid) {
    return healthyGrid
        ? armorRating / ARMOR_CELLS_PER_HIT
        : armorRating * MIN_ARMOR_FRACTION;
  }

  public static float reduction(float hitStrength, float armorRating, boolean healthyGrid) {
    float eff = effectiveArmor(armorRating, healthyGrid);
    return hitStrength / (hitStrength + eff);
  }

  public static float detuneGain(float hitStrength, float fromRating, float toRating,
      boolean healthyGrid) {
    return reduction(hitStrength, toRating, healthyGrid)
        / reduction(hitStrength, fromRating, healthyGrid);
  }

  public static final float DUTY_NEAR = 0.50f;
  public static final float DUTY_OUTER = 0.35f;
  public static final float DUTY_NEAR_LO = 0.35f, DUTY_NEAR_HI = 0.70f;

  public static final float SPOTTER_MULT = 1f + (10f / 21f) * 0.3f;

  public static final class Cell {
    public final String name;
    public final float armRawDps;
    public final float armDamagePerShot;
    public final String armDamageType;
    public final float fillRawDps;
    public final float fillDamagePerShot;
    public final String fillDamageType;
    public final float dutyCycle;
    public final float damageTakenMult;

    public Cell(String name, float armRawDps, float armDamagePerShot, String armDamageType,
        float fillRawDps, float fillDamagePerShot, String fillDamageType,
        float dutyCycle, float damageTakenMult) {
      this.name = name;
      this.armRawDps = armRawDps;
      this.armDamagePerShot = armDamagePerShot;
      this.armDamageType = armDamageType;
      this.fillRawDps = fillRawDps;
      this.fillDamagePerShot = fillDamagePerShot;
      this.fillDamageType = fillDamageType;
      this.dutyCycle = dutyCycle;
      this.damageTakenMult = damageTakenMult;
    }

    public float landedDps(float armorRating, boolean healthyGrid) {
      return landedDps(armorRating, healthyGrid, dutyCycle);
    }

    public float landedDps(float armorRating, boolean healthyGrid, float duty) {
      float arm = armRawDps * reduction(
          hitStrength(armDamagePerShot, armDamageType), armorRating, healthyGrid);
      float fill = fillRawDps == 0f ? 0f : fillRawDps * reduction(
          hitStrength(fillDamagePerShot, fillDamageType), armorRating, healthyGrid);
      return (arm + fill) * duty * damageTakenMult;
    }

    public float ttk(float pool, float armorRating, boolean healthyGrid) {
      float dps = landedDps(armorRating, healthyGrid);
      return dps <= 0f ? Float.POSITIVE_INFINITY : pool / dps;
    }

    public boolean emptyMount() {
      return fillRawDps == 0f;
    }
  }

  public static final float BASE_ARM_DPS = 100f, BASE_ARM_SHOT = 50f;
  public static final float DURENDAL_ARM_DPS = 400f / 3.8f, DURENDAL_ARM_SHOT = 400f;
  public static final float CURTANA_ARM_DPS = 10f, CURTANA_ARM_SHOT = 10f;
  public static final float LONGINUS_ARM_DPS = 350f / 3.6f, LONGINUS_ARM_SHOT = 350f;

  public static final float VOIDBLASTER_SHOT = 300f;
  public static final float VOIDBLASTER_DPS = 150f;
  public static final float VOIDBLASTER_SMOD_DPS = 250f;

  public static Cell[] referenceCells() {
    return new Cell[] {
        new Cell("base x empty", BASE_ARM_DPS, BASE_ARM_SHOT, KINETIC,
            0f, 0f, KINETIC, DUTY_NEAR, 1f),
        new Cell("Durendal x empty", DURENDAL_ARM_DPS, DURENDAL_ARM_SHOT, HIGH_EXPLOSIVE,
            0f, 0f, KINETIC, DUTY_NEAR, 1f),
        new Cell("Curtana x empty", CURTANA_ARM_DPS, CURTANA_ARM_SHOT, ENERGY,
            0f, 0f, KINETIC, DUTY_NEAR, 1f),
        new Cell("Longinus x empty", LONGINUS_ARM_DPS, LONGINUS_ARM_SHOT, KINETIC,
            0f, 0f, KINETIC, DUTY_OUTER, SPOTTER_MULT),

        new Cell("base x voidblaster", BASE_ARM_DPS, BASE_ARM_SHOT, KINETIC,
            VOIDBLASTER_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Durendal x voidblaster", DURENDAL_ARM_DPS, DURENDAL_ARM_SHOT, HIGH_EXPLOSIVE,
            VOIDBLASTER_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Curtana x voidblaster", CURTANA_ARM_DPS, CURTANA_ARM_SHOT, ENERGY,
            VOIDBLASTER_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Longinus x voidblaster", LONGINUS_ARM_DPS, LONGINUS_ARM_SHOT, KINETIC,
            VOIDBLASTER_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_OUTER, SPOTTER_MULT),

        new Cell("base x voidblaster s-mod", BASE_ARM_DPS, BASE_ARM_SHOT, KINETIC,
            VOIDBLASTER_SMOD_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Durendal x voidblaster s-mod", DURENDAL_ARM_DPS, DURENDAL_ARM_SHOT, HIGH_EXPLOSIVE,
            VOIDBLASTER_SMOD_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Curtana x voidblaster s-mod", CURTANA_ARM_DPS, CURTANA_ARM_SHOT, ENERGY,
            VOIDBLASTER_SMOD_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_NEAR, 1f),
        new Cell("Longinus x voidblaster s-mod", LONGINUS_ARM_DPS, LONGINUS_ARM_SHOT, KINETIC,
            VOIDBLASTER_SMOD_DPS, VOIDBLASTER_SHOT, HIGH_EXPLOSIVE, DUTY_OUTER, SPOTTER_MULT),
    };
  }

  public static Cell[] filledCells() {
    Cell[] all = referenceCells();
    int n = 0;
    for (Cell c : all) {
      if (!c.emptyMount()) {
        n++;
      }
    }
    Cell[] out = new Cell[n];
    int i = 0;
    for (Cell c : all) {
      if (!c.emptyMount()) {
        out[i++] = c;
      }
    }
    return out;
  }

  public static float spread(Cell[] cells, float armorRating, boolean healthyGrid) {
    float lo = Float.MAX_VALUE, hi = 0f;
    for (Cell c : cells) {
      float d = c.landedDps(armorRating, healthyGrid);
      if (d < lo) {
        lo = d;
      }
      if (d > hi) {
        hi = d;
      }
    }
    return lo <= 0f ? Float.POSITIVE_INFINITY : hi / lo;
  }

  public static final float R3_HULL = 1250f;
  public static final float MESH_CEILING_FRAC = 0.15f;
  public static final float AIMED_CEILING_FRAC = 0.40f;

  public static final String MESH = "MESH";
  public static final String AIMED = "AIMED";

  public static float postAttenuation(float raw, float damageMult) {
    return raw * damageMult;
  }

  public static float shareOfR3(float raw, float damageMult) {
    return postAttenuation(raw, damageMult) / R3_HULL;
  }

  public static float ceilingFor(String avoidabilityClass) {
    return MESH.equals(avoidabilityClass) ? MESH_CEILING_FRAC : AIMED_CEILING_FRAC;
  }

  public static float maxRawFor(String avoidabilityClass, float damageMult) {
    return ceilingFor(avoidabilityClass) * R3_HULL / damageMult;
  }

  public static final float MODULAR_EMIT_RADIUS = 245f;

  public static float reachFromCentre(float specRange) {
    return specRange + MODULAR_EMIT_RADIUS;
  }

  public static float tangentialSpeedRequired(float radius, float walkDegrees, float cadence) {
    return radius * (float) Math.toRadians(walkDegrees) / cadence;
  }

  public static float maxTrackingRadius(float maxSpeed, float walkDegrees, float cadence) {
    return maxSpeed * cadence / (float) Math.toRadians(walkDegrees);
  }

  public static float cageRadialEscapeSeconds(float cageRadius, float cageSpeed, float playerSpeed) {
    return cageRadius / (cageSpeed + playerSpeed);
  }

  public static float cageEscapeMeetingRadius(float cageRadius, float cageSpeed, float playerSpeed) {
    return cageRadius - cageSpeed * cageRadialEscapeSeconds(cageRadius, cageSpeed, playerSpeed);
  }

  public static float flightToRadius(float radius, float projectileSpeed) {
    return (radius - MODULAR_EMIT_RADIUS) / projectileSpeed;
  }

  public static int peakConcurrent(int shotsPerVolley, float specRange, float projectileSpeed,
      float cadence) {
    return peakConcurrentTimed(shotsPerVolley, specRange / projectileSpeed, cadence);
  }

  public static int peakConcurrentTimed(int shotsPerVolley, float lifetimeSeconds, float cadence) {
    int volleysAirborne = (int) Math.ceil(lifetimeSeconds / cadence);
    return shotsPerVolley * Math.max(1, volleysAirborne);
  }

  public static int ringShotsAfterDoors(int shots, int doors, float doorHalfDegrees) {
    float spacing = 360f / shots;
    int cutPerDoor = Math.max(0, (int) Math.ceil(2f * doorHalfDegrees / spacing) - 1);
    return Math.max(0, shots - doors * cutPerDoor);
  }

  public static int peakConcurrentSpiral(int arms, float specRange, float projectileSpeed,
      float tickSeconds, int cycle, int skipPerCycle) {
    int ticksAirborne = Math.max(1,
        (int) Math.ceil((specRange / projectileSpeed) / tickSeconds));
    if (cycle <= 0 || skipPerCycle <= 0) {
      return arms * ticksAirborne;
    }
    int firesPerCycle = cycle - skipPerCycle;
    int fires = (ticksAirborne / cycle) * firesPerCycle
        + Math.min(ticksAirborne % cycle, firesPerCycle);
    return arms * fires;
  }

  public static final int ENTITY_BUDGET = 350;
  public static final int ENTITY_FPS_DIP_COUNT = 496;
}
