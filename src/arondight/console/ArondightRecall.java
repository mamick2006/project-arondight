package arondight.console;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightIds;
import arondight.ArondightStrings;
import arondight.campaign.ArondightQuest;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.util.ArondightLog;
import arondight.weave.ArondightWeave;

public final class ArondightRecall {

    public enum Outcome { IN_FLEET, FROM_STORAGE, ADOPTED, NOT_FOUND }

    public static final class Result {
        public final Outcome outcome;
        public final String message;
        public final FleetMemberAPI member;

        Result(Outcome outcome, String message, FleetMemberAPI member) {
            this.outcome = outcome;
            this.message = message;
            this.member = member;
        }
    }

    private ArondightRecall() {}

    public static Result run() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

        FleetMemberAPI inFleet = ArondightState.findPlayerFrame();
        if (inFleet != null) {
            return new Result(Outcome.IN_FLEET, line("recall_in_fleet", inFleet.getShipName()), inFleet);
        }

        List<String> tracked = trackedIds();
        if (!tracked.isEmpty()) {
            Result stored = recallFromSubmarkets(fleet, tracked);
            if (stored != null) return stored;
        }

        FleetMemberAPI stray = findUntrackedFrame(fleet);
        if (stray != null) {
            return adopt(fleet, stray, tracked.isEmpty() ? null : tracked.get(0));
        }

        return new Result(Outcome.NOT_FOUND, line("recall_not_found"), null);
    }

    private static List<String> trackedIds() {
        List<String> ids = new ArrayList<>(ArondightState.getFrameMemberIds());
        Object questFrame = Global.getSector().getMemoryWithoutUpdate().get(ArondightQuest.FRAME_MEMBER);
        if (questFrame instanceof String && ids.remove(questFrame)) {
            ids.add(0, (String) questFrame);
        }
        return ids;
    }

    private static Result recallFromSubmarkets(CampaignFleetAPI fleet, List<String> tracked) {
        Map<MarketAPI, Boolean> seen = new IdentityHashMap<>();
        List<MarketAPI> markets = new ArrayList<>(Global.getSector().getEconomy().getMarketsCopy());
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (SectorEntityToken entity : location.getAllEntities()) {
                if (entity.getMarket() != null) markets.add(entity.getMarket());
            }
        }
        for (MarketAPI market : markets) {
            if (seen.put(market, Boolean.TRUE) != null) continue;
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                if (submarket.getPlugin() == null || !submarket.getPlugin().isFreeTransfer()) continue;
                CargoAPI cargo = submarket.getCargo();
                FleetDataAPI ships = cargo == null ? null : cargo.getMothballedShips();
                if (ships == null) continue;
                for (FleetMemberAPI member : ships.getMembersListCopy()) {
                    if (!tracked.contains(member.getId())) continue;
                    ships.removeFleetMember(member);
                    fleet.getFleetData().addFleetMember(member);
                    Global.getSector().getMemoryWithoutUpdate().set(ArondightQuest.FRAME_MEMBER, member.getId());
                    ArondightWeave.apply(member);
                    syncFleet(fleet);
                    return new Result(Outcome.FROM_STORAGE, line("recall_from_storage",
                            member.getShipName(), submarket.getNameOneLine(), market.getName()), member);
                }
            }
        }
        return null;
    }

    private static Result adopt(CampaignFleetAPI fleet, FleetMemberAPI member, String lostId) {
        String id = member.getId();
        boolean carried = lostId != null && ArondightState.moveFrameState(lostId, id);
        String worn = formOf(member);
        if (!carried) {
            ArondightState.createFrameState(id);
            ArondightState.setCurrentForm(id, worn);
        }
        Global.getSector().getMemoryWithoutUpdate().set(ArondightQuest.FRAME_MEMBER, id);

        String carriedForm = ArondightState.getCurrentForm(id);
        if (carriedForm.equals(worn)) {
            ArondightWeave.apply(member);
        } else {
            ArondightState.setCurrentForm(id, worn);
            ArondightWeave.apply(member);
            ArondightTransformer.TransformResult r = ArondightTransformer.transformUnchecked(member, carriedForm);
            if (!r.success) {
                ArondightLog.warnOnce("recall_transform_" + carriedForm, "the recalled frame stays in "
                        + worn + "; the transform to " + carriedForm + " was refused (" + r.refusalReason + ")");
            }
        }
        syncFleet(fleet);
        return new Result(Outcome.ADOPTED, carried
                ? line("recall_adopted_carried", member.getShipName())
                : line("recall_adopted", member.getShipName()), member);
    }

    private static FleetMemberAPI findUntrackedFrame(CampaignFleetAPI fleet) {
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isFighterWing() || ArondightState.isFrame(member.getId())) continue;
            if (formOf(member) != null) return member;
        }
        return null;
    }

    private static String formOf(FleetMemberAPI member) {
        ShipHullSpecAPI spec = member.getHullSpec();
        if (spec == null) return null;
        for (String id : new String[] { spec.getHullId(), spec.getDParentHullId(), spec.getBaseHullId() }) {
            if (id != null && ArondightIds.ALL_FORMS.contains(id)) return id;
        }
        return null;
    }

    private static void syncFleet(CampaignFleetAPI fleet) {
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    private static String line(String key, Object... args) {
        return ArondightStrings.get("arondight_recall", key, args);
    }
}
