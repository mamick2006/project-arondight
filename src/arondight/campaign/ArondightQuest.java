package arondight.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.campaign.bar.ArondightBarEvent;
import arondight.campaign.missions.ArondightLoomMission;
import arondight.campaign.missions.ArondightReclaimerMission;
import arondight.campaign.missions.ArondightReclaimerWaitScript;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.util.ArondightLog;
import arondight.util.ArondightSettings;

public class ArondightQuest {

	public static final String LOOM_MISSION_ID = "arondight_loom";
	public static final String RECLAIMER_MISSION_ID = "arondight_reclaimer";

	public static final String FRAME_VARIANT = "wolf_Starting";

	public static final String RECLAIMER_VARIANT = "arondight_reclaimer_Boss";
	public static final String RECLAIMER_FALLBACK_VARIANT = "hive_unit_Type350";

	public static String reclaimerVariant() {
		try {
			if (Global.getSettings().getVariant(RECLAIMER_VARIANT) != null) {
				return RECLAIMER_VARIANT;
			}
		} catch (Exception e) {
		}
		return RECLAIMER_FALLBACK_VARIANT;
	}

	public static final String QUEST_ACCEPTED = "$arondight_questAccepted";
	public static final String SALVOR_DECLINED = "$arondight_salvorDeclined";
	public static final String AT_LOOM_SYSTEM = "$arondight_atLoomSystem";
	public static final String WARDENS_DEFEATED = "$arondight_wardensDefeated";
	public static final String LOGS_READ = "$arondight_logsRead";
	public static final String LOOM_STARTED = "$arondight_loomStarted";
	public static final String LOOM_WON = "$arondight_loomWon";
	public static final String M1_DONE = "$arondight_m1Done";
	public static final String RELAY_SCANNED = "$arondight_relayScanned";
	public static final String ESCORT_DEFEATED = "$arondight_escortDefeated";
	public static final String DUEL_STARTED = "$arondight_duelStarted";
	public static final String DUEL_WON = "$arondight_duelWon";
	public static final String M2_DONE = "$arondight_m2Done";
	public static final String START_NOTE_SHOWN = "$arondight_startNoteShown";

	public static final String FRAME_MEMBER = "$arondight_frameMember";

	private static boolean warnedNoFrame = false;

	public static final String LOOM_BATTLE_FLEET = "$arondight_loomBattle";
	public static final String DUEL_BATTLE_FLEET = "$arondight_duelBattle";

	public static float m2OfferDelayDays() { return f("arondight_m2_offer_delay_days", 10f); }
	public static float loomSweeperSpeed() { return f("arondight_loom_sweeper_speed", 135f); }
	public static float bossStage2Hull() { return f("arondight_boss_stage2_hull", 0.50f); }
	public static float bossRegenWindow() { return f("arondight_boss_regen_window_seconds", 6f); }
	public static float bossReviveGrace() { return f("arondight_boss_revive_grace_seconds", 3f); }
	public static float bossWallGrace() { return f("arondight_boss_wall_grace_seconds", 6.5f); }
	public static float bossCageInterval() { return f("arondight_boss_cage_interval", 8f); }
	public static float bossCageRadius() { return f("arondight_boss_cage_radius", 1400f); }
	public static float bossCageSpeed() { return f("arondight_boss_cage_speed", 220f); }
	public static float bossCageGap() { return f("arondight_boss_cage_gap", 40f); }
	public static float bossFigureInterval() { return f("arondight_boss_figure_interval", 3.5f); }
	public static float bossHoldSeconds() { return f("arondight_boss_hold_seconds", 1.2f); }
	public static float bossFigureRadius() { return f("arondight_boss_figure_radius", 600f); }
	public static float bossDriftInterval() { return f("arondight_boss_drift_interval", 2f); }
	public static float bossRainInterval() { return f("arondight_boss_rain_interval", 1.0f); }
	public static float bossRainSpacing() { return f("arondight_boss_rain_spacing", 200f); }
	public static float bossRainPrecess() { return f("arondight_boss_rain_precess", 0.8f); }
	public static int bossGuidedCap() { return (int) f("arondight_boss_guided_cap", 8f); }
	public static int bossGuidedHeavyCap() { return (int) f("arondight_boss_guided_heavy_cap", 1f); }
	public static float bossRegenHullFraction() { return f("arondight_boss_regen_hull_fraction", 0.40f); }
	public static float bossRegenHullFraction2() { return f("arondight_boss_regen_hull_fraction_2", 0.25f); }
	public static float bossRadialInterval() { return f("arondight_boss_radial_interval", 2.5f); }
	public static float bossAimedInterval() { return f("arondight_boss_aimed_interval", 0.9f); }
	public static float bossFanInterval() { return f("arondight_boss_fan_interval", 6f); }
	public static float bossSwarmInterval() { return f("arondight_boss_swarm_interval", 5f); }
	public static float bossTelegraphSeconds() { return f("arondight_boss_telegraph_seconds", 1.2f); }
	public static float bossRingRadius() { return f("arondight_boss_ring_radius", 5200f); }
	public static float surgeTimeMult() { return f("arondight_surge_time_mult", 2.5f); }
	public static float surgeDuration() { return f("arondight_surge_duration_seconds", 1.5f); }
	public static float surgeCooldown() { return f("arondight_surge_cooldown_seconds", 5f); }

	public static float f(String key, float fallback) {
		return ArondightSettings.getFloat(key, fallback);
	}

	public static void onGameLoad() {
		Global.getSector().registerPlugin(new ArondightCampaignPlugin());

		boolean present = false;
		for (PortsideBarEvent event : PortsideBarData.getInstance().getEvents()) {
			if (event instanceof ArondightBarEvent) {
				present = true;
				break;
			}
		}
		if (!present && !mem(QUEST_ACCEPTED) && !mem(SALVOR_DECLINED)) {
			PortsideBarData.getInstance().addEvent(new ArondightBarEvent());
		}
		postStartNoteIfDue();

		ArondightSoloBattle.restoreIfLeftOver();

		Object loom = Global.getSector().getMemoryWithoutUpdate().get(ArondightLoomMission.GLOBAL_REF);
		if (loom instanceof ArondightLoomMission) {
			try {
				((ArondightLoomMission) loom).ensureWatch();
			} catch (Exception e) {
				ArondightLog.warnOnce("ensureWatchOnLoad",
						"could not repair the quarantine watch on load (" + e
						+ "); interacting with the station tries again.");
			}
		}

		Object rec = Global.getSector().getMemoryWithoutUpdate().get("$arondight_rec_ref");
		if (rec instanceof ArondightReclaimerMission) {
			try {
				((ArondightReclaimerMission) rec).ensureEscort();
			} catch (Exception e) {
				ArondightLog.warnOnce("ensureEscortOnLoad",
						"could not repair the Reclamation Escort on load (" + e
						+ "); interacting with the relay tries again.");
			}
		}

		ensureReclaimerBridge();

		arondight.combat.loom.LoomScenarioPlugin.resetForLoad();
	}

	public static boolean postStartNoteIfDue() {
		if (mem(START_NOTE_SHOWN) || mem(QUEST_ACCEPTED) || mem(SALVOR_DECLINED)) return false;
		String note = ArondightStrings.quest("quest_start_note");
		try {
			Global.getSector().getCampaignUI().getMessageDisplay()
					.addMessage(note, START_NOTE_HIGHLIGHT, Misc.getHighlightColor());
		} catch (Exception e) {
			return false;
		}
		set(START_NOTE_SHOWN);
		return true;
	}

	private static final String START_NOTE_HIGHLIGHT = "Project Arondight";

	public static void ensureReclaimerBridge() {
		if (!mem(M1_DONE) || mem(M2_DONE)) return;
		if (Global.getSector().getMemoryWithoutUpdate().get("$arondight_rec_ref")
				instanceof ArondightReclaimerMission) {
			return;
		}
		for (EveryFrameScript script : Global.getSector().getScripts()) {
			if (script instanceof ArondightReclaimerWaitScript) return;
		}
		ArondightReclaimerWaitScript again = new ArondightReclaimerWaitScript();
		Global.getSector().addScript(again);
		ArondightSignalIntel.onLoomWin(again);
	}

	public static boolean mem(String flag) {
		return Global.getSector().getMemoryWithoutUpdate().getBoolean(flag);
	}

	public static boolean gone(SectorEntityToken entity) {
		return entity == null || !entity.isAlive();
	}

	public static boolean hasLiveFleet(LocationAPI where, String flag) {
		if (where == null) return false;
		for (CampaignFleetAPI f : where.getFleets()) {
			if (f.isDespawning() || f.isEmpty()) continue;
			if (f.getMemoryWithoutUpdate().getBoolean(flag)) return true;
		}
		return false;
	}

	public static void set(String flag) {
		Global.getSector().getMemoryWithoutUpdate().set(flag, true);
	}

	public static void unset(String flag) {
		Global.getSector().getMemoryWithoutUpdate().unset(flag);
	}

	public static void acquireFrame(FleetMemberAPI member, TextPanelAPI text) {
		Global.getSector().getMemoryWithoutUpdate().set(FRAME_MEMBER, member.getId());
		ArondightState.createFrameState(member.getId());
		ArondightState.setQuestFlag(ArondightIds.QUEST_FRAME_RECOVERED, true);
		ArondightState.setQuestFlag(ArondightIds.QUEST_M1_COMPLETE, true);
		arondight.ui.ArondightIntel.ensureExists();
		if (text != null) {
			AddRemoveCommodity.addFleetMemberGainText(member, text);
		}
	}

	public static void completeChain() {
		ArondightState.setQuestFlag(ArondightIds.QUEST_M2_COMPLETE, true);
		set(M2_DONE);
	}

	public static FleetMemberAPI findFrameMember() {
		Object id = Global.getSector().getMemoryWithoutUpdate().get(FRAME_MEMBER);
		if (!(id instanceof String)) return null;
		for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
			if (id.equals(member.getId())) return member;
		}
		return null;
	}

	public static FleetMemberAPI findTempOrOwnedFrame() {
		Object id = Global.getSector().getMemoryWithoutUpdate().get(ArondightSoloBattle.TEMP_FRAME);
		if (id instanceof String) {
			for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
				if (id.equals(member.getId())) return member;
			}
		}
		return findFrameMember();
	}

	public static FleetMemberAPI createFrameMember() {
		FleetMemberAPI member = null;
		try {
			member = ArondightTransformer.createFrameMember();
		} catch (Exception e) {
			member = null;
		}
		if (member == null) {
			member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, FRAME_VARIANT);
		}
		member.setShipName("Arondight");
		member.getRepairTracker().setCR(Math.max(member.getRepairTracker().getCR(), 0.7f));
		return member;
	}

	public static void isolateFrameVariant(FleetMemberAPI member) {
		ShipVariantAPI variant = member.getVariant().clone();
		variant.setSource(VariantSource.REFIT);
		variant.setHullVariantId(Misc.genUID());
		member.setVariant(variant, false, false);
	}

	public static void completeMission1(FleetMemberAPI member, TextPanelAPI text) {
		boolean first = !mem(M1_DONE);
		if (member != null) {
			acquireFrame(member, text);
		}
		set(QUEST_ACCEPTED);
		set(M1_DONE);
		set(LOOM_WON);
		Global.getSector().getMemoryWithoutUpdate().set("$arondight_mgmt_attention", true);
		if (!first) {
			return;
		}
		if (member != null) {
			grantFrameStoryPoints(text);
		}
		ArondightReclaimerWaitScript wait = new ArondightReclaimerWaitScript();
		Global.getSector().addScript(wait);
		ArondightSignalIntel.onLoomWin(wait);
	}

	public static final String FRAME_SP_GRANTED = "$arondight_frame_sp_granted";

	public static int grantFrameStoryPoints(TextPanelAPI text) {
		if (mem(FRAME_SP_GRANTED)) return 0;
		int points = ArondightSettings.getInt("arondight_frame_story_points", 3);
		set(FRAME_SP_GRANTED);
		if (points <= 0) return 0;
		if (text != null) {
			Global.getSector().getPlayerStats().addStoryPoints(points, text, false);
		} else {
			Global.getSector().getPlayerStats().addStoryPoints(points);
		}
		return points;
	}
}
