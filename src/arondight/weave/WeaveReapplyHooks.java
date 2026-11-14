package arondight.weave;

import java.util.List;

import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.campaign.listeners.DModRepairListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.state.ArondightState;

public class WeaveReapplyHooks implements CoreUITabListener, ShipRecoveryListener,
        ColonyInteractionListener, EconomyTickListener, DModRepairListener {

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (id == CoreUITabId.FLEET || id == CoreUITabId.REFIT) {
            WeaveApplier.reapplyPlayerFleetIfNeeded();
        }
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> members, InteractionDialogAPI dialog) {
        if (!ArondightState.hasAnyFrame()) return;
        WeaveApplier.reapplyPlayerFleetIfNeeded();
    }

    @Override
    public void reportPlayerOpenedMarket(com.fs.starfarer.api.campaign.econ.MarketAPI market) {}

    @Override
    public void reportPlayerClosedMarket(com.fs.starfarer.api.campaign.econ.MarketAPI market) {}

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(com.fs.starfarer.api.campaign.econ.MarketAPI market) {}

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        WeaveApplier.reapplyPlayerFleetIfNeeded();
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        WeaveApplier.reapplyPlayerFleetIfNeeded();
    }

    @Override
    public void reportEconomyMonthEnd() {}

    @Override
    public void reportDModsRepaired(FleetMemberAPI member, ShipVariantAPI variant, int parts) {
        WeaveApplier.applyForRepairedDHull(member, variant);
    }
}
