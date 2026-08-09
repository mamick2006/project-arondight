package arondight.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import arondight.combat.loom.LoomBCP;
import arondight.combat.reclaimer.ReclaimerBCP;

public class ArondightCampaignPlugin extends BaseCampaignPlugin {

	@Override
	public String getId() {
		return "ArondightCampaignPlugin";
	}

	@Override
	public boolean isTransient() {
		return true;
	}

	@Override
	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
		if (interactionTarget instanceof CampaignFleetAPI) {
			CampaignFleetAPI fleet = (CampaignFleetAPI) interactionTarget;
			if (fleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.LOOM_BATTLE_FLEET)
					|| fleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.DUEL_BATTLE_FLEET)) {
				return new PluginPick<InteractionDialogPlugin>(new ArondightFIDPlugin(), PickPriority.HIGHEST);
			}
		}
		return null;
	}

	@Override
	public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
		if (opponent instanceof CampaignFleetAPI) {
			CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
			if (fleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.LOOM_BATTLE_FLEET)) {
				return new PluginPick<BattleCreationPlugin>(new LoomBCP(), PickPriority.HIGHEST);
			}
			if (fleet.getMemoryWithoutUpdate().getBoolean(ArondightQuest.DUEL_BATTLE_FLEET)) {
				return new PluginPick<BattleCreationPlugin>(new ReclaimerBCP(), PickPriority.HIGHEST);
			}
		}
		return null;
	}
}
