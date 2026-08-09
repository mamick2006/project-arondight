package arondight.weave.nodes;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.EngineSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import arondight.weave.SpecMutators;

public final class SpecStatNode extends BaseNode {

    public enum Op { HULL_MULT, ARMOR_FLAT, FLUX_CAP_MULT, FLUX_DISS_MULT, SPEED_FLAT, MANEUVER_MULT, OP_FLAT }

    private final Op op;
    private final float amount;

    public SpecStatNode(String id, String name, String description, Cluster cluster,
                        java.util.List<String> prereqAnyOf, Op op, float amount) {
        super(id, name, description, cluster, prereqAnyOf, capabilitiesFor(op));
        this.op = op;
        this.amount = amount;
    }

    private static String[] capabilitiesFor(Op op) {
        switch (op) {
            case HULL_MULT:
            case ARMOR_FLAT:
                return new String[] { SpecMutators.CAP_ARMOR };
            case FLUX_CAP_MULT:
            case FLUX_DISS_MULT:
                return new String[] { SpecMutators.CAP_FLUX };
            case OP_FLAT:
                return new String[] { SpecMutators.CAP_OP };
            default:
                return new String[] { SpecMutators.CAP_ENGINE };
        }
    }

    @Override
    public void mutate(ShipHullSpecAPI spec, ShipVariantAPI variant) {
        switch (op) {
            case HULL_MULT:
                SpecMutators.setHullAndArmor(spec, spec.getHitpoints() * amount, spec.getArmorRating());
                break;
            case ARMOR_FLAT:
                SpecMutators.setHullAndArmor(spec, spec.getHitpoints(), spec.getArmorRating() + amount);
                break;
            case FLUX_CAP_MULT:
                SpecMutators.setFlux(spec, spec.getFluxCapacity() * amount, spec.getFluxDissipation());
                break;
            case FLUX_DISS_MULT:
                SpecMutators.setFlux(spec, spec.getFluxCapacity(), spec.getFluxDissipation() * amount);
                break;
            case SPEED_FLAT: {
                EngineSpecAPI engine = spec.getEngineSpec();
                engine.setMaxSpeed(engine.getMaxSpeed() + amount);
                break;
            }
            case MANEUVER_MULT: {
                EngineSpecAPI engine = spec.getEngineSpec();
                engine.setAcceleration(engine.getAcceleration() * amount);
                engine.setDeceleration(engine.getDeceleration() * amount);
                engine.setMaxTurnRate(engine.getMaxTurnRate() * amount);
                engine.setTurnAcceleration(engine.getTurnAcceleration() * amount);
                break;
            }
            case OP_FLAT:
                SpecMutators.setOrdnancePoints(spec, spec.getOrdnancePoints(null) + (int) amount);
                break;
        }
    }
}
