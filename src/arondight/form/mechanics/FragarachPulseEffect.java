package arondight.form.mechanics;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import arondight.util.ArondightSettings;

public class FragarachPulseEffect implements EveryFrameWeaponEffectPlugin {

    private static final Color PULSE = new Color(120, 255, 200, 160);

    private float lastCooldown = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        float cooldown = weapon.getCooldownRemaining();
        boolean fired = cooldown > lastCooldown + 0.5f;
        lastCooldown = cooldown;
        if (!fired || weapon.getShip() == null) return;

        ShipAPI source = weapon.getShip();
        float radius = ArondightSettings.getFloat("arondight_pulse_radius", 800f);
        float duration = ArondightSettings.getFloat("arondight_pulse_duration", 4f);
        float slowMult = ArondightSettings.getFloat("arondight_pulse_slow_mult", 0.65f);
        float hasteMult = ArondightSettings.getFloat("arondight_pulse_haste_mult", 1.35f);

        StasisField field = StasisField.get(engine);
        float r2 = radius * radius;
        for (ShipAPI other : engine.getShips()) {
            if (!other.isAlive() || other.isHulk()) continue;
            Vector2f d = source.getLocation();
            float dx = other.getLocation().x - d.x, dy = other.getLocation().y - d.y;
            if (dx * dx + dy * dy > r2) continue;
            if (other.getOwner() == source.getOwner()) {
                field.applyHaste(other, hasteMult, duration);
            } else {
                field.applySlow(other, slowMult, duration);
            }
        }
        engine.addSmoothParticle(source.getLocation(), source.getVelocity(),
                radius * 2f, 0.6f, 0.5f, PULSE);
    }
}
