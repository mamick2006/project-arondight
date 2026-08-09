package arondight;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import arondight.util.ArondightLog;
import arondight.weave.ArondightWeave;
import arondight.weave.WeaveReapplyHooks;

public class ArondightModPlugin extends BaseModPlugin {

    private static final String[] ARMAA_MODS = { "cataphract2", "strikeCraft", "armaa_strikeCraftFrig" };

    @Override
    public void onApplicationLoad() throws Exception {
        if (Global.getSettings().getModManager().isModEnabled("armaa")) {
            for (String hullId : ArondightIds.ALL_FORMS) {
                try {
                    ShipHullSpecAPI spec = Global.getSettings().getHullSpec(hullId);
                    if (spec == null) continue;
                    for (String modId : ARMAA_MODS) {
                        spec.addBuiltInMod(modId);
                    }
                    spec.addTag("independent_of_carrier");
                } catch (Exception e) {
                    ArondightLog.warnOnce("armaa_graft_" + hullId, "armaa graft skipped for missing hull '"
                            + hullId + "' (" + e + ")");
                }
            }
        }
    }

    @Override
    public void beforeGameSave() {
        ArondightWeave.snapshotShoulderFits();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        arondight.ui.ArondightConsolePanel.resetStaticsForLoad();
        arondight.topbar.ArondightMgmtInjector.resetStaticsForLoad();
        arondight.ui.ArondightMgmtScreen.resetStaticsForLoad();

        ArondightWeave.healOnGameLoad();
        ArondightWeave.reapplyPlayerFleetIfNeeded();
        Global.getSector().getListenerManager().addListener(new WeaveReapplyHooks(), true);
        Global.getSector().addTransientScript(new arondight.weave.LoomSalvageScript());

        arondight.campaign.ArondightQuest.onGameLoad();

        arondight.ui.ArondightIntel.ensureExists();
        if (!arondight.util.ArondightSettings.getBoolean(ArondightIds.SET_MGMT_INTEL_ONLY, false)) {
            try {
                if (!Global.getSector().hasTransientScript(arondight.topbar.ArondightMgmtInjector.class)) {
                    Global.getSector().addTransientScript(new arondight.topbar.ArondightMgmtInjector());
                }
            } catch (Throwable ignore) {
            }
        }
    }
}
