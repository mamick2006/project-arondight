package arondight.form.mechanics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.util.ArondightSettings;

public class GaeBolgPlugin implements AdvanceableListener, DamageDealtModifier,
        ArondightKillTracker.KillListener {

    private static final float POLL_JUMP_THRESHOLD = 0.2f;

    private final ShipAPI ship;
    private final String modId;
    private final IntervalUtil pollInterval = new IntervalUtil(0.04f, 0.06f);
    private final Map<WeaponAPI, Float> lastCooldown = new HashMap<>();
    private final Map<WeaponAPI, Integer> lastAmmo = new HashMap<>();
    private final Map<WeaponAPI, Float> lockRemaining = new HashMap<>();

    private boolean initialized = false;
    private ArondightKillTracker kills;

    public GaeBolgPlugin(ShipAPI ship) {
        this.ship = ship;
        this.modId = "arondight_gaebolg_" + ship.getId();
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) return;

        if (!initialized) {
            initialized = true;
            kills = ArondightKillTracker.get(engine);
            kills.subscribe(ship, this);
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDecorative()) continue;
                lastCooldown.put(weapon, weapon.getCooldownRemaining());
                if (weapon.usesAmmo()) lastAmmo.put(weapon, weapon.getAmmo());
            }
        }

        if (!lockRemaining.isEmpty()) {
            Iterator<Map.Entry<WeaponAPI, Float>> it = lockRemaining.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<WeaponAPI, Float> entry = it.next();
                float left = entry.getValue() - amount;
                if (left <= 0f) {
                    it.remove();
                    continue;
                }
                entry.setValue(left);
                entry.getKey().setForceNoFireOneFrame(true);
            }
        }

        pollInterval.advance(amount);
        if (!pollInterval.intervalElapsed()) return;

        for (Map.Entry<WeaponAPI, Float> entry : lastCooldown.entrySet()) {
            WeaponAPI weapon = entry.getKey();
            float now = weapon.getCooldownRemaining();
            boolean fired = now > entry.getValue() + POLL_JUMP_THRESHOLD;
            entry.setValue(now);
            if (fired && !lockRemaining.containsKey(weapon)) {
                lockWeapon(weapon);
            }
        }

        for (Map.Entry<WeaponAPI, Integer> entry : lastAmmo.entrySet()) {
            WeaponAPI weapon = entry.getKey();
            int now = weapon.getAmmo();
            int prev = entry.getValue();
            if (now < prev) {
                chargeDoubleAmmo(weapon, prev - now, now);
                entry.setValue(weapon.getAmmo());
            } else {
                entry.setValue(now);
            }
        }
    }

    private void lockWeapon(WeaponAPI weapon) {
        float lock = ArondightSettings.getFloat("arondight_gaebolg_lock", 3f);
        lockRemaining.put(weapon, lock);
        weapon.setRemainingCooldownTo(Math.max(weapon.getCooldownRemaining(), lock));
        lastCooldown.put(weapon, weapon.getCooldownRemaining());
    }

    private void chargeDoubleAmmo(WeaponAPI weapon, int spent, int ammoNow) {
        int extra = Math.round(spent
                * (ArondightSettings.getFloat("arondight_gaebolg_ammo_mult", 2f) - 1f));
        if (extra <= 0) return;
        int paid = Math.min(extra, ammoNow);
        if (paid > 0) {
            weapon.setAmmo(ammoNow - paid);
        }
        int unpaid = extra - paid;
        if (unpaid > 0) {
            selfDamage(weapon, unpaid);
        }
    }

    private void selfDamage(WeaponAPI weapon, int rounds) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        float perShot = 0f;
        if (weapon.getDerivedStats() != null) {
            perShot = weapon.getDerivedStats().getDamagePerShot();
        }
        if (perShot <= 0f && weapon.getDamage() != null) {
            perShot = weapon.getDamage().getDamage();
        }
        if (perShot <= 0f) return;
        float fraction = ArondightSettings.getFloat("arondight_gaebolg_self_damage_mult", 0.5f);
        engine.applyDamage(ship, ship.getLocation(), fraction * perShot * rounds,
                weapon.getDamage() != null ? weapon.getDamage().getType()
                        : com.fs.starfarer.api.combat.DamageType.ENERGY,
                0f, true, false, ship);
    }

    @Override
    public void onKill(ShipAPI victim) {
        if (lockRemaining.isEmpty() || !ship.isAlive()) return;
        for (WeaponAPI weapon : lockRemaining.keySet()) {
            weapon.setRemainingCooldownTo(0.05f);
            lastCooldown.put(weapon, 0.05f);
        }
        lockRemaining.clear();
        ship.setJitter(this, java.awt.Color.RED, 1f, 8, 0f, 20f);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (target == ship) return null;
        if (target instanceof ShipAPI && kills != null) {
            kills.noteDamage(ship, (ShipAPI) target);
        }
        WeaponAPI fired = null;
        if (param instanceof BeamAPI) {
            fired = ((BeamAPI) param).getWeapon();
        } else if (param instanceof DamagingProjectileAPI) {
            WeaponAPI candidate = ((DamagingProjectileAPI) param).getWeapon();
            if (candidate != null && candidate.getCooldown() < POLL_JUMP_THRESHOLD) {
                fired = candidate;
            }
        }
        if (fired != null && lastCooldown.containsKey(fired)
                && !lockRemaining.containsKey(fired)) {
            lockWeapon(fired);
        }
        damage.getModifier().modifyMult(modId,
                ArondightSettings.getFloat("arondight_gaebolg_damage_mult", 3f));
        return modId;
    }
}
