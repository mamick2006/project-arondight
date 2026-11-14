package arondight.form.mechanics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import arondight.balance.CombatRules;

public class ArondightKillTracker extends BaseEveryFrameCombatPlugin {

    private static final float ATTRIBUTION_WINDOW = 3f;

    public interface KillListener {
        void onKill(ShipAPI victim);
    }

    public static ArondightKillTracker get(CombatEngineAPI engine) {
        Object existing = engine.getPluginOfClass(ArondightKillTracker.class);
        if (existing instanceof ArondightKillTracker) return (ArondightKillTracker) existing;
        ArondightKillTracker tracker = new ArondightKillTracker();
        engine.addPlugin(tracker);
        return tracker;
    }

    private final Map<ShipAPI, Map<ShipAPI, Float>> recentDamage = new HashMap<>();
    private final Map<ShipAPI, KillListener> subscribers = new HashMap<>();
    private final IntervalUtil interval = new IntervalUtil(0.2f, 0.3f);

    public void subscribe(ShipAPI ship, KillListener listener) {
        subscribers.put(ship, listener);
    }

    public void noteDamage(ShipAPI attacker, ShipAPI victim) {
        if (attacker == null || victim == null || !subscribers.containsKey(attacker)) return;
        if (!victim.isAlive() || victim.isHulk()) return;
        if (!CombatRules.countsAsKill(victim.isFighter(), victim.isDrone())) return;
        Map<ShipAPI, Float> byAttacker = recentDamage.get(victim);
        if (byAttacker == null) {
            byAttacker = new HashMap<>();
            recentDamage.put(victim, byAttacker);
        }
        byAttacker.put(attacker, 0f);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()) return;
        float elapsed = interval.getElapsed();

        Iterator<Map.Entry<ShipAPI, Map<ShipAPI, Float>>> it = recentDamage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ShipAPI, Map<ShipAPI, Float>> entry = it.next();
            ShipAPI victim = entry.getKey();
            Map<ShipAPI, Float> byAttacker = entry.getValue();

            Iterator<Map.Entry<ShipAPI, Float>> at = byAttacker.entrySet().iterator();
            while (at.hasNext()) {
                Map.Entry<ShipAPI, Float> a = at.next();
                a.setValue(a.getValue() + elapsed);
                if (a.getValue() > ATTRIBUTION_WINDOW) at.remove();
            }

            boolean dead = !victim.isAlive() || victim.isHulk();
            if (dead) {
                for (ShipAPI attacker : byAttacker.keySet()) {
                    KillListener listener = subscribers.get(attacker);
                    if (listener != null && attacker.isAlive()) listener.onKill(victim);
                }
            }
            if (dead || byAttacker.isEmpty()) it.remove();
        }
    }
}
