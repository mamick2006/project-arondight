package arondight.campaign;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

public class ArondightSoloBattle {

	public static final String ACTIVE = "$arondight_soloActive";
	public static final String PRE_MOTHBALLED = "$arondight_preMothballed";
	public static final String PRE_REAL_MOTHBALLED = "$arondight_preRealMothballed";
	public static final String ORIG_FLAGSHIP = "$arondight_origFlagship";
	public static final String TEMP_FRAME = "$arondight_tempFrame";
	public static final String TEMP_CREW = "$arondight_tempCrew";

	public static FleetMemberAPI beginWithTempFrame() {
		restoreIfLeftOver();

		FleetMemberAPI frame = ArondightQuest.createFrameMember();
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

		fleet.getFleetData().addFleetMember(frame);
		mem.set(TEMP_FRAME, frame.getId());

		int crew = (int) frame.getMinCrew();
		if (crew > 0) {
			fleet.getCargo().addCommodity(Commodities.CREW, crew);
			mem.set(TEMP_CREW, crew);
		}

		begin(frame);
		return frame;
	}

	public static FleetMemberAPI beginWithOwnedFrame() {
		restoreIfLeftOver();
		FleetMemberAPI frame = ArondightQuest.findFrameMember();
		if (frame == null) frame = arondight.state.ArondightState.findPlayerFrame();
		if (frame == null) return null;
		begin(frame);
		return frame;
	}

	protected static void begin(FleetMemberAPI frame) {
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

		mem.set(ACTIVE, true);

		List<String> preCrash = new ArrayList<String>();
		List<String> preReal = new ArrayList<String>();
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.getRepairTracker().isCrashMothballed()) {
				preCrash.add(member.getId());
			}
			if (member.getRepairTracker().isMothballed()) {
				preReal.add(member.getId());
			}
		}
		mem.set(PRE_MOTHBALLED, preCrash);
		mem.set(PRE_REAL_MOTHBALLED, preReal);

		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.isFlagship()) {
				mem.set(ORIG_FLAGSHIP, member.getId());
				member.setFlagship(false);
				member.setCaptain(null);
			}
			if (member != frame) {
				if (!member.getRepairTracker().isCrashMothballed()) {
					member.getRepairTracker().setCrashMothballed(true);
				}
				if (!member.getRepairTracker().isMothballed()) {
					member.getRepairTracker().setMothballed(true);
				}
			}
		}

		frame.getRepairTracker().setCrashMothballed(false);
		frame.getRepairTracker().setMothballed(false);
		frame.setCaptain(Global.getSector().getPlayerPerson());
		fleet.getFleetData().setFlagship(frame);
	}

	public static void restore() {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		if (!mem.getBoolean(ACTIVE)) return;

		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

		Object tempId = mem.get(TEMP_FRAME);
		FleetMemberAPI temp = memberById(fleet, tempId);
		boolean keepFrame = mem.getBoolean(ArondightQuest.LOOM_WON);
		if (temp != null && !keepFrame) {
			fleet.getFleetData().removeFleetMember(temp);
			arondight.state.ArondightState.removeFrameState(temp.getId());
			Object crew = mem.get(TEMP_CREW);
			if (crew instanceof Integer) {
				float have = fleet.getCargo().getCommodityQuantity(Commodities.CREW);
				fleet.getCargo().removeCommodity(Commodities.CREW, Math.min((Integer) crew, have));
			}
		}
		if (temp != null && keepFrame) {
			mem.set(ArondightQuest.FRAME_MEMBER, temp.getId());
		}

		List<String> preCrash = stringList(mem.get(PRE_MOTHBALLED));
		List<String> preReal = stringList(mem.get(PRE_REAL_MOTHBALLED));
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member == temp) continue;
			if (member.getRepairTracker().isCrashMothballed() && !preCrash.contains(member.getId())) {
				member.getRepairTracker().setCrashMothballed(false);
			}
			if (member.getRepairTracker().isMothballed() && !preReal.contains(member.getId())) {
				member.getRepairTracker().setMothballed(false);
			}
		}

		FleetMemberAPI orig = memberById(fleet, mem.get(ORIG_FLAGSHIP));
		FleetMemberAPI newFlagship = null;
		if (temp != null && keepFrame) {
			newFlagship = temp;
		} else if (orig != null) {
			newFlagship = orig;
		}
		if (newFlagship != null) {
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (member.isFlagship()) member.setFlagship(false);
			}
			newFlagship.setCaptain(Global.getSector().getPlayerPerson());
			fleet.getFleetData().setFlagship(newFlagship);
		}

		mem.unset(ACTIVE);
		mem.unset(PRE_MOTHBALLED);
		mem.unset(PRE_REAL_MOTHBALLED);
		mem.unset(ORIG_FLAGSHIP);
		mem.unset(TEMP_FRAME);
		mem.unset(TEMP_CREW);
	}

	static List<FleetMemberAPI> liftTransactionMothballs() {
		List<FleetMemberAPI> lifted = new ArrayList<FleetMemberAPI>();
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		if (!mem.getBoolean(ACTIVE)) return lifted;

		List<String> preReal = stringList(mem.get(PRE_REAL_MOTHBALLED));
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (!member.getRepairTracker().isMothballed()) continue;
			if (preReal.contains(member.getId())) continue;
			member.getRepairTracker().setMothballed(false);
			lifted.add(member);
		}
		return lifted;
	}

	static void reapplyTransactionMothballs(List<FleetMemberAPI> lifted) {
		for (FleetMemberAPI member : lifted) {
			member.getRepairTracker().setMothballed(true);
		}
	}

	protected static List<String> stringList(Object value) {
		List<String> list = new ArrayList<String>();
		if (value instanceof List) {
			for (Object id : (List<?>) value) {
				if (id instanceof String) list.add((String) id);
			}
		}
		return list;
	}

	public static void restoreIfLeftOver() {
		if (Global.getSector().getMemoryWithoutUpdate().getBoolean(ACTIVE)) {
			restore();
		}
	}

	protected static FleetMemberAPI memberById(CampaignFleetAPI fleet, Object id) {
		if (!(id instanceof String)) return null;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (id.equals(member.getId())) return member;
		}
		return null;
	}
}
