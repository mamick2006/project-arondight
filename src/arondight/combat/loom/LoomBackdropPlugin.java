package arondight.combat.loom;

import java.util.EnumSet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class LoomBackdropPlugin extends BaseCombatLayeredRenderingPlugin {

	protected static final String TEXTURE = "graphics/arondight/backgrounds/loom_interior.jpg";
	protected static final float PARALLAX = 0.7f;

	private static boolean loadTried = false;
	private static boolean loadOk = false;

	protected SpriteAPI sprite = null;

	@Override
	public void init(CombatEntityAPI entity) {
		super.init(entity);
		if (!loadTried) {
			loadTried = true;
			try {
				Global.getSettings().loadTexture(TEXTURE);
				loadOk = true;
			} catch (Exception e) {
				Global.getLogger(LoomBackdropPlugin.class).warn(
						"Project Arondight: loom interior backdrop failed to load (" + TEXTURE
								+ "); the trial runs on the base background", e);
			}
		}
		if (loadOk) {
			sprite = Global.getSettings().getSprite(TEXTURE);
		}
	}

	@Override
	public EnumSet<CombatEngineLayers> getActiveLayers() {
		return EnumSet.of(CombatEngineLayers.BELOW_PLANETS);
	}

	@Override
	public float getRenderRadius() {
		return 1000000f;
	}

	@Override
	public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		if (sprite == null || layer != CombatEngineLayers.BELOW_PLANETS) return;
		float vx = viewport.getLLX() + viewport.getVisibleWidth() / 2f;
		float vy = viewport.getLLY() + viewport.getVisibleHeight() / 2f;
		float half = Math.max(
				(1f - PARALLAX) * LoomMazeGen.HALF_W + viewport.getVisibleWidth() / 2f,
				(1f - PARALLAX) * LoomMazeGen.HALF_H + viewport.getVisibleHeight() / 2f) + 100f;
		sprite.setSize(half * 2f, half * 2f);
		sprite.setAlphaMult(viewport.getAlphaMult());
		sprite.renderAtCenter(vx * PARALLAX, vy * PARALLAX);
	}
}
