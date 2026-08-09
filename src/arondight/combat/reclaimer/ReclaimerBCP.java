package arondight.combat.reclaimer;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;

public class ReclaimerBCP implements BattleCreationPlugin {

	public static final float SIZE = 12000f;

	@Override
	public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
		CampaignFleetAPI playerFleet = context.getPlayerFleet();
		CampaignFleetAPI otherFleet = context.getOtherFleet();

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

		loader.initMap(-SIZE / 2f, SIZE / 2f, -SIZE / 2f, SIZE / 2f);
		loader.setBackgroundSpriteName("graphics/backgrounds/background4.jpg");
		loader.setBackgroundGlowColor(new Color(90, 120, 110, 50));
		loader.setHyperspaceMode(false);

		context.setInitialDeploymentBurnDuration(1f);
		context.setNormalDeploymentBurnDuration(4f);
		context.setStandoffRange(7000f);

		loader.addPlugin(new ReclaimerFightPlugin());
	}

	@Override
	public void afterDefinitionLoad(CombatEngineAPI engine) {
	}
}
