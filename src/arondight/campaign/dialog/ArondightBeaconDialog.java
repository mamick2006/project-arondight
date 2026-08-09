package arondight.campaign.dialog;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.campaign.ArondightQuest;
import arondight.campaign.missions.ArondightReclaimerMission;
import arondight.ui.ArondightUi;

public class ArondightBeaconDialog implements InteractionDialogPlugin {

	public static enum OptionId {
		READ_LEDGER,
		DONE,
	}

	protected InteractionDialogAPI dialog;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();

		SpriteAPI signal = ArondightUi.spriteObj("illustrations", "arondight_beacon_signal");
		if (signal != null) {
			dialog.getVisualPanel().showImagePortion("illustrations", "arondight_beacon_signal",
					signal.getWidth(), signal.getHeight(), 0, 0, 480, 300);
		}

		text.addPara(ArondightStrings.quest("quest_beacon_1"));

		text.addPara(ArondightStrings.quest("quest_beacon_2"));

		options.clearOptions();
		options.addOption(ArondightStrings.quest("quest_beacon_opt_ledger"), OptionId.READ_LEDGER, null);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (optionData == OptionId.READ_LEDGER) {
			text.addPara(ArondightStrings.quest("quest_beacon_ledger"));

			text.addPara(ArondightStrings.quest("quest_beacon_reclamation"), Misc.getNegativeHighlightColor());

			ArondightReclaimerMission mission = new ArondightReclaimerMission();
			mission.setMissionId(ArondightQuest.RECLAIMER_MISSION_ID);
			String icon = ArondightUi.sprite("icons", "arondight_intel");
			if (icon == null) {
				icon = Global.getSettings().getSpriteName("intel", "fleet_log");
			}
			mission.setIconName(icon);
			mission.setGenRandom(new java.util.Random(Misc.random.nextLong()));
			mission.createAndAbortIfFailed(null, false);
			if (!mission.isMissionCreationAborted()) {
				mission.accept(dialog, null);
				arondight.campaign.ArondightSignalIntel.onBeaconAnswered();
			}

			options.clearOptions();
			options.addOption(ArondightStrings.quest("quest_beacon_opt_done"), OptionId.DONE, null);
		} else if (optionData == OptionId.DONE) {
			Global.getSector().setPaused(false);
			dialog.dismiss();
		}
	}

	@Override
	public void optionMousedOver(String optionText, Object optionData) {}

	@Override
	public void advance(float amount) {}

	@Override
	public void backFromEngagement(EngagementResultAPI battleResult) {}

	@Override
	public Object getContext() {
		return null;
	}

	@Override
	public Map<String, MemoryAPI> getMemoryMap() {
		return null;
	}
}
