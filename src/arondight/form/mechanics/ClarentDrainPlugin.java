package arondight.form.mechanics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.util.ArondightSettings;

public class ClarentDrainPlugin implements AdvanceableListener {

    private final ShipAPI ship;
    private final String modId;
    private final IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);
    private boolean draining = false;

    public ClarentDrainPlugin(ShipAPI ship) {
        this.ship = ship;
        this.modId = "arondight_clarent_" + ship.getId();
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        float elapsed = interval.getElapsed();

        StasisField field = StasisField.get(engine);
        int frozen = field.countFrozenBy(ship);
        boolean shouldDrain = frozen > 0;

        if (shouldDrain != draining) {
            draining = shouldDrain;
            if (draining) {
                ship.getMutableStats().getVentRateMult().modifyMult(modId,
                        ArondightSettings.getFloat("arondight_clarent_vent_mult", 2f));
            } else {
                ship.getMutableStats().getVentRateMult().unmodifyMult(modId);
            }
        }
        if (!draining) return;

        float cooldownBonus = ArondightSettings.getFloat("arondight_clarent_cooldown_bonus", 0.5f);
        for (com.fs.starfarer.api.combat.WeaponAPI weapon : ship.getAllWeapons()) {
            float remaining = weapon.getCooldownRemaining();
            if (remaining > 0) {
                weapon.setRemainingCooldownTo(Math.max(0f, remaining - elapsed * cooldownBonus));
            }
        }

        float leechPerSec = ArondightSettings.getFloat("arondight_clarent_leech", 15f);
        float perStatue = leechPerSec * elapsed;
        float drained = 0f;
        for (ShipAPI other : engine.getShips()) {
            if (!field.isFrozen(other) || other.getOwner() == ship.getOwner()) continue;
            float take = Math.min(perStatue, Math.max(0f, other.getHitpoints() - 1f));
            if (take > 0) {
                other.setHitpoints(other.getHitpoints() - take);
                drained += take;
            }
        }
        if (drained > 0) {
            ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + drained));
        }
    }
}
