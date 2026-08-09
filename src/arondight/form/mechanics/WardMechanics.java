package arondight.form.mechanics;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;

import arondight.util.ArondightSettings;

public class WardMechanics implements AdvanceableListener, DamageTakenModifier {

    public enum Variant { DURENDAL, FLASH, BULWARK }

    private static final Color GLOW_READY = new Color(160, 210, 255, 140);
    private static final Color ENGINE_READY = new Color(150, 220, 255, 255);
    private static final Color ACTIVE_FLARE = new Color(235, 250, 255, 220);
    private static final Color ACTIVE_UNDER = new Color(215, 240, 255, 200);
    private static final Color ARC_FRINGE = new Color(120, 180, 255, 255);
    private static final Color ARC_CORE = new Color(230, 245, 255, 255);

    public static final java.util.LinkedList<String> wardOutcomesForTest = new java.util.LinkedList<>();

    private static void noteWard(String outcome) {
        wardOutcomesForTest.addLast(outcome);
        while (wardOutcomesForTest.size() > 16) wardOutcomesForTest.removeFirst();
    }

    public static WardMechanics of(ShipAPI ship) {
        List<WardMechanics> found = ship.getListeners(WardMechanics.class);
        return found.isEmpty() ? null : found.get(0);
    }

    private final ShipAPI ship;
    private final Variant variant;
    private final String modId;

    private boolean scriptActive = false;
    private boolean effective = false;
    private boolean inactiveHitNoted = false;
    private boolean engineTinted = false;
    private float activeTime = 0f;
    private boolean parryBanked = false;
    private Boolean quickCycle;
    private final Map<ShipAPI, float[]> reflectAccum = new HashMap<>();
    private float armorHealAccum = 0f;
    private float hullHealedForNote = 0f;
    private float lastHealFlush = 0f;

    public WardMechanics(ShipAPI ship, Variant variant) {
        this.ship = ship;
        this.variant = variant;
        this.modId = "arondight_ward_" + ship.getId();
        noteWard("ward:attached:" + variant);
    }

    public void updateSystemState(State state, float effectLevel) {
        scriptActive = state == State.IN || state == State.ACTIVE;
    }

    public boolean isWardActive() {
        return effective;
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) return;

        if (quickCycle == null) {
            quickCycle = arondight.weave.ArondightWeave.readWovenTags(ship.getHullSpec())
                    .contains(arondight.ArondightIds.NODE_QUICK_CYCLE);
        }

        boolean polled = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();
        boolean nowEffective = scriptActive || polled;
        if (nowEffective && !effective) {
            activeTime = 0f;
            reflectAccum.clear();
            inactiveHitNoted = false;
            noteWard("ward:up:" + (scriptActive ? "script" : "polled"));
        } else if (!nowEffective && effective) {
            noteWard("ward:down");
            if (variant == Variant.BULWARK) flushHeal(engine);
        }
        effective = nowEffective;

        if (effective) {
            activeTime += amount;
            WardSystemScript.cancelPhasing(ship);
            ship.setJitter(this, ACTIVE_FLARE, 1f, 4, 0f, 8f);
            ship.setJitterUnder(this, ACTIVE_UNDER, 1f, 25, 0f, 20f);
            if (variant == Variant.BULWARK && armorHealAccum > 0f
                    && engine.getTotalElapsedTime(false) - lastHealFlush > 0.5f) {
                flushHeal(engine);
            }
        } else if (variant == Variant.FLASH && parryBanked
                && ship.getPhaseCloak() != null
                && ship.getPhaseCloak().getCooldownRemaining() > 0f) {
            ship.getPhaseCloak().setCooldownRemaining(0f);
            parryBanked = false;
            noteWard("flash:bank:cashed");
        } else if (quickCycle && ship.getPhaseCloak() != null
                && ship.getPhaseCloak().getCooldownRemaining() > 0f) {
            ship.getPhaseCloak().setCooldownRemaining(
                    Math.max(0f, ship.getPhaseCloak().getCooldownRemaining() - amount / 3f));
        } else if (isReady()) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(engine.getTotalElapsedTime(false) * 4f);
            ship.setJitterUnder(this, GLOW_READY, pulse, 21, 0f, 12f);
            ship.getEngineController().fadeToOtherColor(this, ENGINE_READY, null, pulse, 0.7f);
            engineTinted = true;
        } else if (engineTinted) {
            ship.getEngineController().fadeToOtherColor(this, ENGINE_READY, null, 0f, 0.7f);
            engineTinted = false;
        }
    }

    private boolean isReady() {
        return ship.getPhaseCloak() != null && ship.getPhaseCloak().getCooldownRemaining() <= 0f;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                    Vector2f point, boolean shieldHit) {
        if (target != ship) return null;
        if (!effective) {
            if (!inactiveHitNoted) {
                inactiveHitNoted = true;
                noteWard("ward:inactive-hit");
            }
            return null;
        }

        float incoming = damage.getDamage();
        ShipAPI attacker = resolveAttacker(param);

        float reflectFraction = reflectFractionForHit();
        if (reflectFraction > 0 && attacker != null && attacker.isAlive()) {
            float[] accum = reflectAccum.get(attacker);
            if (accum == null) {
                accum = new float[2];
                reflectAccum.put(attacker, accum);
            }
            accum[0] += incoming * reflectFraction;
            float sinceLast = Global.getCombatEngine().getTotalElapsedTime(false) - accum[1];
            if (accum[0] > 0 && sinceLast > 0.5f) {
                boolean perfect = variant == Variant.FLASH
                        && activeTime <= ArondightSettings.getFloat("arondight_ward_perfect_window", 0.15f);
                spawnReflectArc(attacker, point, accum[0], perfect);
                noteWard("reflect:arc:" + (int) accum[0] + (perfect ? ":perfect" : ""));
                accum[0] = 0;
                accum[1] = Global.getCombatEngine().getTotalElapsedTime(false);
            }
        }

        if (variant == Variant.FLASH && !parryBanked
                && activeTime <= ArondightSettings.getFloat("arondight_ward_perfect_window", 0.15f)) {
            parryBanked = true;
            noteWard("flash:bank:armed");
        }

        if (variant == Variant.BULWARK && incoming > 0) {
            float hullHeal = ArondightSettings.getFloat("arondight_ward_heal_hull", 0.20f) * incoming;
            ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + hullHeal));
            hullHealedForNote += hullHeal;
            armorHealAccum += ArondightSettings.getFloat("arondight_ward_heal_armor", 0.05f) * incoming;
        }

        damage.getModifier().modifyMult(modId, 0f);
        noteWard("block:zeroed:" + (int) incoming);
        return modId;
    }

    private float reflectFractionForHit() {
        switch (variant) {
            case DURENDAL:
                return ArondightSettings.getFloat("arondight_ward_reflect", 0.5f);
            case FLASH:
                boolean perfect = activeTime <= ArondightSettings.getFloat("arondight_ward_perfect_window", 0.15f);
                return perfect
                        ? ArondightSettings.getFloat("arondight_ward_perfect_reflect", 2f)
                        : ArondightSettings.getFloat("arondight_ward_sloppy_reflect", 0.25f);
            case BULWARK:
            default:
                return 0f;
        }
    }

    private void spawnReflectArc(ShipAPI attacker, Vector2f from, float arcDamage, boolean perfect) {
        Global.getCombatEngine().spawnEmpArc(ship, from, ship, attacker,
                DamageType.ENERGY, arcDamage, arcDamage,
                ArondightSettings.getFloat("arondight_ward_arc_range", 1200f),
                "tachyon_lance_emp_impact",
                perfect ? 30f : 15f, ARC_FRINGE, ARC_CORE);
    }

    private static ShipAPI resolveAttacker(Object param) {
        if (param instanceof DamagingProjectileAPI) {
            return ((DamagingProjectileAPI) param).getSource();
        }
        if (param instanceof BeamAPI) {
            return ((BeamAPI) param).getSource();
        }
        if (param instanceof ShipAPI) {
            return (ShipAPI) param;
        }
        return null;
    }

    private void flushHeal(CombatEngineAPI engine) {
        lastHealFlush = engine.getTotalElapsedTime(false);
        float budget = armorHealAccum;
        armorHealAccum = 0f;
        float hullHealed = hullHealedForNote;
        hullHealedForNote = 0f;
        if (budget > 0f) {
            ArmorGridAPI grid = ship.getArmorGrid();
            if (grid != null) {
                float max = grid.getMaxArmorInCell();
                float[][] cells = grid.getGrid();
                int damaged = 0;
                for (int x = 0; x < cells.length; x++) {
                    for (int y = 0; y < cells[x].length; y++) {
                        if (cells[x][y] < max) damaged++;
                    }
                }
                if (damaged > 0) {
                    float perCell = budget / damaged;
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
            }
        }
        if (hullHealed > 0f || budget > 0f) {
            noteWard("bulwark:heal:" + (int) hullHealed + ":" + (int) budget);
        }
    }
}
