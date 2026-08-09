package arondight.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.campaign.ArondightBattleResultScript;
import arondight.campaign.ArondightBattleResultScript.Kind;
import arondight.campaign.ArondightFIDPlugin;
import arondight.campaign.ArondightQuest;
import arondight.campaign.ArondightSoloBattle;

import static com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY;

public class ArondightCMD extends BaseCommandPlugin {

	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (params.isEmpty()) return false;
		String cmd = params.get(0).getString(memoryMap);

		switch (cmd) {
			case "startProving":
				return startProving(dialog);
			case "startDuel":
				return startDuel(dialog);
			default:
				return false;
		}
	}

	protected boolean startProving(InteractionDialogAPI dialog) {
		ArondightQuest.unset(ArondightQuest.LOOM_STARTED);
		ArondightQuest.unset(ArondightQuest.LOOM_WON);

		ArondightSoloBattle.beginWithTempFrame();

		CampaignFleetAPI loom = Global.getFactory().createEmptyFleet(Factions.DERELICT, "The Loom", true);
		loom.setNoFactionInName(true);
		addMember(loom, "arondight_wall_Hull");
		loom.getMemoryWithoutUpdate().set(ArondightQuest.LOOM_BATTLE_FLEET, true);

		launchScriptedBattle(dialog, loom, Kind.LOOM, ArondightStrings.quest("quest_opt_enter_course"));
		return true;
	}

	protected boolean startDuel(InteractionDialogAPI dialog) {
		FleetMemberAPI frame = ArondightSoloBattle.beginWithOwnedFrame();
		if (frame == null) {
			dialog.getTextPanel().addPara(ArondightStrings.quest("quest_duel_no_frame"), Misc.getNegativeHighlightColor());
			return false;
		}

		ArondightQuest.unset(ArondightQuest.DUEL_STARTED);
		ArondightQuest.unset(ArondightQuest.DUEL_WON);

		CampaignFleetAPI boss = Global.getFactory().createEmptyFleet("threat", "Reclaimer", true);
		boss.setNoFactionInName(true);
		FleetMemberAPI reclaimer = addMember(boss, ArondightQuest.reclaimerVariant());
		reclaimer.setShipName("Unnamed");
		reclaimer.setFlagship(true);
		reclaimer.getRepairTracker().setCR(1f);
		boss.getMemoryWithoutUpdate().set(ArondightQuest.DUEL_BATTLE_FLEET, true);

		launchScriptedBattle(dialog, boss, Kind.DUEL, ArondightStrings.quest("quest_opt_close_distance"));
		return true;
	}

	protected FleetMemberAPI addMember(CampaignFleetAPI fleet, String variantId) {
		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
		fleet.getFleetData().addFleetMember(member);
		member.setOwner(1);
		member.getRepairTracker().setCR(Math.max(member.getRepairTracker().getCR(), 0.7f));
		member.getRepairTracker().setCrashMothballed(false);
		member.getRepairTracker().setMothballed(false);
		return member;
	}

	protected void launchScriptedBattle(InteractionDialogAPI dialog, final CampaignFleetAPI enemy,
										Kind kind, String engageText) {
		enemy.getMemoryWithoutUpdate().set(MEMORY_KEY_NO_SHIP_RECOVERY, true);

		ArondightBattleResultScript script = new ArondightBattleResultScript(kind);
		Global.getSector().addScript(script);

		dialog.setInteractionTarget(enemy);

		final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
		config.leaveAlwaysAvailable = false;
		config.showCommLinkOption = false;
		config.showEngageText = false;
		config.showFleetAttitude = false;
		config.showTransponderStatus = false;
		config.showWarningDialogWhenNotHostile = false;
		config.alwaysAttackVsAttack = true;
		config.impactsAllyReputation = false;
		config.impactsEnemyReputation = false;
		config.pullInAllies = false;
		config.pullInEnemies = false;
		config.pullInStations = false;
		config.lootCredits = false;
		config.straightToEngage = true;
		config.firstTimeEngageOptionText = engageText;
		config.afterFirstTimeEngageOptionText = ArondightStrings.quest("quest_opt_reengage");
		config.noSalvageLeaveOptionText = ArondightStrings.quest("opt_continue");
		config.dismissOnLeave = false;
		config.printXPToDialog = true;

		config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
			@Override
			public void notifyLeave(InteractionDialogAPI dialog) {
				enemy.getMemoryWithoutUpdate().clear();
				enemy.clearAssignments();
				enemy.deflate();
				ArondightSoloBattle.restore();
				dialog.dismiss();
			}

			@Override
			public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
				bcc.aiRetreatAllowed = false;
				bcc.enemyDeployAll = true;
				bcc.fightToTheLast = true;
			}

			@Override
			public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
			}
		};

		ArondightFIDPlugin plugin = new ArondightFIDPlugin(config);
		dialog.setPlugin(plugin);
		plugin.init(dialog);
	}
}
