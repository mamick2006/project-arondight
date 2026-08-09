package arondight.weave;

import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import arondight.util.ArondightLog;
import arondight.util.ArondightReflect;

public final class SpecMutators {

    public static final String CAP_CLONE = "clone";
    public static final String CAP_ARMOR = "armor";
    public static final String CAP_FLUX = "flux";
    public static final String CAP_OP = "op";
    public static final String CAP_SLOT = "slot";
    public static final String CAP_ENGINE = "engine";
    public static final String CAP_MOUNT = "mount";

    private static final float EPSILON = 0.5f;
    private static final Set<String> broken = new HashSet<>();

    private SpecMutators() {}

    public static final class MutationFailed extends RuntimeException {
        MutationFailed(String capability, Throwable cause) {
            super(capability, cause);
        }
    }

    public static boolean isWorking(String capability) {
        return ArondightReflect.isWorking() && !broken.contains(capability);
    }

    private static void markBroken(String capability, Throwable cause) {
        broken.add(capability);
        ArondightLog.warnOnce("cap_" + capability, "spec mutation capability '" + capability
                + "' broke (" + cause + "), likely an obfuscation rename in a game update;"
                + " the weave nodes needing it self-disable and SP is never charged for them");
    }

    public static ShipHullSpecAPI cloneSpec(ShipHullSpecAPI canonical) {
        if (!isWorking(CAP_CLONE)) return null;
        ShipHullSpecAPI cloned;
        try {
            Object clone = ArondightReflect.invoke(canonical, "clone");
            if (!(clone instanceof ShipHullSpecAPI) || clone == canonical) {
                throw new IllegalStateException("clone() returned " + clone);
            }
            cloned = (ShipHullSpecAPI) clone;
        } catch (Throwable t) {
            markBroken(CAP_CLONE, t);
            return null;
        }
        isolateEngineSpec(cloned, canonical);
        return cloned;
    }

    private static void isolateEngineSpec(ShipHullSpecAPI clone, ShipHullSpecAPI canonical) {
        if (!isWorking(CAP_ENGINE)) return;
        try {
            ShipHullSpecAPI.EngineSpecAPI canon = canonical.getEngineSpec();
            if (clone.getEngineSpec() != canon) {
                return;
            }
            Object fresh = null;
            try {
                fresh = ArondightReflect.invoke(canon, "clone");
            } catch (Throwable ignore) {
            }
            if (!(fresh instanceof ShipHullSpecAPI.EngineSpecAPI)) {
                fresh = ArondightReflect.construct(canon.getClass(),
                        MethodType.methodType(void.class));
                ShipHullSpecAPI.EngineSpecAPI f = (ShipHullSpecAPI.EngineSpecAPI) fresh;
                f.setMaxSpeed(canon.getMaxSpeed());
                f.setAcceleration(canon.getAcceleration());
                f.setDeceleration(canon.getDeceleration());
                f.setMaxTurnRate(canon.getMaxTurnRate());
                f.setTurnAcceleration(canon.getTurnAcceleration());
            }
            ArondightReflect.invoke(clone, "setEngineSpec", fresh);
            ShipHullSpecAPI.EngineSpecAPI got = clone.getEngineSpec();
            if (got == canon) {
                throw new IllegalStateException("engine sub-spec still shared after rebuild");
            }
            if (Math.abs(got.getMaxSpeed() - canon.getMaxSpeed()) > EPSILON
                    || Math.abs(got.getAcceleration() - canon.getAcceleration()) > EPSILON
                    || Math.abs(got.getDeceleration() - canon.getDeceleration()) > EPSILON
                    || Math.abs(got.getMaxTurnRate() - canon.getMaxTurnRate()) > EPSILON
                    || Math.abs(got.getTurnAcceleration() - canon.getTurnAcceleration()) > EPSILON) {
                throw new IllegalStateException("isolated engine spec failed value verification");
            }
        } catch (Throwable t) {
            markBroken(CAP_ENGINE, t);
        }
    }

    public static void setHullAndArmor(ShipHullSpecAPI clone, float hitpoints, float armor) {
        try {
            Object armorSpec = ArondightReflect.invoke(clone, "getArmorSpec");
            if (!trySubSpec(clone, "setArmorSpec", armorSpec.getClass(), hitpoints, armor)
                    || !verifyHullArmor(clone, hitpoints, armor)) {
                if (!trySubSpec(clone, "setArmorSpec", armorSpec.getClass(), armor, hitpoints)
                        || !verifyHullArmor(clone, hitpoints, armor)) {
                    throw new IllegalStateException("armor sub-spec rebuild failed verification");
                }
            }
        } catch (Throwable t) {
            markBroken(CAP_ARMOR, t);
            throw new MutationFailed(CAP_ARMOR, t);
        }
    }

    public static void setFlux(ShipHullSpecAPI clone, float capacity, float dissipation) {
        try {
            Object reactorSpec = ArondightReflect.invoke(clone, "getReactorSpec");
            if (!trySubSpec(clone, "setReactorSpec", reactorSpec.getClass(), capacity, dissipation)
                    || !verifyFlux(clone, capacity, dissipation)) {
                if (!trySubSpec(clone, "setReactorSpec", reactorSpec.getClass(), dissipation, capacity)
                        || !verifyFlux(clone, capacity, dissipation)) {
                    throw new IllegalStateException("reactor sub-spec rebuild failed verification");
                }
            }
        } catch (Throwable t) {
            markBroken(CAP_FLUX, t);
            throw new MutationFailed(CAP_FLUX, t);
        }
    }

    private static boolean trySubSpec(ShipHullSpecAPI clone, String setter, Class<?> subSpecClass,
                                      float first, float second) {
        try {
            Object fresh = ArondightReflect.construct(subSpecClass,
                    MethodType.methodType(void.class, float.class, float.class), first, second);
            ArondightReflect.invoke(clone, setter, fresh);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean verifyHullArmor(ShipHullSpecAPI spec, float hitpoints, float armor) {
        return Math.abs(spec.getHitpoints() - hitpoints) < EPSILON
                && Math.abs(spec.getArmorRating() - armor) < EPSILON;
    }

    private static boolean verifyFlux(ShipHullSpecAPI spec, float capacity, float dissipation) {
        return Math.abs(spec.getFluxCapacity() - capacity) < EPSILON
                && Math.abs(spec.getFluxDissipation() - dissipation) < EPSILON;
    }

    public static void setOrdnancePoints(ShipHullSpecAPI clone, int op) {
        try {
            ArondightReflect.invoke(clone, "setOrdnancePoints", op);
            if (clone.getOrdnancePoints(null) != op) {
                throw new IllegalStateException("setOrdnancePoints failed verification");
            }
        } catch (Throwable t) {
            markBroken(CAP_OP, t);
            throw new MutationFailed(CAP_OP, t);
        }
    }

    public static void unsealShoulder(ShipHullSpecAPI clone, String slotId) {
        try {
            clone.getBuiltInWeapons().remove(slotId);
            WeaponSlotAPI slot = null;
            for (WeaponSlotAPI s : clone.getAllWeaponSlotsCopy()) {
                if (slotId.equals(s.getId())) {
                    slot = s;
                    break;
                }
            }
            if (slot == null) throw new IllegalStateException("slot " + slotId + " not found on clone");
            ArondightReflect.invoke(slot, "setWeaponType", WeaponAPI.WeaponType.MISSILE);
            if (slot.getWeaponType() != WeaponAPI.WeaponType.MISSILE) {
                throw new IllegalStateException("setWeaponType failed verification");
            }
        } catch (Throwable t) {
            markBroken(CAP_SLOT, t);
            throw new MutationFailed(CAP_SLOT, t);
        }
    }

    public static boolean unhideIfAnimated(ShipHullSpecAPI clone, String slotId, String weaponId) {
        if (clone == null || slotId == null || weaponId == null) return false;
        WeaponSlotAPI target = null;
        WeaponSlotAPI turretDonor = null;
        for (WeaponSlotAPI s : clone.getAllWeaponSlotsCopy()) {
            if (slotId.equals(s.getId())) {
                target = s;
            } else if (turretDonor == null && s.isTurret()) {
                turretDonor = s;
            }
        }
        if (target == null) return false;
        if (!target.isHidden()) return true;
        if (!isAnimatedWeapon(weaponId)) return false;
        if (!isWorking(CAP_MOUNT)) return false;
        try {
            if (turretDonor == null) {
                throw new IllegalStateException("no turret-mount donor slot on " + clone.getHullId());
            }
            Object turretType = ArondightReflect.invoke(turretDonor, "getSlotType");
            ArondightReflect.invoke(target, "setSlotType", turretType);
            if (target.isHidden()) {
                throw new IllegalStateException("setSlotType failed verification on " + slotId);
            }
            return true;
        } catch (Throwable t) {
            markBroken(CAP_MOUNT, t);
            return false;
        }
    }

    static boolean isAnimatedWeapon(String weaponId) {
        Boolean cached = animatedWeapons.get(weaponId);
        if (cached != null) return cached;
        boolean animated = false;
        try {
            com.fs.starfarer.api.loading.WeaponSpecAPI spec =
                    com.fs.starfarer.api.Global.getSettings().getWeaponSpec(weaponId);
            if (spec != null) {
                try {
                    Object frames = ArondightReflect.invoke(spec, "getNumFrames");
                    animated = frames instanceof Number && ((Number) frames).intValue() > 1;
                } catch (Throwable ignore) {
                }
                animated = animated || framedSpriteName(spec.getTurretSpriteName())
                        || framedSpriteName(spec.getHardpointSpriteName());
            }
        } catch (Throwable t) {
        }
        animatedWeapons.put(weaponId, animated);
        return animated;
    }

    private static final java.util.Map<String, Boolean> animatedWeapons =
            new java.util.HashMap<>();

    private static boolean framedSpriteName(String spriteName) {
        return spriteName != null && spriteName.endsWith("00.png");
    }
}
