package arondight.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.ArondightIds;
import arondight.form.mechanics.ArondightKillTracker;
import arondight.form.mechanics.WardMechanics;
import arondight.util.ArondightSettings;

public class LatticeCombatPlugin implements AdvanceableListener, DamageTakenModifier,
        DamageDealtModifier, HullDamageAboutToBeTakenListener, ArondightKillTracker.KillListener {

    private static final Color LATTICE_GLOW = new Color(160, 220, 255, 160);

    private static final java.util.Set<String> COMBAT_NODES = new java.util.HashSet<>(java.util.Arrays.asList(
            ArondightIds.NODE_MUNITIONS_LOOM, ArondightIds.NODE_ARM_ACTUATORS,
            ArondightIds.NODE_WATCHDOG_REMOTES, ArondightIds.NODE_SELF_WEAVE,
            ArondightIds.NODE_AFTERIMAGE, ArondightIds.NODE_STATIC_BLEED,
            ArondightIds.NODE_HARVEST_REFLEX, ArondightIds.NODE_KINETIC_BUFFER));

    static boolean wantsCombatPlugin(Set<String> nodes) {
        for (String node : nodes) {
            if (COMBAT_NODES.contains(node)) return true;
        }
        return false;
    }

    private final ShipAPI ship;
    private final String modId;
    private final boolean loom, actuators, drones, selfWeave, afterimage, bleed, harvest, buffer;

    private final IntervalUtil slowInterval = new IntervalUtil(0.4f, 0.6f);
    private final IntervalUtil loomInterval = new IntervalUtil(0.9f, 1.1f);

    private boolean initialized = false;
    private List<WeaponAPI> missileWeapons;
    private float loomAmmoCarry = 0f;
    private float selfWeaveBudget = 0f;
    private float afterimageCooldown = 0f;
    private final List<ShipAPI> watchdogs = new ArrayList<>(2);
    private final float[] watchdogRespawnClock = new float[2];
    private float droneOrbitAngle = 0f;
    private float droneLeash = 220f;

    public LatticeCombatPlugin(ShipAPI ship, Set<String> nodes) {
        this.ship = ship;
        this.modId = "arondight_lattice_" + ship.getId();
        this.loom = nodes.contains(ArondightIds.NODE_MUNITIONS_LOOM);
        this.actuators = nodes.contains(ArondightIds.NODE_ARM_ACTUATORS);
        this.drones = nodes.contains(ArondightIds.NODE_WATCHDOG_REMOTES);
        this.selfWeave = nodes.contains(ArondightIds.NODE_SELF_WEAVE);
        this.afterimage = nodes.contains(ArondightIds.NODE_AFTERIMAGE);
        this.bleed = nodes.contains(ArondightIds.NODE_STATIC_BLEED);
        this.harvest = nodes.contains(ArondightIds.NODE_HARVEST_REFLEX);
        this.buffer = nodes.contains(ArondightIds.NODE_KINETIC_BUFFER);
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) return;

        if (!initialized) {
            initialized = true;
            init(engine);
        }

        if (afterimageCooldown > 0) afterimageCooldown -= amount;
        if (drones && !watchdogs.isEmpty()) keepDronesLeashed(amount);

        slowInterval.advance(amount);
        if (slowInterval.intervalElapsed()) {
            float elapsed = slowInterval.getElapsed();
            if (selfWeave) selfWeaveTick(elapsed);
            if (drones) {
                droneLeash = ArondightSettings.getFloat("arondight_watchdog_leash", 220f);
                watchdogRespawnTick(engine, elapsed);
                watchdogAutofireTick();
            }
            if (bleed) {
                if (ship.getFluxTracker().isVenting()) {
                    bleedTick(engine);
                } else {
                    noteBleed("bleed:not-venting");
                }
            }
        }

        if (loom) {
            loomInterval.advance(amount);
            if (loomInterval.intervalElapsed()) loomTick(loomInterval.getElapsed());
        }
    }

    private void init(CombatEngineAPI engine) {
        if (harvest) {
            ArondightKillTracker.get(engine).subscribe(ship, this);
        }
        if (loom) {
            missileWeapons = new ArrayList<>();
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getType() == WeaponAPI.WeaponType.MISSILE && weapon.usesAmmo()) {
                    missileWeapons.add(weapon);
                }
            }
        }
        if (drones) {
            droneLeash = ArondightSettings.getFloat("arondight_watchdog_leash", 220f);
            for (int i = 0; i < 2; i++) {
                watchdogs.add(spawnWatchdog(engine, i * 180f));
            }
        }
    }

    static final String WATCHDOG_VARIANT_DEFAULT = "drone_terminator";

    private ShipAPI spawnWatchdog(CombatEngineAPI engine, float angleDeg) {
        String variantId = ArondightSettings.getString("arondight_watchdog_variant",
                WATCHDOG_VARIANT_DEFAULT);
        try {
            ShipAPI drone = engine.getFleetManager(ship.getOwner())
                    .spawnShipOrWing(variantId, orbitPoint(angleDeg), ship.getFacing());
            if (drone != null) {
                drone.setDrone(true);
                drone.giveCommand(ShipCommand.SELECT_GROUP, null, 99);
                drone.setShipAI(null);
            }
            return drone;
        } catch (Exception e) {
            arondight.util.ArondightLog.warnOnce("watchdog_spawn",
                    "Watchdog Remotes could not spawn '" + variantId + "' (" + e + "); retrying on the respawn clock");
            return null;
        }
    }

    private void watchdogRespawnTick(CombatEngineAPI engine, float elapsed) {
        float respawnSecs = ArondightSettings.getFloat("arondight_watchdog_respawn", 10f);
        for (int i = 0; i < watchdogs.size(); i++) {
            ShipAPI drone = watchdogs.get(i);
            if (drone != null && drone.isAlive()) continue;
            watchdogRespawnClock[i] += elapsed;
            if (watchdogRespawnClock[i] < respawnSecs) continue;
            watchdogRespawnClock[i] = 0f;
            watchdogs.set(i, spawnWatchdog(engine, droneOrbitAngle + i * 180f));
        }
    }

    private void watchdogAutofireTick() {
        for (int i = 0; i < watchdogs.size(); i++) {
            ShipAPI drone = watchdogs.get(i);
            if (drone == null || !drone.isAlive()) continue;
            List<WeaponGroupAPI> groups = drone.getWeaponGroupsCopy();
            for (int g = 0; g < groups.size(); g++) {
                if (!groups.get(g).isAutofiring()) {
                    drone.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, g);
                }
            }
        }
    }

    private void keepDronesLeashed(float amount) {
        droneOrbitAngle += amount * 40f;
        float radius = ship.getCollisionRadius() + 90f;
        float shipX = ship.getLocation().x, shipY = ship.getLocation().y;
        for (int i = 0; i < watchdogs.size(); i++) {
            ShipAPI drone = watchdogs.get(i);
            if (drone == null || !drone.isAlive()) continue;
            double rad = Math.toRadians(droneOrbitAngle + i * 180f);
            float anchorX = shipX + (float) (radius * Math.cos(rad));
            float anchorY = shipY + (float) (radius * Math.sin(rad));
            float dx = drone.getLocation().x - anchorX, dy = drone.getLocation().y - anchorY;
            if (dx * dx + dy * dy > droneLeash * droneLeash) {
                drone.getVelocity().set(-dx * 1.5f, -dy * 1.5f);
            }
        }
    }

    private Vector2f orbitPoint(float angleDeg) {
        float r = ship.getCollisionRadius() + 90f;
        double rad = Math.toRadians(angleDeg);
        return new Vector2f(ship.getLocation().x + (float) (r * Math.cos(rad)),
                ship.getLocation().y + (float) (r * Math.sin(rad)));
    }

    private void selfWeaveTick(float elapsed) {
        float capFraction = ArondightSettings.getFloat("arondight_selfweave_cap", 1.0f);
        float ratePerSec = ArondightSettings.getFloat("arondight_selfweave_rate", 0.005f);
        float max = ship.getMaxHitpoints();
        if (selfWeaveBudget >= capFraction * max || ship.getHitpoints() >= max) return;
        float heal = Math.min(ratePerSec * max * elapsed,
                Math.min(capFraction * max - selfWeaveBudget, max - ship.getHitpoints()));
        if (heal <= 0) return;
        ship.setHitpoints(ship.getHitpoints() + heal);
        selfWeaveBudget += heal;
    }

    public static final java.util.LinkedList<String> bleedOutcomesForTest = new java.util.LinkedList<>();

    private static void noteBleed(String outcome) {
        bleedOutcomesForTest.addLast(outcome);
        while (bleedOutcomesForTest.size() > 16) bleedOutcomesForTest.removeFirst();
    }

    private void bleedTick(CombatEngineAPI engine) {
        float range = ArondightSettings.getFloat("arondight_bleed_range", 500f);
        float damage = ArondightSettings.getFloat("arondight_bleed_damage", 60f);
        ShipAPI target = nearestEnemy(engine, range);
        if (target == null) {
            noteBleed("bleed:no-target-in-" + (int) range);
            return;
        }
        engine.spawnEmpArcPierceShields(ship, ship.getLocation(), ship, target, DamageType.ENERGY,
                damage, damage * 2f, range * 1.5f, "tachyon_lance_emp_impact",
                8f, LATTICE_GLOW, Color.WHITE);
        noteBleed("bleed:arc:" + (int) damage + ":o" + target.getOwner());
    }

    private ShipAPI nearestEnemy(CombatEngineAPI engine, float range) {
        ShipAPI best = null;
        float bestD2 = range * range;
        for (ShipAPI other : engine.getShips()) {
            if (!other.isAlive() || other.isHulk() || other.getOwner() == ship.getOwner()
                    || other.isFighter()) continue;
            float dx = other.getLocation().x - ship.getLocation().x;
            float dy = other.getLocation().y - ship.getLocation().y;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = other;
            }
        }
        return best;
    }

    private void loomTick(float elapsed) {
        if (missileWeapons == null || missileWeapons.isEmpty()) return;
        loomAmmoCarry += ArondightSettings.getFloat("arondight_loom_ammo_per_sec", 0.04f) * elapsed;
        if (loomAmmoCarry < 1f) return;
        loomAmmoCarry -= 1f;
        WeaponAPI neediest = null;
        float worstFraction = 1f;
        for (WeaponAPI weapon : missileWeapons) {
            if (weapon.getAmmoTracker().getMaxAmmo() <= 0) continue;
            float fraction = weapon.getAmmoTracker().getAmmo()
                    / (float) weapon.getAmmoTracker().getMaxAmmo();
            if (fraction < worstFraction) {
                worstFraction = fraction;
                neediest = weapon;
            }
        }
        if (neediest != null && worstFraction < 1f) {
            neediest.getAmmoTracker().setAmmo(neediest.getAmmoTracker().getAmmo() + 1);
        }
    }

    @Override
    public void onKill(ShipAPI victim) {
        if (!harvest || !ship.isAlive()) return;
        float hullFraction = ArondightSettings.getFloat("arondight_harvest_hull", 0.10f);
        float armorFraction = ArondightSettings.getFloat("arondight_harvest_armor", 0.10f);
        float cr = ArondightSettings.getFloat("arondight_harvest_cr", 0.02f);
        ship.setHitpoints(Math.min(ship.getMaxHitpoints(),
                ship.getHitpoints() + ship.getMaxHitpoints() * hullFraction));
        restoreArmor(armorFraction);
        ship.setCurrentCR(Math.min(1f, ship.getCurrentCR() + cr));
        Global.getCombatEngine().addSmoothParticle(ship.getLocation(), ship.getVelocity(),
                ship.getCollisionRadius() * 2f, 0.8f, 0.4f, LATTICE_GLOW);
    }

    private void restoreArmor(float fraction) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null || fraction <= 0f) return;
        float max = grid.getMaxArmorInCell();
        float[][] cells = grid.getGrid();
        int damaged = 0, total = 0;
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                total++;
                if (cells[x][y] < max) damaged++;
            }
        }
        if (damaged == 0) return;
        float perCell = fraction * max * total / damaged;
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                float cur = cells[x][y];
                if (cur < max) {
                    grid.setArmorValue(x, y, Math.min(max, cur + perCell));
                }
            }
        }
        ship.syncWithArmorGridState();
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (!buffer || shieldHit || target != ship) return null;
        WardMechanics ward = WardMechanics.of(ship);
        if (ward != null && ward.isWardActive()) return null;
        float fraction = ArondightSettings.getFloat("arondight_buffer_fraction", 0.15f);
        ship.getFluxTracker().increaseFlux(damage.getDamage() * fraction, false);
        damage.getModifier().modifyMult(modId, 1f - fraction);
        return modId;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (harvest && target instanceof ShipAPI && Global.getCombatEngine() != null) {
            ArondightKillTracker.get(Global.getCombatEngine()).noteDamage(ship, (ShipAPI) target);
        }
        if (!actuators) return null;
        WeaponAPI weapon = weaponOf(param);
        if (weapon == null || weapon.getSlot() == null
                || !ArondightIds.SLOT_ARM_R.equals(weapon.getSlot().getId())) return null;
        damage.getModifier().modifyPercent(modId,
                ArondightSettings.getFloat("arondight_actuators_bonus", 25f));
        return modId;
    }

    private static WeaponAPI weaponOf(Object param) {
        if (param instanceof DamagingProjectileAPI) return ((DamagingProjectileAPI) param).getWeapon();
        if (param instanceof BeamAPI) return ((BeamAPI) param).getWeapon();
        if (param instanceof MissileAPI) return ((MissileAPI) param).getWeapon();
        return null;
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI target, Vector2f point, float damageAmount) {
        if (!afterimage || target != ship || afterimageCooldown > 0) return false;
        float threshold = ArondightSettings.getFloat("arondight_afterimage_threshold", 0.05f);
        if (damageAmount < ship.getMaxHitpoints() * threshold) return false;

        float dist = ArondightSettings.getFloat("arondight_afterimage_dist", 250f);
        double rad = Math.toRadians(ship.getFacing());
        Vector2f dest = new Vector2f(ship.getLocation().x + (float) (dist * Math.cos(rad)),
                ship.getLocation().y + (float) (dist * Math.sin(rad)));

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && blinkSpotClear(engine, dest)) {
            afterimageCooldown = ArondightSettings.getFloat("arondight_afterimage_cooldown", 12f);
            engine.addSmoothParticle(ship.getLocation(), new Vector2f(), ship.getCollisionRadius() * 3f,
                    1f, 0.5f, LATTICE_GLOW);
            ship.getLocation().set(dest);
            ship.setJitter(this, LATTICE_GLOW, 1f, 10, 0f, 30f);
        }
        return false;
    }

    private boolean blinkSpotClear(CombatEngineAPI engine, Vector2f dest) {
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || !other.isAlive() || other.isFighter()) continue;
            float minDist = other.getCollisionRadius() + ship.getCollisionRadius();
            float dx = other.getLocation().x - dest.x, dy = other.getLocation().y - dest.y;
            if (dx * dx + dy * dy < minDist * minDist) return false;
        }
        return true;
    }
}
