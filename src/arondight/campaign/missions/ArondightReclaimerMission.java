package arondight.campaign.missions;

import java.awt.Color;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerActionContext;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import arondight.campaign.ArondightQuest;
import arondight.util.ArondightSettings;

public class ArondightReclaimerMission extends HubMissionWithSearch {

	public static enum Stage {
		GO_TO_RELAY,
		ESCORT_FIGHT,
		TO_STAGING,
		COMPLETED,
	}

	public static final String ESCORT_DEFEAT_TRIGGER = "ArondightEscortDefeated";
	public static final String RELAY_ENTITY = "arondight_relay";
	public static final String RELAY_FLAG = "$arondight_relay";
	public static final String RELAY_IMPORTANT_FLAG = "$arondight_rec_imp";
	public static final String STAGING_ENTITY = "arondight_staging";
	public static final String STAGING_FLAG = "$arondight_staging";
	public static final String STAGING_IMPORTANT_FLAG = "$arondight_staging_imp";

	protected StarSystemAPI relaySystem;
	protected StarSystemAPI stagingSystem;
	protected SectorEntityToken relay;
	protected SectorEntityToken staging;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$arondight_rec_ref")) {
			return false;
		}

		Vector2f from = Global.getSector().getPlayerFleet() != null
				? Global.getSector().getPlayerFleet().getLocationInHyperspace()
				: new Vector2f();

		requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_CORE_POPULATED);
		requireSystemWithinRangeOf(from, 14f);
		preferSystemUnexplored();
		relaySystem = pickSystem();
		if (relaySystem == null) return false;

		relay = spawnEntity(RELAY_ENTITY, new LocData(EntityLocationType.UNCOMMON, null, relaySystem, false));
		if (relay == null) return false;
		dressRelay();

		requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_CORE_POPULATED);
		requireSystemWithinRangeOf(relay.getLocationInHyperspace(), 12f);
		requireSystemNot(relaySystem);
		preferSystemUnexplored();
		stagingSystem = pickSystem();
		if (stagingSystem == null) return false;

		staging = spawnEntity(STAGING_ENTITY, new LocData(EntityLocationType.HIDDEN_NOT_NEAR_STAR, null, stagingSystem, false));
		if (staging == null) return false;
		dressStaging();

		setStartingStage(Stage.GO_TO_RELAY);
		addSuccessStages(Stage.COMPLETED);

		connectWithGlobalFlag(Stage.GO_TO_RELAY, Stage.ESCORT_FIGHT, ArondightQuest.RELAY_SCANNED);
		connectWithGlobalFlag(Stage.ESCORT_FIGHT, Stage.TO_STAGING, ArondightQuest.ESCORT_DEFEATED);
		connectWithGlobalFlag(Stage.TO_STAGING, Stage.COMPLETED, ArondightQuest.M2_DONE);

		beginStageTrigger(Stage.ESCORT_FIGHT);
		triggerCustomAction(new SpawnThreatEscort(relay));
		endTrigger();

		changes.add(new ArondightLoomMission.SoloRestoreOnAbort());

		return true;
	}

	public static final String ESCORT_FLEET_FLAG = "$arondight_escortFleet";

	public static class SpawnThreatEscort implements TriggerAction {
		protected SectorEntityToken relay;

		public SpawnThreatEscort(SectorEntityToken relay) {
			this.relay = relay;
		}

		@Override
		public void doAction(TriggerActionContext context) {
			if (ArondightQuest.gone(relay)) return;
			CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet("threat", "Reclamation Escort", true);
			fleet.setNoFactionInName(true);
			addUnit(fleet, "skirmish_unit_Type100");
			addUnit(fleet, "skirmish_unit_Type101");
			addUnit(fleet, "assault_unit_Type200");
			addUnit(fleet, "assault_unit_Type201");
			int standoffs = ArondightSettings.getInt("arondight_escort_standoff_units", 1);
			for (int i = 0; i < standoffs; i++) {
				addUnit(fleet, "standoff_unit_Type300");
			}

			relay.getContainingLocation().addEntity(fleet);
			Vector2f at = Misc.getPointAtRadius(relay.getLocation(), 225f);
			fleet.setLocation(at.x, at.y);

			Misc.addDefeatTrigger(fleet, ESCORT_DEFEAT_TRIGGER);
			fleet.getMemoryWithoutUpdate().set(ESCORT_FLEET_FLAG, true);
			hardenEscort(fleet);

			CampaignFleetAPI player = Global.getSector().getPlayerFleet();
			fleet.addAssignment(FleetAssignment.INTERCEPT, player, 20f);
			fleet.addAssignment(FleetAssignment.HOLD, relay, 60f);
			fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, relay, 3650f);
		}

		protected void addUnit(CampaignFleetAPI fleet, String variantId) {
			FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
			fleet.getFleetData().addFleetMember(member);
			member.setOwner(1);
			member.getRepairTracker().setCR(0.7f);
		}
	}

	public static void hardenEscort(CampaignFleetAPI fleet) {
		if (fleet == null) return;
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
	}

	protected void dressRelay() {
		relay.getMemoryWithoutUpdate().set(RELAY_FLAG, true);
		relay.setDiscoverable(true);
		makeImportant(relay, RELAY_IMPORTANT_FLAG, Stage.GO_TO_RELAY, Stage.ESCORT_FIGHT);
	}

	protected void dressStaging() {
		staging.getMemoryWithoutUpdate().set(STAGING_FLAG, true);
		staging.setDiscoverable(true);
		makeImportant(staging, STAGING_IMPORTANT_FLAG, Stage.TO_STAGING);
	}

	protected void ensureGenRandom() {
		if (genRandom == null) {
			setGenRandom(new Random());
		}
	}

	protected void ensureEntities() {
		if (relaySystem != null && ArondightQuest.gone(relay)) {
			SectorEntityToken fresh = spawnEntity(RELAY_ENTITY,
					new LocData(EntityLocationType.UNCOMMON, null, relaySystem, false));
			if (fresh != null) {
				relay = fresh;
				dressRelay();
			}
		}
		if (stagingSystem != null && ArondightQuest.gone(staging)) {
			SectorEntityToken fresh = spawnEntity(STAGING_ENTITY,
					new LocData(EntityLocationType.HIDDEN_NOT_NEAR_STAR, null, stagingSystem, false));
			if (fresh != null) {
				staging = fresh;
				dressStaging();
			}
		}
	}

	public void ensureEscort() {
		if (isEnded()) return;
		ensureGenRandom();
		ensureEntities();
		if (ArondightQuest.gone(relay) || relay.getContainingLocation() == null) return;
		boolean alive = false;
		for (CampaignFleetAPI f : relay.getContainingLocation().getFleets()) {
			if (!f.getMemoryWithoutUpdate().getBoolean(ESCORT_FLEET_FLAG)) continue;
			alive = true;
			f.setAIMode(true);
			hardenEscort(f);
			for (FleetMemberAPI m : f.getFleetData().getMembersListCopy()) {
				m.getRepairTracker().setCR(Math.max(m.getRepairTracker().getCR(), 0.7f));
			}
		}
		if (!alive && currentStage == Stage.ESCORT_FIGHT
				&& !ArondightQuest.mem(ArondightQuest.ESCORT_DEFEATED)
				&& !ArondightQuest.mem(ArondightQuest.M2_DONE)) {
			new SpawnThreatEscort(relay).doAction(null);
		}
	}

	@Override
	protected void updateInteractionDataImpl() {
		set("$arondight_rec_relaySystem", relaySystem.getNameWithLowercaseTypeShort());
		set("$arondight_rec_stagingSystem", stagingSystem.getNameWithLowercaseTypeShort());
	}

	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		if (currentStage == Stage.GO_TO_RELAY) {
			info.addPara(arondight.ArondightStrings.quest("quest_rec_desc_relay",
					relaySystem.getNameWithLowercaseTypeShort()), opad);
		} else if (currentStage == Stage.ESCORT_FIGHT) {
			info.addPara(arondight.ArondightStrings.quest("quest_rec_desc_escort"), opad);
		} else if (currentStage == Stage.TO_STAGING) {
			info.addPara(arondight.ArondightStrings.quest("quest_rec_desc_staging",
					stagingSystem.getNameWithLowercaseTypeShort()), opad);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		if (currentStage == Stage.GO_TO_RELAY) {
			info.addPara(getGoToSystemTextShort(relaySystem), tc, pad);
			return true;
		} else if (currentStage == Stage.ESCORT_FIGHT) {
			info.addPara(arondight.ArondightStrings.quest("quest_rec_step_escort"), tc, pad);
			return true;
		} else if (currentStage == Stage.TO_STAGING) {
			info.addPara(getGoToSystemTextShort(stagingSystem), tc, pad);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		return "The Reclaimer";
	}
}
