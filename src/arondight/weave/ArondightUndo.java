package arondight.weave;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;

public final class ArondightUndo {

    private static final int MAX_DEPTH = 64;

    private static final class Entry {
        final boolean form;
        final String id;
        final String fromForm;
        final boolean wasUnlocked;
        final int points;
        final long bonusXpAdded;
        final long deferredXpAdded;

        Entry(boolean form, String id, String fromForm, boolean wasUnlocked, int points,
                long bonusXpAdded, long deferredXpAdded) {
            this.form = form;
            this.id = id;
            this.fromForm = fromForm;
            this.wasUnlocked = wasUnlocked;
            this.points = points;
            this.bonusXpAdded = bonusXpAdded;
            this.deferredXpAdded = deferredXpAdded;
        }
    }

    private static final List<Entry> STACK = new ArrayList<>();
    private static String stackMemberId;
    private static long stackStamp;
    private static CoreUITabId stackTab;
    private static Watcher watcher;

    private ArondightUndo() {}

    public static void recordWeave(String memberId, String nodeId, int points,
            long bonusXpAdded, long deferredXpAdded) {
        push(memberId, new Entry(false, nodeId, null, false, points, bonusXpAdded,
                deferredXpAdded));
    }

    public static void recordFormSelect(String memberId, String formId, String fromForm,
            boolean wasUnlocked, int points, long bonusXpAdded, long deferredXpAdded) {
        push(memberId, new Entry(true, formId, fromForm, wasUnlocked, points, bonusXpAdded,
                deferredXpAdded));
    }

    private static void push(String memberId, Entry entry) {
        if (memberId == null) return;
        long stamp = now();
        CoreUITabId tab = currentTab();
        if (!STACK.isEmpty() && (stamp != stackStamp || tab != stackTab
                || !memberId.equals(stackMemberId))) {
            clear();
        }
        if (STACK.isEmpty()) {
            stackMemberId = memberId;
            stackStamp = stamp;
            stackTab = tab;
        }
        if (STACK.size() >= MAX_DEPTH) STACK.remove(0);
        STACK.add(entry);
        arm();
    }

    public static String getUndoBlockReason(FleetMemberAPI member) {
        if (member == null || !ArondightState.isFrame(member.getId())) {
            return ArondightStrings.weave("weave_block_no_frame");
        }
        if (liveTop(member.getId()) == null) {
            return ArondightStrings.weave("undo_block_empty");
        }
        if (ConsoleContext.inBattle()) {
            return ArondightStrings.console("console_block_battle");
        }
        if (ConsoleContext.inRefit()) {
            return ArondightStrings.forms("form_block_refit");
        }
        return null;
    }

    public static String getUndoTargetName(FleetMemberAPI member) {
        Entry e = member == null ? null : liveTop(member.getId());
        if (e == null) return null;
        if (e.form) return ArondightTransformer.getFormDisplayName(e.id);
        ArondightNode node = ArondightNodes.byId(e.id);
        return node == null ? e.id : node.getName();
    }

    public static int getUndoRefund(FleetMemberAPI member) {
        Entry e = member == null ? null : liveTop(member.getId());
        return e == null ? 0 : e.points;
    }

    public static String getUndoLandingForm(FleetMemberAPI member) {
        Entry e = member == null ? null : liveTop(member.getId());
        return e == null || !e.form ? null : e.fromForm;
    }

    public static int depthForTest() {
        return STACK.size();
    }

    public static boolean undo(FleetMemberAPI member) {
        if (getUndoBlockReason(member) != null) {
            if (member != null && liveTop(member.getId()) == null) clear();
            return false;
        }
        MutableCharacterStatsAPI stats = playerStats();
        if (stats == null) return false;
        String memberId = member.getId();
        Entry e = STACK.get(STACK.size() - 1);
        if (e.form) {
            ArondightTransformer.TransformResult back =
                    ArondightTransformer.transformUnchecked(member, e.fromForm);
            if (!back.success) {
                clear();
                return false;
            }
            if (!e.wasUnlocked) ArondightState.lockForm(memberId, e.id);
            ArondightState.reduceSpentFormSp(memberId, e.points);
        } else {
            ArondightState.removeWovenNode(memberId, e.id);
            int ledgered = ArondightState.removeSpentNodeSp(memberId, e.id);
            if (ledgered > e.points) {
                ArondightState.addSpentNodeSp(memberId, e.id, ledgered - e.points);
            }
            WeaveApplier.apply(member);
        }
        STACK.remove(STACK.size() - 1);
        stats.addStoryPoints(e.points);
        stats.setBonusXp(Math.max(0L, stats.getBonusXp() - e.bonusXpAdded));
        stats.setDeferredBonusXp(Math.max(0L, stats.getDeferredBonusXp() - e.deferredXpAdded));
        if (STACK.isEmpty()) clear();
        return true;
    }

    public static void clear() {
        STACK.clear();
        stackMemberId = null;
        stackTab = null;
        if (watcher != null) {
            watcher.done = true;
            watcher = null;
        }
    }

    private static Entry liveTop(String memberId) {
        if (STACK.isEmpty() || !memberId.equals(stackMemberId)) return null;
        if (now() != stackStamp || currentTab() != stackTab) return null;
        Entry e = STACK.get(STACK.size() - 1);
        if (e.form) {
            return e.id.equals(ArondightState.getCurrentForm(memberId)) ? e : null;
        }
        String last = null;
        for (String nodeId : ArondightState.getWovenNodes(memberId)) last = nodeId;
        return e.id.equals(last) ? e : null;
    }

    private static void arm() {
        if (watcher != null || Global.getSector() == null) return;
        watcher = new Watcher();
        Global.getSector().addTransientScript(watcher);
    }

    private static long now() {
        if (Global.getSector() == null || Global.getSector().getClock() == null) return 0L;
        return Global.getSector().getClock().getTimestamp();
    }

    private static CoreUITabId currentTab() {
        if (Global.getSector() == null) return null;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        return ui == null ? null : ui.getCurrentCoreTab();
    }

    private static MutableCharacterStatsAPI playerStats() {
        if (Global.getSector() == null || Global.getSector().getPlayerPerson() == null) return null;
        return Global.getSector().getPlayerPerson().getStats();
    }

    private static final class Watcher implements EveryFrameScript {
        private boolean done;

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done) return;
            if (STACK.isEmpty() || now() != stackStamp || currentTab() != stackTab) {
                clear();
            }
        }
    }
}
