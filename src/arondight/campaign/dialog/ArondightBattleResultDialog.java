package arondight.campaign.dialog;

import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.campaign.ArondightBattleResultScript.Kind;
import arondight.campaign.ArondightQuest;

public class ArondightBattleResultDialog implements InteractionDialogPlugin {

	public static enum OptionId {
		CONTINUE_1,
		DONE,
	}

	public static ArondightBattleResultDialog win(Kind kind) {
		return new ArondightBattleResultDialog(kind, true);
	}

	public static ArondightBattleResultDialog lose(Kind kind) {
		return new ArondightBattleResultDialog(kind, false);
	}

	protected Kind kind;
	protected boolean won;

	protected InteractionDialogAPI dialog;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;

	public ArondightBattleResultDialog(Kind kind, boolean won) {
		this.kind = kind;
		this.won = won;
	}

	@Override
	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();

		if (kind == Kind.LOOM && won) {
			initLoomWin();
		} else if (kind == Kind.LOOM) {
			initLoomLoss();
		} else if (won) {
			initDuelWin();
		} else {
			initDuelLoss();
		}
	}

	protected void initLoomWin() {
		text.addPara(ArondightStrings.quest("quest_loomwin_1"));

		text.addPara(ArondightStrings.quest("quest_loomwin_2"));

		ArondightQuest.completeMission1(ArondightQuest.findTempOrOwnedFrame(), text);

		text.setFontSmallInsignia();
		text.addPara(ArondightStrings.quest("quest_loomwin_note"), Misc.getPositiveHighlightColor());
		text.setFontInsignia();

		options.clearOptions();
		options.addOption(ArondightStrings.quest("quest_loomwin_opt"), OptionId.DONE, null);
	}

	protected void initLoomLoss() {
		ArondightQuest.unset(ArondightQuest.LOOM_STARTED);

		text.addPara(ArondightStrings.quest("quest_loomloss_1"));

		text.addPara(ArondightStrings.quest("quest_loomloss_2"));

		options.clearOptions();
		options.addOption(ArondightStrings.quest("opt_continue"), OptionId.DONE, null);
	}

	protected void initDuelWin() {
		text.addPara(ArondightStrings.quest("quest_duelwin_1"));

		text.addPara(ArondightStrings.quest("quest_duelwin_2"));

		ArondightQuest.completeChain();
		Global.getSector().getMemoryWithoutUpdate().set("$arondight_mgmt_attention", true);

		text.setFontSmallInsignia();
		text.addPara(ArondightStrings.quest("quest_duelwin_note"), Misc.getPositiveHighlightColor());
		text.setFontInsignia();

		options.clearOptions();
		options.addOption(ArondightStrings.quest("quest_duelwin_opt"), OptionId.DONE, null);
	}

	protected void initDuelLoss() {
		ArondightQuest.unset(ArondightQuest.DUEL_STARTED);

		text.addPara(ArondightStrings.quest("quest_duelloss_1"));

		text.addPara(ArondightStrings.quest("quest_duelloss_2"));

		options.clearOptions();
		options.addOption(ArondightStrings.quest("quest_duelloss_opt"), OptionId.DONE, null);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (optionData == OptionId.DONE) {
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
