package arondight.campaign.missions;

import java.awt.Color;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerActionContext;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.campaign.ArondightQuest;
import arondight.campaign.ArondightSoloBattle;

public class ArondightLoomMission extends HubMissionWithSearch {

	public static enum Stage {
		GO_TO_SYSTEM,
		DEFEAT_WARDENS,
		THE_LOOM,
		COMPLETED,
	}

	public static final String WARDENS_DEFEAT_TRIGGER = "ArondightWardensDefeated";
	public static final String WARDEN_FLEET_FLAG = "$arondight_wardenFleet";
	public static final String GLOBAL_REF = "$arondight_loom_ref";

	protected StarSystemAPI system;
	protected SectorEntityToken habitat;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference(GLOBAL_REF)) {
			return false;
		}

		requireSystemTags(ReqMode.ANY, Tags.THEME_DERELICT, Tags.THEME_MISC, Tags.THEME_RUINS);
		requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_UNSAFE, Tags.THEME_CORE_POPULATED);
		preferSystemUnexplored();
		if (createdAt != null) {
			requireSystemWithinRangeOf(createdAt.getLocationInHyperspace(), 18f);
		}
		system = pickSystem();
		if (system == null) return false;

		habitat = spawnEntity("arondight_habitat", new LocData(EntityLocationType.HIDDEN, null, system, false));
		if (habitat == null) return false;
		habitat.getMemoryWithoutUpdate().set("$arondight_habitat", true);
		habitat.setDiscoverable(true);

		setStartingStage(Stage.GO_TO_SYSTEM);
		addSuccessStages(Stage.COMPLETED);

		makeImportant(habitat, "$arondight_loom_imp", Stage.GO_TO_SYSTEM, Stage.DEFEAT_WARDENS, Stage.THE_LOOM);

		connectWithGlobalFlag(Stage.GO_TO_SYSTEM, Stage.DEFEAT_WARDENS, ArondightQuest.AT_LOOM_SYSTEM);
		connectWithGlobalFlag(Stage.DEFEAT_WARDENS, Stage.THE_LOOM, ArondightQuest.WARDENS_DEFEATED);
		connectWithGlobalFlag(Stage.THE_LOOM, Stage.COMPLETED, ArondightQuest.M1_DONE);

		beginEnteredLocationTrigger(system, Stage.GO_TO_SYSTEM);
		triggerSetGlobalMemoryValue(ArondightQuest.AT_LOOM_SYSTEM, true);
		triggerCreateFleet(FleetSize.MEDIUM, FleetQuality.HIGHER, Factions.DERELICT, FleetTypes.PATROL_MEDIUM, system);
		triggerMakeHostileAndAggressive();
		triggerMakeLowRepImpact();
		triggerPickLocationAroundEntity(habitat, 200f);
		triggerSpawnFleetAtPickedLocation(WARDEN_FLEET_FLAG, null);
		triggerCustomAction(new HoldAtEntityAction(habitat));
		triggerMakeFleetIgnoreOtherFleets();
		triggerFleetSetName("Quarantine Watch");
		triggerFleetMakeImportant("$arondight_warden_imp", Stage.DEFEAT_WARDENS);
		triggerFleetAddDefeatTrigger(WARDENS_DEFEAT_TRIGGER);
		endTrigger();

		changes.add(new SoloRestoreOnAbort());

		return true;
	}

	public static class SoloRestoreOnAbort implements Abortable {
		@Override
		public void abort(HubMission mission, boolean missionOver) {
			ArondightSoloBattle.restoreIfLeftOver();
		}
	}

	public static void holdWatchAt(CampaignFleetAPI fleet, SectorEntityToken station, HubMission mission) {
		if (fleet == null || station == null) return;
		fleet.clearAssignments();
		fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, 3650f);
		if (mission != null && !fleet.hasScriptOfClass(MissionFleetAutoDespawn.class)) {
			fleet.addScript(new MissionFleetAutoDespawn(mission, fleet));
		}
	}

	public static class HoldAtEntityAction implements TriggerAction {
		protected SectorEntityToken entity;

		public HoldAtEntityAction(SectorEntityToken entity) {
			this.entity = entity;
		}

		@Override
		public void doAction(TriggerActionContext context) {
			holdWatchAt(context.fleet, entity, context.mission);
		}
	}

	public void retrofitHoldWatch() {
		if (isEnded() || system == null || habitat == null) return;
		for (CampaignFleetAPI f : system.getFleets()) {
			if (f.getMemoryWithoutUpdate().getBoolean(WARDEN_FLEET_FLAG)) {
				holdWatchAt(f, habitat, this);
			}
		}
	}

	@Override
	protected void updateInteractionDataImpl() {
		set("$arondight_loom_systemName", system.getNameWithLowercaseTypeShort());
		set("$arondight_loom_dist", getDistanceLY(habitat));
	}

	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		if (currentStage == Stage.GO_TO_SYSTEM) {
			info.addPara(arondight.ArondightStrings.quest("quest_loom_desc_go",
					system.getNameWithLowercaseTypeShort()), opad);
		} else if (currentStage == Stage.DEFEAT_WARDENS) {
			info.addPara(arondight.ArondightStrings.quest("quest_loom_desc_wardens"), opad);
		} else if (currentStage == Stage.THE_LOOM) {
			info.addPara(arondight.ArondightStrings.quest("quest_loom_desc_dock"), opad);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		if (currentStage == Stage.GO_TO_SYSTEM) {
			info.addPara(getGoToSystemTextShort(system), tc, pad);
			return true;
		} else if (currentStage == Stage.DEFEAT_WARDENS) {
			info.addPara(arondight.ArondightStrings.quest("quest_loom_step_wardens"), tc, pad);
			return true;
		} else if (currentStage == Stage.THE_LOOM) {
			info.addPara(arondight.ArondightStrings.quest("quest_loom_step_dock"), tc, pad);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		return "The Loom";
	}
}
