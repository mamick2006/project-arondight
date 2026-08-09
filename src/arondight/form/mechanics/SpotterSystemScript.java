package arondight.form.mechanics;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.util.ArondightSettings;

public class SpotterSystemScript extends BaseShipSystemScript {

    private static final Object KEY_SHIP = new Object();
    private static final Object KEY_TARGET = new Object();

    private static final Color TEXT_COLOR = new Color(255, 190, 80, 255);
    private static final Color JITTER_COLOR = new Color(255, 190, 80, 75);

    public static final java.util.LinkedList<String> spotterOutcomesForTest = new java.util.LinkedList<>();

    private static void noteSpotter(String outcome) {
        spotterOutcomesForTest.addLast(outcome);
        while (spotterOutcomesForTest.size() > 16) spotterOutcomesForTest.removeFirst();
    }

    private float damMult = -1f;
    private float range = -1f;

    private ShipAPI marked;
    private MarkPlugin markPlugin;
    private float currDamMult = 1f;
    private float elapsedAfterInState = 0f;
    private boolean captured;

    private float damMult() {
        if (damMult < 0f) {
            damMult = ArondightSettings.getFloat("arondight_spotter_damage_mult", 1.5f);
        }
        return damMult;
    }

    private float rangeSetting() {
        if (range < 0f) {
            range = ArondightSettings.getFloat("arondight_spotter_range", 4000f);
        }
        return range;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (state == State.IN && !captured) {
            captured = true;
            elapsedAfterInState = 0f;
            marked = findTarget(ship);
            if (marked != null) {
                noteSpotter("spotter:marked:" + marked.getHullSpec().getHullId());
                if (marked.getFluxTracker().showFloaty()
                        || ship == engine.getPlayerShip()
                        || marked == engine.getPlayerShip()) {
                    marked.getFluxTracker().showOverloadFloatyIfNeeded(
                            "Marked!", TEXT_COLOR, 4f, true);
                }
            } else {
                noteSpotter("spotter:no-target");
            }
        } else if (state == State.IDLE && captured) {
            captured = false;
            currDamMult = 1f;
            marked = null;
            markPlugin = null;
        }
        if (marked == null) return;

        if (!marked.isAlive() || marked.isHulk()) {
            marked = null;
            return;
        }

        currDamMult = 1f + (damMult() - 1f) * effectLevel;
        if (markPlugin == null) {
            markPlugin = new MarkPlugin(ship, marked, id + "_" + ship.getId());
            engine.addPlugin(markPlugin);
        }

        if (effectLevel > 0) {
            if (state != State.IN) {
                elapsedAfterInState += engine.getElapsedInLastFrame();
            }
            float shipJitterLevel;
            if (state == State.IN) {
                shipJitterLevel = effectLevel;
            } else {
                float durOut = 0.5f;
                shipJitterLevel = Math.max(0f, durOut - elapsedAfterInState) / durOut;
            }
            float jitterRangeBonus = shipJitterLevel * 50f;
            if (shipJitterLevel > 0) {
                ship.setJitter(KEY_SHIP, JITTER_COLOR, shipJitterLevel, 4, 0f, jitterRangeBonus);
            }
            marked.setJitter(KEY_TARGET, JITTER_COLOR, effectLevel, 3, 0f, 5f);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && effectLevel > 0 && marked != null) {
            return new StatusData(
                    (int) ((currDamMult - 1f) * 100f) + "% more damage to target", false);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isOutOfAmmo()) return null;
        if (system.getState() != SystemState.IDLE) return null;

        ShipAPI target = findTarget(ship);
        if (target != null && target != ship) {
            return "READY";
        }
        if (target == null && ship.getShipTarget() != null) {
            return "OUT OF RANGE";
        }
        return "NO TARGET";
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        ShipAPI target = findTarget(ship);
        return target != null && target != ship;
    }

    protected ShipAPI findTarget(ShipAPI ship) {
        float range = getMaxRange(ship);
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();

        if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.TARGET_FOR_SHIP_SYSTEM)) {
            target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.TARGET_FOR_SHIP_SYSTEM);
            if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
        }

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
        } else {
            if (target == null || target.getOwner() == ship.getOwner()) {
                if (player) {
                    target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(),
                            HullSize.FRIGATE, range, true);
                } else {
                    Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
                    if (test instanceof ShipAPI) {
                        target = (ShipAPI) test;
                        float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                        float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                        if (dist > range + radSum || target.isFighter()) target = null;
                        if (target != null && target.getOriginalOwner() == ship.getOriginalOwner()) target = null;
                    }
                }
            }
        }

        if (target != null && target.isFighter()) target = null;
        if (target == null) {
            target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(),
                    HullSize.FRIGATE, range, true);
        }

        return target;
    }

    public float getMaxRange(ShipAPI ship) {
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(rangeSetting());
    }

    private class MarkPlugin extends BaseEveryFrameCombatPlugin {
        private final ShipAPI source;
        private final ShipAPI target;
        private final String key;
        private boolean done;

        MarkPlugin(ShipAPI source, ShipAPI target, String key) {
            this.source = source;
            this.target = target;
            this.key = key;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (done) return;
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || engine.isPaused()) return;

            if (target == engine.getPlayerShip() && source.isAlive()) {
                engine.maintainStatusForPlayerShip(KEY_TARGET,
                        source.getSystem().getSpecAPI().getIconSpriteName(),
                        source.getSystem().getDisplayName(),
                        (int) ((currDamMult - 1f) * 100f) + "% more damage taken", true);
            }

            String end = null;
            if (!source.isAlive()) {
                end = "spotter:end:source-death";
            } else if (!target.isAlive() || target.isHulk()) {
                end = "spotter:end:target-death";
            } else if (currDamMult <= 1f) {
                end = "spotter:end:expiry";
            }

            if (end != null) {
                done = true;
                target.getMutableStats().getHullDamageTakenMult().unmodify(key);
                target.getMutableStats().getArmorDamageTakenMult().unmodify(key);
                target.getMutableStats().getShieldDamageTakenMult().unmodify(key);
                target.getMutableStats().getEmpDamageTakenMult().unmodify(key);
                noteSpotter(end);
                engine.removePlugin(this);
            } else {
                target.getMutableStats().getHullDamageTakenMult().modifyMult(key, currDamMult);
                target.getMutableStats().getArmorDamageTakenMult().modifyMult(key, currDamMult);
                target.getMutableStats().getShieldDamageTakenMult().modifyMult(key, currDamMult);
                target.getMutableStats().getEmpDamageTakenMult().modifyMult(key, currDamMult);
            }
        }
    }
}
