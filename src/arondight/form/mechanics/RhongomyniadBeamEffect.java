package arondight.form.mechanics;

import java.awt.Color;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;

import arondight.util.ArondightSettings;

public class RhongomyniadBeamEffect implements BeamEffectPlugin {

    private static final String RAMP_ID = "arondight_ramp";

    private static final float BASE_WIDTH = 18f;
    private static final float WIDTH_GAIN = 0.8f;
    private static final Color FRINGE_COOL = new Color(255, 220, 140, 235);
    private static final Color FRINGE_HOT = new Color(255, 245, 215, 255);

    private CombatEntityAPI lastTarget = null;
    private float timeOnTarget = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (engine.isPaused()) return;

        CombatEntityAPI target = beam.getDamageTarget();
        if (target == null || target != lastTarget) {
            lastTarget = target;
            timeOnTarget = 0f;
        } else if (beam.getBrightness() >= 0.9f) {
            timeOnTarget += amount;
        }

        float rampTime = ArondightSettings.getFloat("arondight_ramp_time", 6f);
        float maxMult = ArondightSettings.getFloat("arondight_ramp_max_mult", 4f);
        float frac = Math.min(timeOnTarget / rampTime, 1f);
        beam.getDamage().getModifier().modifyMult(RAMP_ID, 1f + frac * (maxMult - 1f));

        beam.setWidth(BASE_WIDTH * (1f + WIDTH_GAIN * frac));
        beam.setFringeColor(lerp(FRINGE_COOL, FRINGE_HOT, frac));
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                a.getRed() + (int) ((b.getRed() - a.getRed()) * t),
                a.getGreen() + (int) ((b.getGreen() - a.getGreen()) * t),
                a.getBlue() + (int) ((b.getBlue() - a.getBlue()) * t),
                a.getAlpha() + (int) ((b.getAlpha() - a.getAlpha()) * t));
    }
}
