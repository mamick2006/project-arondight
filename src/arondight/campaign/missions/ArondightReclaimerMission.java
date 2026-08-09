package arondight.campaign.missions;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
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

		relay = spawnEntity("arondight_relay", new LocData(EntityLocationType.UNCOMMON, null, relaySystem, false));
		if (relay == null) return false;
		relay.getMemoryWithoutUpdate().set("$arondight_relay", true);
		relay.setDiscoverable(true);

		requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_CORE_POPULATED);
		requireSystemWithinRangeOf(relay.getLocationInHyperspace(), 12f);
		requireSystemNot(relaySystem);
		preferSystemUnexplored();
		stagingSystem = pickSystem();
		if (stagingSystem == null) return false;

		staging = spawnEntity("arondight_staging", new LocData(EntityLocationType.HIDDEN_NOT_NEAR_STAR, null, stagingSystem, false));
		if (staging == null) return false;
		staging.getMemoryWithoutUpdate().set("$arondight_staging", true);
		staging.setDiscoverable(true);

		setStartingStage(Stage.GO_TO_RELAY);
		addSuccessStages(Stage.COMPLETED);

		makeImportant(relay, "$arondight_rec_imp", Stage.GO_TO_RELAY, Stage.ESCORT_FIGHT);
		makeImportant(staging, "$arondight_staging_imp", Stage.TO_STAGING);

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

	public void retrofitEscort() {
		if (isEnded() || relay == null || relay.getContainingLocation() == null) return;
		boolean alive = false;
		for (CampaignFleetAPI f : relay.getContainingLocation().getFleets()) {
			if (!f.getMemoryWithoutUpdate().getBoolean(ESCORT_FLEET_FLAG)) continue;
			alive = true;
			f.setAIMode(true);
			for (FleetMemberAPI m : f.getFleetData().getMembersListCopy()) {
				m.getRepairTracker().setCR(Math.max(m.getRepairTracker().getCR(), 0.7f));
			}
		}
		if (!alive && currentStage == Stage.ESCORT_FIGHT
				&& !ArondightQuest.mem(ArondightQuest.ESCORT_DEFEATED)) {
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
