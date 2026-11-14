package arondight.form.mechanics;

import java.awt.Color;
import java.util.EnumSet;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import arondight.util.ArondightSettings;

final class VernierFlares implements CombatLayeredRenderingPlugin {

    private static final String GLOW_TEXTURE = "graphics/fx/particlealpha32sq.png";

    private static final EnumSet<CombatEngineLayers> LAYERS =
            EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);

    private static final float DARK = 0.02f;

    private static final float RENDER_RADIUS = 140f;

    private static final float RISE = 20f;
    private static final float FALL = 8f;

    private static final float FLICKER = 0.14f;

    private static final float HALO_SIZE = 2.2f;
    private static final float HALO_ALPHA = 0.5f;
    private static final float MID_SIZE = 0.62f;
    private static final float CORE_SIZE = 0.38f;
    private static final float ANCHOR = 0.28f;

    private static final class Nozzle {
        WeaponAPI blade;
        float slotX;
        float slotY;
        float originX;
        float originY;
        float out;
        float back;
        float sign;
        float level;
    }

    private final ShipAPI ship;
    private final SpriteAPI glow;
    private final Color plumeColor;
    private final Color coreColor;
    private final float length;
    private final float width;
    private final float cant;

    private Nozzle left;
    private Nozzle right;
    private CombatEntityAPI entity;
    private boolean gone;
    private int noise = 0x2545F491;
    private float flicker = 1f;

    VernierFlares(ShipAPI ship, Color engineColor) {
        this.ship = ship;
        this.glow = Global.getSettings().getSprite(GLOW_TEXTURE);
        this.glow.setAdditiveBlend();
        this.plumeColor = new Color(engineColor.getRed(), engineColor.getGreen(),
                engineColor.getBlue(), 255);
        this.coreColor = new Color(
                (engineColor.getRed() + 2 * 255) / 3,
                (engineColor.getGreen() + 2 * 255) / 3,
                (engineColor.getBlue() + 2 * 255) / 3, 255);
        this.length = ArondightSettings.getFloat("arondight_motion_vernier_length", 18f);
        this.width = ArondightSettings.getFloat("arondight_motion_vernier_width", 8f);
        this.cant = ArondightSettings.getFloat("arondight_motion_vernier_cant_deg", 25f);
    }

    void addBlade(WeaponAPI blade, float sign, float originX, float originY,
            float out, float back) {
        if (blade == null || blade.getSlot() == null) return;
        Vector2f at = blade.getSlot().getLocation();
        if (at == null) return;
        Nozzle n = new Nozzle();
        n.blade = blade;
        n.slotX = at.x;
        n.slotY = at.y;
        n.originX = originX;
        n.originY = originY;
        n.out = out;
        n.back = back;
        n.sign = sign;
        if (sign > 0f) left = n;
        else right = n;
    }

    boolean hasBlades() {
        return left != null || right != null;
    }

    boolean isGone() {
        return gone;
    }

    void follow(float wantLeft, float wantRight, float amount) {
        if (left != null) left.level = toward(left.level, wantLeft, amount);
        if (right != null) right.level = toward(right.level, wantRight, amount);
    }

    void cut() {
        if (left != null) left.level = 0f;
        if (right != null) right.level = 0f;
    }

    private static float toward(float level, float want, float amount) {
        if (want > level) {
            level += RISE * amount;
            return level > want ? want : level;
        }
        level -= FALL * amount;
        return level < want ? want : level;
    }

    @Override
    public void init(CombatEntityAPI entity) {
        this.entity = entity;
    }

    @Override
    public void cleanup() {
        gone = true;
    }

    @Override
    public boolean isExpired() {
        return ship.isHulk() || ship.wasRemoved();
    }

    @Override
    public void advance(float amount) {
        if (entity != null) entity.getLocation().set(ship.getLocation());
        noise = noise * 1664525 + 1013904223;
        flicker = 1f - FLICKER * ((noise >>> 16) & 0xFF) / 255f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    @Override
    public float getRenderRadius() {
        return RENDER_RADIUS;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        boolean leftLit = left != null && left.level > DARK;
        boolean rightLit = right != null && right.level > DARK;
        if (!leftLit && !rightLit) return;

        float alpha = ship.getCombinedAlphaMult() * viewport.getAlphaMult();
        if (alpha <= 0f) return;
        double facing = Math.toRadians(ship.getFacing());
        float cosF = (float) Math.cos(facing);
        float sinF = (float) Math.sin(facing);
        Vector2f at = ship.getLocation();
        if (leftLit) draw(left, at.x, at.y, cosF, sinF, alpha);
        if (rightLit) draw(right, at.x, at.y, cosF, sinF, alpha);
    }

    private void draw(Nozzle n, float shipX, float shipY, float cosF, float sinF, float alpha) {
        float angle = n.blade.getCurrAngle();
        double rad = Math.toRadians(angle);
        float cosA = (float) Math.cos(rad);
        float sinA = (float) Math.sin(rad);

        SpriteAPI sprite = n.blade.getSprite();
        float qx = -n.sign * n.out;
        float qy = -n.back;
        if (sprite != null) {
            qx -= sprite.getCenterX() - n.originX;
            qy -= sprite.getCenterY() - n.originY;
        }
        float x = shipX + n.slotX * cosF - n.slotY * sinF + qy * cosA + qx * sinA;
        float y = shipY + n.slotX * sinF + n.slotY * cosF + qy * sinA - qx * cosA;

        float dir = angle + 180f - n.sign * cant;
        float level = n.level * flicker;
        float len = length * (0.35f + 0.65f * level);
        float fade = n.level * 1.5f;
        if (fade > 1f) fade = 1f;

        float halo = width * HALO_SIZE;
        glow.setAngle(0f);
        glow.setSize(halo, halo);
        glow.setCenter(halo * 0.5f, halo * 0.5f);
        glow.setColor(plumeColor);
        glow.setAlphaMult(alpha * fade * HALO_ALPHA);
        glow.renderAtCenter(x, y);

        glow.setAngle(dir);
        stretched(x, y, len, width, plumeColor, alpha * fade);
        stretched(x, y, len * MID_SIZE, width * MID_SIZE, plumeColor, alpha * fade);
        stretched(x, y, len * CORE_SIZE, width * CORE_SIZE, coreColor, alpha * level);
    }

    private void stretched(float x, float y, float len, float wide, Color color, float alpha) {
        glow.setSize(len, wide);
        glow.setCenter(len * ANCHOR, wide * 0.5f);
        glow.setColor(color);
        glow.setAlphaMult(alpha);
        glow.renderAtCenter(x, y);
    }
}
