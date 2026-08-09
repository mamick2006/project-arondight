package arondight.form.mechanics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import arondight.util.ArondightSettings;

public class WardBulwarkSystemScript extends WardSystemScript {

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        super.apply(stats, id, state, effectLevel);
        if (state == State.IN || state == State.ACTIVE) {
            float crawl = ArondightSettings.getFloat("arondight_ward_bulwark_anchor_mult", 0.1f);
            stats.getMaxSpeed().modifyMult(id, crawl);
            stats.getAcceleration().modifyMult(id, crawl * 3f);
            stats.getDeceleration().modifyMult(id, 3f);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        super.unapply(stats, id);
        stats.getMaxSpeed().unmodifyMult(id);
        stats.getAcceleration().unmodifyMult(id);
        stats.getDeceleration().unmodifyMult(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && (state == State.IN || state == State.ACTIVE)) {
            return new StatusData("bulwark holding", false);
        }
        return null;
    }
}
