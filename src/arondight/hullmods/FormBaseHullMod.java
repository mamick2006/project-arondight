package arondight.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public abstract class FormBaseHullMod extends BaseHullMod {

    private static String handReason() {
        return arondight.ArondightStrings.hullmods("hullmod_hand_reason");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null
                || !ship.getHullSpec().getHullId().startsWith("arondight_")) {
            return false;
        }
        String ownFamily = ownFormHullId();
        return ownFamily == null || ship.getHullSpec().getHullId().startsWith(ownFamily);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return handReason();
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull,
                                          CampaignUIAPI.CoreUITradeMode mode) {
        return false;
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull,
                                                CampaignUIAPI.CoreUITradeMode mode) {
        return handReason();
    }

    private String ownFormHullId() {
        if (spec == null) return null;
        String id = spec.getId();
        if (!id.startsWith("arondight_form_")) return null;
        return "arondight_" + id.substring("arondight_form_".length());
    }
}
