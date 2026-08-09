package arondight.form.mechanics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.ArondightIds;
import arondight.util.ArondightSettings;

public class RhongomyniadAnchor implements AdvanceableListener {

    private final ShipAPI ship;
    private final String modId;
    private final IntervalUtil interval = new IntervalUtil(0.15f, 0.25f);
    private WeaponAPI armWeapon;
    private boolean anchored = false;

    public RhongomyniadAnchor(ShipAPI ship) {
        this.ship = ship;
        this.modId = "arondight_anchor_" + ship.getId();
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        if (armWeapon == null) {
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getSlot() != null
                        && ArondightIds.SLOT_ARM_R.equals(weapon.getSlot().getId())) {
                    armWeapon = weapon;
                    break;
                }
            }
            if (armWeapon == null) return;
        }

        boolean firing = armWeapon.isFiring();
        if (firing == anchored) return;
        anchored = firing;
        if (anchored) {
            float crawl = ArondightSettings.getFloat("arondight_anchor_mult", 0.05f);
            ship.getMutableStats().getMaxSpeed().modifyMult(modId, crawl);
            ship.getMutableStats().getAcceleration().modifyMult(modId, crawl * 4f);
        } else {
            ship.getMutableStats().getMaxSpeed().unmodifyMult(modId);
            ship.getMutableStats().getAcceleration().unmodifyMult(modId);
        }
    }
}
