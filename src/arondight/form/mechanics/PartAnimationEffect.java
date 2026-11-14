package arondight.form.mechanics;

import java.awt.Color;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import arondight.util.ArondightSettings;

public class PartAnimationEffect implements EveryFrameWeaponEffectPlugin {

    private static final String SLOT_ARM_R = "WS_ARM_R";
    private static final String SLOT_ARM_L = "WS_ARM_L";
    private static final String SLOT_DECO_ARM_L = "WS_DECO_ARM_L";
    private static final String SLOT_BLADE_L = "WS_DECO_PL";
    private static final String SLOT_BLADE_R = "WS_DECO_PR";
    private static final String SLOT_HEAD = "WS_DECO_HEAD";

    private static final int RESOLVE_ATTEMPTS = 120;

    private static final float VIEW_MARGIN = 150f;

    private static final float MAX_STEP = 0.05f;

    private static final float BLADE_RECOIL_SHARE = 0.5f;

    private static final float BEAM_RECOIL_SHARE = 0.7f;

    private static final float RECOIL_REFERENCE_DAMAGE = 100f;
    private static final float RECOIL_SCALE_MIN = 0.8f;
    private static final float RECOIL_SCALE_MAX = 1.3f;

    private static final float FIRE_EPSILON = 0.02f;

    private static final float IDLE_FADE_SPEED = 0.33f;

    private static final float HEAD_EASE = 9f;

    private static final float HEAD_MIN_RANGE_SQ = 400f;

    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private boolean resolved;
    private boolean inert;
    private int attempts;
    private boolean offScreen;

    private ShipAPI ship;
    private ShipEngineControllerAPI engines;
    private Side left;
    private Side right;
    private WeaponAPI head;
    private float headRest;
    private VernierFlares flares;
    private Color engineColor;

    private float bladeTrack;
    private float armTrack;
    private float recoilPx;
    private float recoilDecay;
    private float joltPx;
    private float counterDeg;
    private float speedFold;
    private float thrustFold;
    private float brakeOpen;
    private float bankDeg;
    private float idleDeg;
    private float idleRate;
    private float stiffness;
    private float damping;
    private float shrugKick;
    private float headTurnRate;
    private float headLimit;
    private float headSweepDeg;
    private float headSweepRate;
    private float swell;
    private float vernierCruise;
    private float bladeRootFrac;

    private float clock;
    private float phase;
    private float lastTop;
    private float lastTurnTop;
    private boolean wrecked;

    private static final class Side {
        float sign;
        WeaponAPI driver;
        boolean driverIsBeam;
        boolean beamOn;
        float recoil;
        float scale;
        WeaponAPI armPart;
        boolean armHasBarrel;
        float armOriginX;
        float armOriginY;
        float armRest;
        WeaponAPI blade;
        float bladeOriginX;
        float bladeOriginY;
        float bladeRest;
        float bladeArcHalf = 180f;
        float bladeRoot;
        float lastCooldown;
        float kick;
        float jolt = 1f;
        float fold;
        float foldRate;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || inert) return;
        if (!resolved && !resolve(weapon, engine)) return;
        if (ship == null) return;

        if (ship.getOriginalOwner() == -1) {
            float facing = ship.getFacing();
            animate(right, facing, amount, false);
            animate(left, facing, amount, false);
            return;
        }

        ViewportAPI viewport = engine.getViewport();
        if (viewport != null && !viewport.isNearViewport(ship.getLocation(), VIEW_MARGIN)) {
            offScreen = true;
            return;
        }
        if (ship.isHulk()) {
            if (flares != null) flares.cut();
            if (!wrecked) {
                wrecked = true;
                dropKicks();
                float facing = ship.getFacing();
                animate(right, facing, 0f, true);
                animate(left, facing, 0f, true);
            }
            return;
        }
        wrecked = false;
        boolean returning = offScreen;
        offScreen = false;
        if (returning) dropKicks();
        if (amount > MAX_STEP) amount = MAX_STEP;
        clock += amount;

        float facing = ship.getFacing();
        fly(amount, facing, engine);
        animate(right, facing, amount, returning);
        animate(left, facing, amount, returning);
        turnHead(amount, facing, engine);
    }

    private void fly(float amount, float facing, CombatEngineAPI engine) {
        double rad = facing * DEG_TO_RAD;
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        Vector2f velocity = ship.getVelocity();
        float top;
        float turnTop;
        boolean out = engines.isFlamedOut();
        if (out && lastTop > 0f) {
            top = lastTop;
            turnTop = lastTurnTop;
        } else {
            top = ship.getMaxSpeed();
            if (top < 1f) top = 1f;
            turnTop = ship.getMaxTurnRate();
            if (turnTop < 1f) turnTop = 1f;
            if (!out) {
                lastTop = top;
                lastTurnTop = turnTop;
            }
        }
        float forward = (velocity.x * cos + velocity.y * sin) / top;
        if (forward > 1f) forward = 1f;
        else if (forward < -1f) forward = -1f;
        float lateral = (velocity.y * cos - velocity.x * sin) / top;

        float turn = ship.getAngularVelocity() / turnTop;
        if (turn > 1f) turn = 1f;
        else if (turn < -1f) turn = -1f;

        boolean dead = engines.isFlamedOut();
        boolean thrust = !dead && engines.isAccelerating();
        boolean brake = !dead && (engines.isDecelerating() || engines.isAcceleratingBackwards());

        float fold = forward > 0f ? forward * speedFold : forward * brakeOpen;
        if (thrust) fold += thrustFold;
        if (brake) fold -= brakeOpen;

        float speed = forward < 0f ? -forward : forward;
        speed += lateral < 0f ? -lateral : lateral;
        if (speed < IDLE_FADE_SPEED) {
            fold += idleDeg * (1f - speed / IDLE_FADE_SPEED)
                    * (float) Math.sin(clock * idleRate + phase);
        }

        float bank = turn * bankDeg;
        if (left != null) spring(left, fold + bank, amount);
        if (right != null) spring(right, fold - bank, amount);

        if (thrust && swell > 0f) {
            engines.extendFlame(this, swell, swell * 0.33f, swell * 0.66f);
        }

        if (flares != null) {
            if (flares.isGone()) {
                flares = null;
                lightFlares(engine);
                if (flares == null) return;
            }
            float wantLeft = 0f;
            float wantRight = 0f;
            if (!dead) {
                if (engines.isTurningLeft() || engines.isStrafingLeft()) wantRight = 1f;
                if (engines.isTurningRight() || engines.isStrafingRight()) wantLeft = 1f;
                if (thrust) {
                    if (wantLeft < vernierCruise) wantLeft = vernierCruise;
                    if (wantRight < vernierCruise) wantRight = vernierCruise;
                }
            }
            flares.follow(wantLeft, wantRight, amount);
        }
    }

    private void dropKicks() {
        if (right != null) right.kick = 0f;
        if (left != null) left.kick = 0f;
    }

    private void spring(Side side, float want, float amount) {
        float pull = stiffness * (want - side.fold) - damping * side.foldRate;
        side.foldRate += pull * amount;
        side.fold += side.foldRate * amount;
    }

    private void animate(Side side, float facing, float amount, boolean returning) {
        if (side == null) return;

        side.kick = ArmKick.decay(side.kick, amount, recoilDecay);
        float lean = 0f;
        float hold = 0f;
        if (side.driver != null) {
            if (side.driverIsBeam) {
                hold = side.driver.getChargeLevel();
                if (ArmKick.beamLights(hold, side.beamOn, returning)) kick(side);
                side.beamOn = hold > 0f;
            } else {
                float cooldown = side.driver.getCooldownRemaining();
                if (!returning && cooldown > side.lastCooldown + FIRE_EPSILON) kick(side);
                side.lastCooldown = cooldown;
            }
            lean = shortestRotation(facing, side.driver.getCurrAngle());
        }

        float shove = ArmKick.shove(side.kick);
        float bladeShove = hold > shove ? hold : shove;

        if (side.armPart != null) {
            if (side.armPart != side.driver) {
                side.armPart.setCurrAngle(facing + side.armRest + lean * armTrack
                        + side.sign * shove * counterDeg * side.scale);
            }
            SpriteAPI sprite = side.armHasBarrel ? null : side.armPart.getSprite();
            if (sprite != null) {
                sprite.setCenter(
                        side.armOriginX + side.jolt * shove * joltPx * side.scale,
                        side.armOriginY + shove * side.recoil);
            }
        }
        if (side.blade != null) {
            float sway = lean * bladeTrack;
            float off = sway + side.sign * side.fold;
            if (off > side.bladeArcHalf) off = side.bladeArcHalf;
            else if (off < -side.bladeArcHalf) off = -side.bladeArcHalf;
            float shown = side.sign * (off - sway);
            side.blade.setCurrAngle(facing + side.bladeRest + off);
            SpriteAPI sprite = side.blade.getSprite();
            if (sprite != null) {
                float f = shown * DEG_TO_RAD;
                float f2 = f * f;
                float sinF = f - f * f2 / 6f;
                float versF = f2 / 2f - f2 * f2 / 24f;
                sprite.setCenter(
                        side.bladeOriginX + side.sign * side.bladeRoot * versF,
                        side.bladeOriginY + side.bladeRoot * sinF
                                + bladeShove * side.recoil * BLADE_RECOIL_SHARE);
            }
        }
    }

    private void kick(Side side) {
        float spent = 1f - side.kick;
        if (side.kick < 0.5f) side.jolt = -side.jolt;
        side.kick = 1f;
        side.foldRate += shrugKick * side.scale * spent;
    }

    private void turnHead(float amount, float facing, CombatEngineAPI engine) {
        if (head == null) return;
        Vector2f at = null;
        ShipAPI target = ship.getShipTarget();
        if (target != null) {
            at = target.getLocation();
        } else if (ship == engine.getPlayerShip() && engine.isUIAutopilotOn()) {
            at = ship.getMouseTarget();
        }

        float want;
        if (at != null) {
            Vector2f from = ship.getLocation();
            float dx = at.x - from.x;
            float dy = at.y - from.y;
            if (dx * dx + dy * dy < HEAD_MIN_RANGE_SQ) {
                want = 0f;
            } else {
                want = shortestRotation(facing + headRest,
                        (float) Math.toDegrees(Math.atan2(dy, dx)));
            }
        } else {
            want = headSweepDeg * (float) Math.sin(clock * headSweepRate + phase * 1.7f);
        }
        if (want > headLimit) want = headLimit;
        else if (want < -headLimit) want = -headLimit;

        float now = shortestRotation(facing + headRest, head.getCurrAngle());
        float ease = amount * HEAD_EASE;
        if (ease > 1f) ease = 1f;
        float step = (want - now) * ease;
        float most = headTurnRate * amount;
        if (step > most) step = most;
        else if (step < -most) step = -most;
        head.setCurrAngle(facing + headRest + now + step);
    }

    private static float shortestRotation(float from, float to) {
        float d = (to - from) % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private boolean resolve(WeaponAPI self, CombatEngineAPI engine) {
        attempts++;
        ShipAPI host = self.getShip();
        if (host == null || host.getAllWeapons() == null || host.getAllWeapons().isEmpty()) {
            if (attempts >= RESOLVE_ATTEMPTS) {
                resolved = true;
                inert = true;
                Global.getLogger(PartAnimationEffect.class).warn(
                        "Project Arondight: mech part animation found no weapons on its host"
                                + " after " + attempts + " frames; the arms, blades and head"
                                + " stay static this battle");
            }
            return false;
        }
        resolved = true;
        ship = host;
        engines = host.getEngineController();

        WeaponAPI armR = null;
        WeaponAPI armLGun = null;
        WeaponAPI decoArmL = null;
        WeaponAPI bladeL = null;
        WeaponAPI bladeR = null;
        for (WeaponAPI w : host.getAllWeapons()) {
            WeaponSlotAPI slot = w.getSlot();
            if (slot == null) continue;
            String id = slot.getId();
            if (SLOT_ARM_R.equals(id)) armR = w;
            else if (SLOT_ARM_L.equals(id)) armLGun = w;
            else if (SLOT_DECO_ARM_L.equals(id)) decoArmL = w;
            else if (SLOT_BLADE_L.equals(id)) bladeL = w;
            else if (SLOT_BLADE_R.equals(id)) bladeR = w;
            else if (SLOT_HEAD.equals(id)) head = w;
        }
        if (head != null && head.getSlot() != null) headRest = head.getSlot().getAngle();

        readTunables();
        right = buildSide(-1f, armR, armR, bladeR);
        left = buildSide(1f, armLGun, decoArmL, bladeL);
        inert = right == null && left == null && head == null;
        if (inert || engines == null) {
            inert = true;
            return false;
        }

        String id = host.getId();
        phase = id == null ? 0f : (id.hashCode() & 0x3FF) / 1024f * TWO_PI;

        if (host.getOriginalOwner() != -1) {
            List<ShipEngineAPI> list = engines.getShipEngines();
            engineColor = list == null || list.isEmpty() ? null : list.get(0).getEngineColor();
            lightFlares(engine);
        }
        return true;
    }

    private void readTunables() {
        bladeTrack = ArondightSettings.getFloat("arondight_part_track_factor", 0.5f);
        armTrack = ArondightSettings.getFloat("arondight_part_arm_track_factor", 0.75f);
        recoilPx = ArondightSettings.getFloat("arondight_part_recoil_px", 6f);
        recoilDecay = ArondightSettings.getFloat("arondight_part_recoil_decay", 0.22f);
        if (recoilDecay <= 0f) recoilDecay = 0.22f;
        joltPx = ArondightSettings.getFloat("arondight_motion_recoil_jolt_px", 1.5f);
        counterDeg = ArondightSettings.getFloat("arondight_motion_arm_counter_deg", 5f);

        speedFold = ArondightSettings.getFloat("arondight_motion_blade_speed_deg", 9f);
        thrustFold = ArondightSettings.getFloat("arondight_motion_blade_thrust_deg", 6f);
        brakeOpen = ArondightSettings.getFloat("arondight_motion_blade_brake_deg", 9f);
        bankDeg = ArondightSettings.getFloat("arondight_motion_blade_bank_deg", 7f);
        idleDeg = ArondightSettings.getFloat("arondight_motion_blade_idle_deg", 3f);
        float idlePeriod = ArondightSettings.getFloat("arondight_motion_blade_idle_period", 3.5f);
        idleRate = TWO_PI / (idlePeriod > 0f ? idlePeriod : 3.5f);

        float response = ArondightSettings.getFloat("arondight_motion_blade_response", 14f);
        if (response <= 0f) response = 14f;
        float settle = ArondightSettings.getFloat("arondight_motion_blade_damping", 0.55f);
        if (settle < 0.2f) settle = 0.2f;
        if (settle > 1f) settle = 1f;
        stiffness = response * response;
        damping = 2f * settle * response;
        shrugKick = ArondightSettings.getFloat("arondight_motion_shrug_deg", 6f)
                * kickForPeak(response, settle);

        headTurnRate = ArondightSettings.getFloat("arondight_motion_head_turn_rate", 160f);
        headLimit = ArondightSettings.getFloat("arondight_motion_head_limit_deg", 60f);
        headSweepDeg = ArondightSettings.getFloat("arondight_motion_head_sweep_deg", 25f);
        float sweepPeriod = ArondightSettings.getFloat("arondight_motion_head_sweep_period", 6f);
        headSweepRate = TWO_PI / (sweepPeriod > 0f ? sweepPeriod : 6f);

        swell = ArondightSettings.getFloat("arondight_motion_thruster_swell", 0.75f);
        vernierCruise = ArondightSettings.getFloat("arondight_motion_vernier_cruise", 0.45f);
        bladeRootFrac = ArondightSettings.getFloat("arondight_motion_blade_root_frac", 0.75f);
    }

    private static float kickForPeak(float response, float settle) {
        if (settle >= 0.99f) return response * (float) Math.E;
        double ringing = response * Math.sqrt(1.0 - settle * settle);
        double peakAt = Math.atan2(ringing, settle * response) / ringing;
        double peak = Math.exp(-settle * response * peakAt) * Math.sin(ringing * peakAt) / ringing;
        return (float) (1.0 / peak);
    }

    private void lightFlares(CombatEngineAPI engine) {
        if (engine == null || engineColor == null) return;
        try {
            VernierFlares made = new VernierFlares(ship, engineColor);
            addNozzle(made, left);
            addNozzle(made, right);
            if (!made.hasBlades()) return;
            engine.addLayeredRenderingPlugin(made);
            flares = made;
        } catch (RuntimeException e) {
            engineColor = null;
            Global.getLogger(PartAnimationEffect.class).warn(
                    "Project Arondight: the blade vernier flares could not be set up ("
                            + e + "); the frame flies without them this battle");
        }
    }

    private void addNozzle(VernierFlares made, Side side) {
        if (side == null || side.blade == null || side.blade.getSprite() == null) return;
        List<Vector2f> points = side.blade.getSpec().getTurretFireOffsets();
        if (points == null || points.isEmpty()) return;
        Vector2f nozzle = points.get(0);
        made.addBlade(side.blade, side.sign, side.bladeOriginX, side.bladeOriginY,
                side.sign * nozzle.y, -nozzle.x);
    }

    private Side buildSide(float sign, WeaponAPI driver, WeaponAPI armPart, WeaponAPI blade) {
        if (driver == null && armPart == null && blade == null) return null;
        Side side = new Side();
        side.sign = sign;
        side.scale = 1f;
        if (driver != null) {
            side.driver = driver;
            side.driverIsBeam = driver.isBeam();
            side.scale = recoilScale(driver, side.driverIsBeam);
            side.recoil = recoilPx * side.scale;
            side.lastCooldown = driver.getCooldownRemaining();
        }
        if (armPart != null) {
            side.armPart = armPart;
            side.armHasBarrel = armPart.getBarrelSpriteAPI() != null;
            SpriteAPI sprite = armPart.getSprite();
            if (sprite != null) {
                side.armOriginX = sprite.getCenterX();
                side.armOriginY = sprite.getCenterY();
            }
            if (armPart.getSlot() != null) side.armRest = armPart.getSlot().getAngle();
        }
        if (blade != null) {
            side.blade = blade;
            SpriteAPI sprite = blade.getSprite();
            if (sprite != null) {
                side.bladeOriginX = sprite.getCenterX();
                side.bladeOriginY = sprite.getCenterY();
                side.bladeRoot = sprite.getWidth() * 0.5f * bladeRootFrac;
            }
            if (blade.getSlot() != null) {
                side.bladeRest = blade.getSlot().getAngle();
                side.bladeArcHalf = blade.getSlot().getArc() * 0.5f;
            }
        }
        return side;
    }

    private float recoilScale(WeaponAPI driver, boolean beam) {
        if (beam) return BEAM_RECOIL_SHARE;
        float perShot = 0f;
        try {
            perShot = driver.getSpec().getDerivedStats().getDamagePerShot();
        } catch (Exception e) {
            perShot = 0f;
        }
        if (perShot <= 0f) return 1f;
        float scale = perShot / RECOIL_REFERENCE_DAMAGE;
        if (scale < RECOIL_SCALE_MIN) scale = RECOIL_SCALE_MIN;
        if (scale > RECOIL_SCALE_MAX) scale = RECOIL_SCALE_MAX;
        return scale;
    }
}
