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

public class ArondightKillTracker extends BaseEveryFrameCombatPlugin {

    private static final String ENGINE_KEY = "arondight_kills";
    private static final float ATTRIBUTION_WINDOW = 3f;

    public interface KillListener {
        void onKill(ShipAPI victim);
    }

    public static ArondightKillTracker get(CombatEngineAPI engine) {
        Object existing = engine.getCustomData().get(ENGINE_KEY);
        if (existing instanceof ArondightKillTracker) return (ArondightKillTracker) existing;
        ArondightKillTracker tracker = new ArondightKillTracker();
        engine.getCustomData().put(ENGINE_KEY, tracker);
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
