package arondight.form.mechanics;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class StasisField extends BaseEveryFrameCombatPlugin {

    private static final Color ICE = new Color(150, 220, 255, 255);
    private static final Color ICE_FAINT = new Color(150, 220, 255, 90);
    private static final Color SLOW_GLOW = new Color(150, 220, 255, 170);

    public static StasisField get(CombatEngineAPI engine) {
        Object existing = engine.getPluginOfClass(StasisField.class);
        if (existing instanceof StasisField) return (StasisField) existing;
        StasisField field = new StasisField();
        engine.addPlugin(field);
        return field;
    }

    private static final class Effect {
        final String statId;
        final ShipAPI source;
        final boolean freeze;
        final boolean sustained;
        final float mult;
        float remaining;

        Effect(String statId, ShipAPI source, boolean freeze, boolean sustained,
               float mult, float remaining) {
            this.statId = statId;
            this.source = source;
            this.freeze = freeze;
            this.sustained = sustained;
            this.mult = mult;
            this.remaining = remaining;
        }
    }

    public static volatile String lastApplyOutcomeForTest = null;
    public static final java.util.LinkedList<String> outcomesForTest = new java.util.LinkedList<>();

    public static void noteForTest(String outcome) {
        lastApplyOutcomeForTest = outcome;
        outcomesForTest.addLast(outcome);
        while (outcomesForTest.size() > 16) outcomesForTest.removeFirst();
    }

    private final Map<ShipAPI, Effect> effects = new HashMap<>();
    private final IntervalUtil interval = new IntervalUtil(0.08f, 0.12f);
    private int nextId = 0;

    public void applySustained(ShipAPI source, ShipAPI target, float mult,
                               boolean freeze, float hold) {
        if (target == null || !target.isAlive()) {
            noteForTest("sustain:refused-dead");
            return;
        }
        Effect existing = effects.get(target);
        if (existing != null) {
            if (existing.sustained && existing.source == source) {
                existing.remaining = hold;
            } else {
                noteForTest("sustain:refused-active");
            }
            return;
        }
        String statId = "arondight_sustain_" + nextId++;
        target.getMutableStats().getTimeMult().modifyMult(statId, mult);
        effects.put(target, new Effect(statId, source, freeze, true, mult, hold));
        noteForTest("sustain:applied:" + mult + ":o" + target.getOwner());
        flash(target, freeze ? 1f : 0.5f);
    }

    public void applySlow(ShipAPI target, float mult, float duration) {
        if (target == null || !target.isAlive()) {
            noteForTest("slow:refused-dead");
            return;
        }
        if (effects.containsKey(target)) {
            noteForTest("slow:refused-active");
            return;
        }
        String statId = "arondight_slow_" + nextId++;
        target.getMutableStats().getTimeMult().modifyMult(statId, mult);
        effects.put(target, new Effect(statId, null, false, false, mult, duration));
        noteForTest("slow:applied:" + mult + ":" + duration + ":o" + target.getOwner());
        flash(target, 0.5f);
    }

    public void applyHaste(ShipAPI target, float mult, float duration) {
        if (target == null || !target.isAlive()) {
            noteForTest("haste:refused-dead");
            return;
        }
        if (effects.containsKey(target)) {
            noteForTest("haste:refused-active");
            return;
        }
        String statId = "arondight_haste_" + nextId++;
        target.getMutableStats().getTimeMult().modifyMult(statId, mult);
        effects.put(target, new Effect(statId, null, false, false, mult, duration));
        noteForTest("haste:applied:" + mult + ":" + duration + ":o" + target.getOwner());
    }

    public int countFrozenBy(ShipAPI source) {
        int count = 0;
        for (Effect e : effects.values()) {
            if (e.freeze && e.source == source) count++;
        }
        return count;
    }

    public boolean isFrozen(ShipAPI target) {
        Effect e = effects.get(target);
        return e != null && e.freeze;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        for (Map.Entry<ShipAPI, Effect> entry : effects.entrySet()) {
            Effect e = entry.getValue();
            e.remaining -= amount;
            entry.getKey().getMutableStats().getTimeMult().modifyMult(e.statId, e.mult);
            if (e.freeze) {
                entry.getKey().setJitterUnder(this, ICE_FAINT, 1f, 12, 0f, 6f);
            } else if (e.sustained) {
                entry.getKey().setJitterUnder(this, SLOW_GLOW, 1f, 15, 0f, 8f);
            }
        }

        interval.advance(amount);
        if (!interval.intervalElapsed()) return;

        Iterator<Map.Entry<ShipAPI, Effect>> it = effects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ShipAPI, Effect> entry = it.next();
            ShipAPI target = entry.getKey();
            Effect e = entry.getValue();
            if (e.remaining <= 0 || !target.isAlive()) {
                target.getMutableStats().getTimeMult().unmodifyMult(e.statId);
                if (e.freeze && target.isAlive()) {
                    flash(target, 0.7f);
                }
                it.remove();
            }
        }
    }

    private void flash(ShipAPI target, float strength) {
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null) return;
        engine.addSmoothParticle(target.getLocation(), target.getVelocity(),
                target.getCollisionRadius() * 2.5f * strength, 1f, 0.4f, ICE);
    }
}
