package arondight.combat.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class LoomBCP implements BattleCreationPlugin {

	protected static final String WALL_VARIANT = "arondight_wall_Hull";
	protected static final String WALL_FALLBACK = "module_armour3_Hull";

	public static final String SEED_KEY = "$arondight_loomSeed";

	protected MissionDefinitionAPI loader;
	protected LoomScenarioPlugin scenario;

	@Override
	public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
		this.loader = loader;
		CampaignFleetAPI playerFleet = context.getPlayerFleet();
		CampaignFleetAPI otherFleet = context.getOtherFleet();

		long settled = LoomLayout.generate(LoomLayout.resolveSeed());
		otherFleet.getMemoryWithoutUpdate().set(SEED_KEY, Long.toString(settled));
		Global.getLogger(LoomBCP.class).info("Project Arondight: Loom maze seed " + settled);

		LoomZoomLock.engage();

		context.aiRetreatAllowed = false;
		context.fightToTheLast = true;
		context.enemyDeployAll = true;

		int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");
		loader.initFleet(FleetSide.PLAYER, "", context.getPlayerGoal(), false,
				context.getPlayerCommandPoints() - baseCommandPoints,
				(int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
		loader.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true,
				(int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);

		for (FleetMemberAPI member : playerFleet.getFleetData().getCombatReadyMembersListCopy()) {
			loader.addFleetMember(FleetSide.PLAYER, member);
		}
		for (FleetMemberAPI member : otherFleet.getFleetData().getMembersListCopy()) {
			loader.addFleetMember(FleetSide.ENEMY, member);
		}

		loader.initMap(-LoomLayout.HALF_W, LoomLayout.HALF_W, -LoomLayout.HALF_H, LoomLayout.HALF_H);
		loader.setBackgroundSpriteName("graphics/backgrounds/background4.jpg");
		loader.setHyperspaceMode(false);

		addBoundaryNebula();

		Vector2f gate = LoomLayout.exitGate();
		loader.addObjective(gate.x, gate.y, "nav_buoy");

		context.setInitialDeploymentBurnDuration(1f);
		context.setNormalDeploymentBurnDuration(4f);
		context.setStandoffRange(9000f);

		scenario = new LoomScenarioPlugin();
		loader.addPlugin(scenario);
	}

	protected void addBoundaryNebula() {
		Random random = new Random(51L);
		for (float x = -LoomLayout.HALF_W; x <= LoomLayout.HALF_W; x += 340f) {
			loader.addNebula(x, LoomLayout.HALF_H - 120f, 280f + random.nextFloat() * 100f);
			loader.addNebula(x, -LoomLayout.HALF_H + 120f, 280f + random.nextFloat() * 100f);
		}
	}

	@Override
	public void afterDefinitionLoad(CombatEngineAPI engine) {
		engine.setBackgroundColor(new Color(10, 14, 12));
		engine.setRenderStarfield(false);
		engine.addLayeredRenderingPlugin(new LoomBackdropPlugin());

		List<ShipAPI> walls = new ArrayList<ShipAPI>();
		List<float[]> homes = new ArrayList<float[]>();
		for (float[] piece : LoomLayout.wallPieces()) {
			tilePiece(engine, piece, walls, homes);
		}
		if (scenario != null) {
			scenario.setWallAnchors(walls, homes);
		}
	}

	protected void tilePiece(CombatEngineAPI engine, float[] piece,
								List<ShipAPI> walls, List<float[]> homes) {
		boolean vertical = piece[0] != 0f;
		float fixed = piece[1];
		float from = piece[2];
		float to = piece[3];
		float span = to - from;
		int n = Math.max(1, (int) Math.ceil(span / LoomLayout.SEG_LEN - 0.02f));
		float step = n > 1 ? (span - LoomLayout.SEG_LEN) / (n - 1) : 0f;
		for (int i = 0; i < n; i++) {
			float along = n > 1
					? from + LoomLayout.SEG_LEN / 2f + step * i
					: (from + to) / 2f;
			float x = vertical ? fixed : along;
			float y = vertical ? along : fixed;
			float facing = (vertical ? 90f : 0f) + (i % 2 == 0 ? 0f : 180f);
			ShipAPI wall = trySpawn(engine, WALL_VARIANT, new Vector2f(x, y), facing);
			if (wall == null) {
				wall = trySpawn(engine, WALL_FALLBACK, new Vector2f(x, y), facing);
			}
			if (wall == null) continue;
			wall.setOwner(100);
			wall.setCollisionClass(CollisionClass.FIGHTER);
			wall.setStation(true);
			wall.setMass(500000f);
			wall.setInvalidTransferCommandTarget(true);
			wall.addListener(IMMORTAL);
			markLiveProp(wall);
			walls.add(wall);
			homes.add(new float[] {x, y, facing});
		}
	}

	protected void markLiveProp(ShipAPI wall) {
		wall.addTag("ship_being_reclaimed");
		wall.addTag("Abyssal_XO_NanoThief_HasReclamed");
		wall.setShipAI(null);
		wall.setShipSystemDisabled(true);
		wall.getMutableStats().getPeakCRDuration().modifyFlat("arondight_loom_wall", 100000f);
	}

	protected ShipAPI trySpawn(CombatEngineAPI engine, String specId, Vector2f at, float facing) {
		try {
			return engine.getFleetManager(1).spawnShipOrWing(specId, at, facing, 0f, null);
		} catch (Exception e) {
			return null;
		}
	}

	protected static final HullDamageAboutToBeTakenListener IMMORTAL =
			new HullDamageAboutToBeTakenListener() {
		@Override
		public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship,
													Vector2f point, float damageAmount) {
			return true;
		}
	};
}
