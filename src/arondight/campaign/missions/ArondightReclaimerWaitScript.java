package arondight.campaign.missions;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;

import arondight.campaign.ArondightQuest;
import arondight.campaign.dialog.ArondightBeaconDialog;

public class ArondightReclaimerWaitScript implements EveryFrameScript {

	protected float days = 0f;

	public float getRemainingDays() {
		return Math.max(0f, ArondightQuest.m2OfferDelayDays() - days);
	}

	@Override
	public void advance(float amount) {
		if (Global.getSector().isPaused()) return;
		days += Global.getSector().getClock().convertToDays(amount);
		if (days < ArondightQuest.m2OfferDelayDays()) return;

		if (Global.getCurrentState() != GameState.CAMPAIGN) return;
		if (Global.getSector().getCampaignUI().isShowingDialog()) return;
		if (ArondightQuest.mem(ArondightQuest.M2_DONE)) {
			Global.getSector().removeScript(this);
			return;
		}

		Global.getSector().getCampaignUI().showInteractionDialog(new ArondightBeaconDialog(), null);
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
