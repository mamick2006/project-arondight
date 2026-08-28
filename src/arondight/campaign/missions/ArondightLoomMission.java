package arondight.campaign.missions;

import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerActionContext;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

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
	public static final String WARDEN_IMPORTANT_FLAG = "$arondight_warden_imp";
	public static final String WATCH_NAME = "Quarantine Watch";
	public static final String HABITAT_ENTITY = "arondight_habitat";
	public static final String HABITAT_FLAG = "$arondight_habitat";
	public static final String HABITAT_IMPORTANT_FLAG = "$arondight_loom_imp";

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

		habitat = spawnEntity(HABITAT_ENTITY, new LocData(EntityLocationType.HIDDEN, null, system, false));
		if (habitat == null) return false;
		dressHabitat();

		setStartingStage(Stage.GO_TO_SYSTEM);
		addSuccessStages(Stage.COMPLETED);

		connectWithGlobalFlag(Stage.GO_TO_SYSTEM, Stage.DEFEAT_WARDENS, ArondightQuest.AT_LOOM_SYSTEM);
		connectWithGlobalFlag(Stage.DEFEAT_WARDENS, Stage.THE_LOOM, ArondightQuest.WARDENS_DEFEATED);
		connectWithGlobalFlag(Stage.THE_LOOM, Stage.COMPLETED, ArondightQuest.M1_DONE);

		beginEnteredLocationTrigger(system, Stage.GO_TO_SYSTEM);
		triggerSetGlobalMemoryValue(ArondightQuest.AT_LOOM_SYSTEM, true);
		triggerCustomAction(new SpawnWatchAction(habitat));
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

	protected void dressHabitat() {
		habitat.getMemoryWithoutUpdate().set(HABITAT_FLAG, true);
		habitat.setDiscoverable(true);
		makeImportant(habitat, HABITAT_IMPORTANT_FLAG,
				Stage.GO_TO_SYSTEM, Stage.DEFEAT_WARDENS, Stage.THE_LOOM);
	}

	protected void ensureGenRandom() {
		if (genRandom == null) {
			setGenRandom(new Random());
		}
	}

	protected void ensureHabitat() {
		if (system == null || !ArondightQuest.gone(habitat)) return;
		SectorEntityToken fresh = spawnEntity(HABITAT_ENTITY,
				new LocData(EntityLocationType.HIDDEN, null, system, false));
		if (fresh == null) return;
		habitat = fresh;
		dressHabitat();
	}

	public static void holdWatchAt(CampaignFleetAPI fleet, SectorEntityToken station, HubMission mission) {
		if (fleet == null || station == null) return;
		fleet.clearAssignments();
		fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, 3650f);
		if (mission != null && !fleet.hasScriptOfClass(MissionFleetAutoDespawn.class)) {
			fleet.addScript(new MissionFleetAutoDespawn(mission, fleet));
		}
	}

	public static void hardenWatch(CampaignFleetAPI fleet) {
		if (fleet == null) return;
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
	}

	public static class SpawnWatchAction implements TriggerAction {
		protected SectorEntityToken habitat;

		public SpawnWatchAction(SectorEntityToken habitat) {
			this.habitat = habitat;
		}

		@Override
		public void doAction(TriggerActionContext context) {
			if (ArondightQuest.gone(habitat)) return;
			if (!(context.mission instanceof ArondightLoomMission)) return;
			ArondightLoomMission mission = (ArondightLoomMission) context.mission;
			mission.ensureGenRandom();

			CreateFleetAction create = new CreateFleetAction(FleetTypes.PATROL_MEDIUM,
					habitat.getLocationInHyperspace(), FleetSize.MEDIUM,
					FleetQuality.HIGHER, Factions.DERELICT);
			create.nameOverride = WATCH_NAME;
			create.doAction(context);
			CampaignFleetAPI fleet = context.fleet;
			if (fleet == null) return;

			String reason = mission.getReason();
			new SetFleetFlagsWithReasonAction(reason, false,
					MemFlags.MEMORY_KEY_MAKE_HOSTILE,
					MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE).doAction(context);
			new SetFleetFlagAction(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE_ONE_BATTLE_ONLY,
					false, (Object[]) null).doAction(context);
			new SetFleetFlagsWithReasonAction(reason, false,
					MemFlags.MEMORY_KEY_LOW_REP_IMPACT).doAction(context);

			habitat.getContainingLocation().addEntity(fleet);
			Vector2f at = Misc.getPointAtRadius(habitat.getLocation(), 200f);
			fleet.setLocation(at.x, at.y);
			new SetFleetFlagAction(WARDEN_FLEET_FLAG, false, (Object[]) null).doAction(context);

			holdWatchAt(fleet, habitat, mission);
			hardenWatch(fleet);
			new FleetMakeImportantAction(WARDEN_IMPORTANT_FLAG, Stage.DEFEAT_WARDENS)
					.doAction(context);
			new AddFleetDefeatTriggerAction(WARDENS_DEFEAT_TRIGGER, false).doAction(context);
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

	public void ensureWatch() {
		if (isEnded()) return;
		ensureGenRandom();
		ensureHabitat();
		if (ArondightQuest.gone(habitat)) return;
		LocationAPI where = habitat.getContainingLocation();
		if (where == null) return;
		boolean alive = false;
		for (CampaignFleetAPI f : where.getFleets()) {
			if (!f.getMemoryWithoutUpdate().getBoolean(WARDEN_FLEET_FLAG)) continue;
			alive = true;
			holdWatchAt(f, habitat, this);
			hardenWatch(f);
		}
		if (alive || currentStage != Stage.DEFEAT_WARDENS) return;
		if (ArondightQuest.mem(ArondightQuest.WARDENS_DEFEATED)
				|| ArondightQuest.mem(ArondightQuest.M1_DONE)) {
			return;
		}
		new SpawnWatchAction(habitat).doAction(new TriggerActionContext(this));
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
