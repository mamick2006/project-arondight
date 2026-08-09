package arondight.campaign.bar;

import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarData;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.campaign.ArondightQuest;
import arondight.campaign.missions.ArondightLoomMission;

public class ArondightBarEvent extends BaseBarEvent {

	public static enum OptionId {
		PRESS,
		TAKE,
		DECLINE,
	}

	protected long seed = Misc.random.nextLong();

	transient protected PersonAPI salvor;

	@Override
	public boolean isAlwaysShow() {
		return true;
	}

	@Override
	public String getBarEventId() {
		return "arondight_salvor";
	}

	@Override
	public boolean shouldShowAtMarket(MarketAPI market) {
		return !ArondightQuest.mem(ArondightQuest.QUEST_ACCEPTED)
				&& !ArondightQuest.mem(ArondightQuest.SALVOR_DECLINED);
	}

	@Override
	public void wasShownAtMarket(MarketAPI market) {
	}

	@Override
	public boolean shouldRemoveEvent() {
		return ArondightQuest.mem(ArondightQuest.QUEST_ACCEPTED)
				|| ArondightQuest.mem(ArondightQuest.SALVOR_DECLINED);
	}

	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.addPromptAndOption(dialog, memoryMap);

		Random random = new Random(seed + dialog.getInteractionTarget().getMarket().getId().hashCode());
		salvor = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson(Gender.MALE, random);
		salvor.setPostId(Ranks.POST_SPACER);
		salvor.setImportanceAndVoice(PersonImportance.LOW, random);

		dialog.getTextPanel().addPara(ArondightStrings.bar("bar_prompt"));

		dialog.getOptionPanel().addOption(ArondightStrings.bar("bar_opt_sit"), this);
	}

	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.init(dialog, memoryMap);
		done = false;

		dialog.getVisualPanel().showPersonInfo(salvor, true);

		text.addPara(ArondightStrings.bar("bar_open_1"));

		text.addPara(ArondightStrings.bar("bar_open_2"));

		text.addPara(ArondightStrings.bar("bar_open_3"));

		options.clearOptions();
		options.addOption(ArondightStrings.bar("bar_opt_press"), OptionId.PRESS, null);
		options.addOption(ArondightStrings.bar("bar_opt_take"), OptionId.TAKE, null);
		options.addOption(ArondightStrings.bar("bar_opt_decline"),
				OptionId.DECLINE, null);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (!(optionData instanceof OptionId)) return;

		switch ((OptionId) optionData) {
			case PRESS:
				text.addPara(ArondightStrings.bar("bar_press_1"));

				text.addPara(ArondightStrings.bar("bar_press_2"));

				text.addPara(ArondightStrings.bar("bar_press_3"));

				options.clearOptions();
				options.addOption(ArondightStrings.bar("bar_opt_take"), OptionId.TAKE, null);
				options.addOption(ArondightStrings.bar("bar_opt_decline"),
				OptionId.DECLINE, null);
				break;

			case TAKE:
				ArondightLoomMission mission = new ArondightLoomMission();
				mission.setMissionId(ArondightQuest.LOOM_MISSION_ID);
				mission.setPersonOverride(salvor);
				mission.setGenRandom(new Random(Misc.random.nextLong()));
				mission.createAndAbortIfFailed(dialog.getInteractionTarget().getMarket(), true);
				if (mission.isMissionCreationAborted()) {
					text.addPara(ArondightStrings.bar("bar_take_aborted"));
					done = true;
					break;
				}

				text.addPara(ArondightStrings.bar("bar_take_1"));

				text.addPara(ArondightStrings.bar("bar_take_2"));

				ArondightQuest.set(ArondightQuest.QUEST_ACCEPTED);
				mission.accept(dialog, memoryMap);
				PortsideBarData.getInstance().removeEvent(this);

				done = true;
				break;

			case DECLINE:
				text.addPara(ArondightStrings.bar("bar_decline"));
				ArondightQuest.set(ArondightQuest.SALVOR_DECLINED);
				PortsideBarData.getInstance().removeEvent(this);
				done = true;
				break;
		}
	}
}
