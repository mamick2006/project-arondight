package arondight.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DevMenuOptions;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import org.lwjgl.input.Keyboard;

public class ArondightFIDPlugin extends FleetInteractionDialogPluginImpl {

	public ArondightFIDPlugin() {
		this(null);
	}

	public ArondightFIDPlugin(FIDConfig config) {
		super(config);
		context = new ArondightEncounterContext();
	}

	@Override
	protected void updatePreCombat() {
		options.clearOptions();

		FleetMemberAPI frame = findSoloMember();
		if (frame != null && Global.getSector().getPlayerFleet() != null) {
			for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
				if (member.isFlagship()) member.setFlagship(false);
			}
			selectedFlagship = frame;
			Global.getSector().getPlayerFleet().getFleetData().setFlagship(frame);
		}

		String label = "Launch";
		if (otherFleet != null && otherFleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.LOOM_BATTLE_FLEET)) {
			label = arondight.ArondightStrings.quest("quest_opt_enter_course");
		} else if (otherFleet != null && otherFleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.DUEL_BATTLE_FLEET)) {
			label = arondight.ArondightStrings.quest("quest_opt_close_distance");
		}
		options.addOption(label, OptionId.CONTINUE_INTO_BATTLE, null);

		boolean canGoBack = ongoingBattle || Global.getSettings().isDevMode();
		if (canGoBack) {
			options.addOption("Go back", OptionId.GO_TO_MAIN, null);
			options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
		}

		if (Global.getSettings().isDevMode()) {
			DevMenuOptions.addOptions(dialog);
		}
	}

	protected FleetMemberAPI findSoloMember() {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		Object id = mem.get(ArondightSoloBattle.TEMP_FRAME);
		if (!(id instanceof String)) id = mem.get(ArondightQuest.FRAME_MEMBER);
		if (!(id instanceof String)) return null;
		for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
			if (id.equals(member.getId())) return member;
		}
		return null;
	}
}
