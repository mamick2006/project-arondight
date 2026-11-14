package arondight.form.mechanics;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import arondight.util.ArondightSettings;

public class MercyEdgeBeamEffect implements BeamEffectPlugin {

    private static final float SUSTAIN_HOLD = 0.1f;

    private String lastNote = null;
    private StasisField stasis;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;
        if (beam.getBrightness() < 0.1f) {
            note("mercy:brightness-low");
            return;
        }
        ShipAPI target = beam.getDamageTarget() instanceof ShipAPI
                ? (ShipAPI) beam.getDamageTarget()
                : endpointTarget(engine, beam);
        if (target == null) {
            note("mercy:no-ship-target");
            return;
        }
        ShipAPI source = beam.getSource();
        if (source == null || target.getOwner() == source.getOwner()) {
            note("mercy:same-owner-or-no-source");
            return;
        }
        if (target.isFighter()) {
            note("mercy:fighter-target");
            return;
        }
        if (target.getShield() != null && target.getShield().isOn()
                && target.getShield().isWithinArc(beam.getRayEndPrevFrame())) {
            note("mercy:shield-hit");
            return;
        }
        note("mercy:on-target");

        boolean clarent = beam.getWeapon() != null
                && beam.getWeapon().getSpec().getWeaponId().contains("clarent");
        if (stasis == null) stasis = StasisField.get(engine);
        StasisField field = stasis;
        if (clarent) {
            field.applySustained(source, target,
                    ArondightSettings.getFloat("arondight_stasis_timemult", 0.01f),
                    true, SUSTAIN_HOLD);
        } else {
            field.applySustained(source, target,
                    ArondightSettings.getFloat("arondight_curtana_slow_mult", 0.5f),
                    false, SUSTAIN_HOLD);
        }
    }

    private ShipAPI endpointTarget(CombatEngineAPI engine, BeamAPI beam) {
        org.lwjgl.util.vector.Vector2f to = beam.getTo();
        if (to == null) return null;
        for (ShipAPI other : engine.getShips()) {
            if (!other.isAlive() || other.isHulk() || other == beam.getSource()) continue;
            float r = other.getCollisionRadius() + 15f;
            float dx = other.getLocation().x - to.x, dy = other.getLocation().y - to.y;
            if (dx * dx + dy * dy <= r * r) {
                note("mercy:endpoint-fallback");
                return other;
            }
        }
        return null;
    }

    private void note(String outcome) {
        if (outcome.equals(lastNote)) return;
        lastNote = outcome;
        StasisField.noteForTest(outcome);
    }
}
