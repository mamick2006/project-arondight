package arondight.form.mechanics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import arondight.util.ArondightSettings;

public class PartAnimationEffect implements EveryFrameWeaponEffectPlugin {

    private static final String SLOT_ARM_R = "WS_ARM_R";
    private static final String SLOT_ARM_L = "WS_ARM_L";
    private static final String SLOT_DECO_ARM_L = "WS_DECO_ARM_L";
    private static final String SLOT_PAULDRON_L = "WS_DECO_PL";
    private static final String SLOT_PAULDRON_R = "WS_DECO_PR";

    private static final int RESOLVE_ATTEMPTS = 120;

    private static final float PAULDRON_RECOIL_SHARE = 0.5f;

    private static final float BEAM_RECOIL_SHARE = 0.7f;

    private static final float RECOIL_REFERENCE_DAMAGE = 100f;
    private static final float RECOIL_SCALE_MIN = 0.7f;
    private static final float RECOIL_SCALE_MAX = 1.8f;

    private static final float FIRE_EPSILON = 0.02f;

    private boolean resolved;
    private boolean inert;
    private int attempts;

    private ShipAPI ship;
    private Side left;
    private Side right;

    private float pauldronTrack;
    private float armTrack;
    private float recoilPx;
    private float recoilDecay;

    private static final class Side {
        WeaponAPI driver;
        boolean driverIsBeam;
        float recoil;
        WeaponAPI armPart;
        float armOrigin;
        float armRest;
        WeaponAPI pauldron;
        float pauldronOrigin;
        float pauldronRest;
        float lastCooldown;
        float kick;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || inert) return;
        if (!resolved && !resolve(weapon)) return;
        if (ship == null) return;
        float facing = ship.getFacing();
        animate(right, facing, amount);
        animate(left, facing, amount);
    }

    private void animate(Side side, float facing, float amount) {
        if (side == null) return;

        side.kick -= amount / recoilDecay;
        if (side.kick < 0f) side.kick = 0f;
        if (side.driverIsBeam) {
            float charge = side.driver.getChargeLevel();
            if (charge > side.kick) side.kick = charge;
        } else {
            float cooldown = side.driver.getCooldownRemaining();
            if (cooldown > side.lastCooldown + FIRE_EPSILON) side.kick = 1f;
            side.lastCooldown = cooldown;
        }

        float lean = shortestRotation(facing, side.driver.getCurrAngle());

        if (side.armPart != null) {
            if (side.armPart != side.driver) {
                side.armPart.setCurrAngle(facing + side.armRest + lean * armTrack);
            }
            SpriteAPI sprite = side.armPart.getSprite();
            if (sprite != null) {
                sprite.setCenterY(side.armOrigin + side.kick * side.recoil);
            }
        }
        if (side.pauldron != null) {
            side.pauldron.setCurrAngle(facing + side.pauldronRest + lean * pauldronTrack);
            SpriteAPI sprite = side.pauldron.getSprite();
            if (sprite != null) {
                sprite.setCenterY(side.pauldronOrigin
                        + side.kick * side.recoil * PAULDRON_RECOIL_SHARE);
            }
        }
    }

    private static float shortestRotation(float from, float to) {
        float d = (to - from) % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private boolean resolve(WeaponAPI self) {
        attempts++;
        ShipAPI host = self.getShip();
        if (host == null || host.getAllWeapons() == null || host.getAllWeapons().isEmpty()) {
            if (attempts >= RESOLVE_ATTEMPTS) {
                resolved = true;
                inert = true;
                Global.getLogger(PartAnimationEffect.class).warn(
                        "Project Arondight: mech part animation found no weapons on its host"
                                + " after " + attempts + " frames; the arms and pauldrons"
                                + " stay static this battle");
            }
            return false;
        }
        resolved = true;
        ship = host;

        WeaponAPI armR = null;
        WeaponAPI armLGun = null;
        WeaponAPI decoArmL = null;
        WeaponAPI pauldronL = null;
        WeaponAPI pauldronR = null;
        for (WeaponAPI w : host.getAllWeapons()) {
            WeaponSlotAPI slot = w.getSlot();
            if (slot == null) continue;
            String id = slot.getId();
            if (SLOT_ARM_R.equals(id)) armR = w;
            else if (SLOT_ARM_L.equals(id)) armLGun = w;
            else if (SLOT_DECO_ARM_L.equals(id)) decoArmL = w;
            else if (SLOT_PAULDRON_L.equals(id)) pauldronL = w;
            else if (SLOT_PAULDRON_R.equals(id)) pauldronR = w;
        }

        pauldronTrack = ArondightSettings.getFloat("arondight_part_track_factor", 0.5f);
        armTrack = ArondightSettings.getFloat("arondight_part_arm_track_factor", 0.75f);
        recoilPx = ArondightSettings.getFloat("arondight_part_recoil_px", 2f);
        recoilDecay = ArondightSettings.getFloat("arondight_part_recoil_decay", 0.18f);
        if (recoilDecay <= 0f) recoilDecay = 0.18f;

        right = buildSide(armR, armR, pauldronR);
        left = buildSide(armLGun, decoArmL, pauldronL);
        inert = right == null && left == null;
        return !inert;
    }

    private Side buildSide(WeaponAPI driver, WeaponAPI armPart, WeaponAPI pauldron) {
        if (driver == null) return null;
        Side side = new Side();
        side.driver = driver;
        side.driverIsBeam = driver.isBeam();
        side.recoil = recoilFor(driver, side.driverIsBeam);
        side.lastCooldown = driver.getCooldownRemaining();
        if (armPart != null) {
            side.armPart = armPart;
            SpriteAPI sprite = armPart.getSprite();
            if (sprite != null) side.armOrigin = sprite.getCenterY();
            if (armPart.getSlot() != null) side.armRest = armPart.getSlot().getAngle();
        }
        if (pauldron != null) {
            side.pauldron = pauldron;
            SpriteAPI sprite = pauldron.getSprite();
            if (sprite != null) side.pauldronOrigin = sprite.getCenterY();
            if (pauldron.getSlot() != null) side.pauldronRest = pauldron.getSlot().getAngle();
        }
        return side;
    }

    private float recoilFor(WeaponAPI driver, boolean beam) {
        if (beam) return recoilPx * BEAM_RECOIL_SHARE;
        float perShot = 0f;
        try {
            perShot = driver.getSpec().getDerivedStats().getDamagePerShot();
        } catch (Exception e) {
            perShot = 0f;
        }
        if (perShot <= 0f) return recoilPx;
        float scale = perShot / RECOIL_REFERENCE_DAMAGE;
        if (scale < RECOIL_SCALE_MIN) scale = RECOIL_SCALE_MIN;
        if (scale > RECOIL_SCALE_MAX) scale = RECOIL_SCALE_MAX;
        return recoilPx * scale;
    }
}
