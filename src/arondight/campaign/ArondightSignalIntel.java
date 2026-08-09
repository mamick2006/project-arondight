package arondight.campaign;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import arondight.ArondightStrings;
import arondight.campaign.missions.ArondightReclaimerWaitScript;

public class ArondightSignalIntel extends BaseIntelPlugin {

	protected ArondightReclaimerWaitScript wait;
	protected boolean answered;

	public static ArondightSignalIntel get() {
		if (Global.getSector() == null) {
			return null;
		}
		return (ArondightSignalIntel) Global.getSector().getIntelManager()
				.getFirstIntel(ArondightSignalIntel.class);
	}

	public static void onLoomWin(ArondightReclaimerWaitScript wait) {
		if (Global.getSector() == null || ArondightQuest.mem(ArondightQuest.M2_DONE)) {
			return;
		}
		if (get() != null) {
			return;
		}
		ArondightSignalIntel intel = new ArondightSignalIntel();
		intel.wait = wait;
		Global.getSector().getIntelManager().addIntel(intel);
	}

	public static void onBeaconAnswered() {
		ArondightSignalIntel intel = get();
		if (intel == null || intel.answered) {
			return;
		}
		intel.answered = true;
		intel.wait = null;
		intel.sendUpdateIfPlayerHasIntel(null, false);
		intel.endAfterDelay();
	}

	@Override
	protected void advanceImpl(float amount) {
		if (!answered && !isEnding() && !isEnded()
				&& ArondightQuest.mem(ArondightQuest.M2_DONE)) {
			answered = true;
			wait = null;
			endAfterDelay();
		}
	}

	@Override
	public String getName() {
		return "The Recovered Frame";
	}

	@Override
	public String getSortString() {
		return getName();
	}

	@Override
	public String getIcon() {
		String icon = arondight.ui.ArondightUi.sprite("icons", "arondight_intel");
		if (icon != null) {
			return icon;
		}
		return Global.getSettings().getSpriteName("intel", "fleet_log");
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_MISSIONS);
		return tags;
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		info.addPara(getName(), getTitleColor(mode), 0f);
		float days = wait != null ? wait.getRemainingDays() : 0f;
		if (!answered && days >= 0.5f) {
			bullet(info);
			addDays(info, ArondightStrings.quest("signal_countdown_suffix"), days,
					getBulletColorForMode(mode), 3f);
			unindent(info);
		}
	}

	@Override
	public boolean hasSmallDescription() {
		return true;
	}

	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color tc = Misc.getTextColor();
		if (!answered) {
			info.addPara(ArondightStrings.quest("signal_wait_1"), tc, 10f);
			info.addPara(ArondightStrings.quest("signal_wait_hint"),
					tc, 10f);
			float days = wait != null ? wait.getRemainingDays() : 0f;
			if (days >= 0.5f) {
				addDays(info, ArondightStrings.quest("signal_countdown_suffix_period"), days, tc, 10f);
			}
			return;
		}
		info.addPara(ArondightStrings.quest("signal_answered"), 10f, tc,
				Misc.getNegativeHighlightColor(), "RECLAMATION");
	}
}
