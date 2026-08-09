package arondight.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import arondight.campaign.dialog.ArondightBattleResultDialog;

public class ArondightBattleResultScript implements EveryFrameScript {

	public static enum Kind { LOOM, DUEL }

	protected Kind kind;
	protected float counter = 0f;

	public ArondightBattleResultScript(Kind kind) {
		this.kind = kind;
	}

	@Override
	public void advance(float amount) {
		if (Global.getSector().isPaused()) return;
		counter += amount;
		if (counter < 1f) return;

		ArondightSoloBattle.restoreIfLeftOver();

		String started = kind == Kind.LOOM ? ArondightQuest.LOOM_STARTED : ArondightQuest.DUEL_STARTED;
		String won = kind == Kind.LOOM ? ArondightQuest.LOOM_WON : ArondightQuest.DUEL_WON;

		if (ArondightQuest.mem(won)) {
			Global.getSector().getCampaignUI().showInteractionDialog(
					ArondightBattleResultDialog.win(kind), null);
		} else if (ArondightQuest.mem(started)) {
			Global.getSector().getCampaignUI().showInteractionDialog(
					ArondightBattleResultDialog.lose(kind), null);
		}
		Global.getSector().removeScript(this);
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
