package arondight.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightIds;

public final class ArondightState {

    private static final String K_MEMBERS = "members";
    private static final String K_QUEST = "quest";
    private static final String QUEST_MGMT_OPENED = "mgmt_opened";
    private static final String K_FORM = "form";
    private static final String K_UNLOCKED = "unlocked";
    private static final String K_WOVEN = "woven";
    private static final String K_RESPECS = "respecs";
    private static final String K_SHOULDER_FITS = "shoulderFits";
    private static final String K_SPENT_SP = "spentNodeSp";
    private static final String K_SPENT_FORM_SP = "spentFormSp";

    private ArondightState() {}

    @SuppressWarnings("unchecked")
    private static Map<String, Object> store(boolean createIfMissing) {
        if (Global.getSector() == null) return null;
        Map<String, Object> data = Global.getSector().getPersistentData();
        Object entry = data.get(ArondightIds.PERSISTENT_KEY);
        if (!(entry instanceof Map)) {
            if (!createIfMissing) return null;
            entry = new HashMap<String, Object>();
            data.put(ArondightIds.PERSISTENT_KEY, entry);
        }
        return (Map<String, Object>) entry;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> members(boolean createIfMissing) {
        Map<String, Object> store = store(createIfMissing);
        if (store == null) return null;
        Object m = store.get(K_MEMBERS);
        if (!(m instanceof Map)) {
            if (!createIfMissing) return null;
            m = new HashMap<String, Map<String, Object>>();
            store.put(K_MEMBERS, m);
        }
        return (Map<String, Map<String, Object>>) m;
    }

    private static Map<String, Object> member(String memberId, boolean createIfMissing) {
        if (memberId == null) return null;
        Map<String, Map<String, Object>> members = members(createIfMissing);
        if (members == null) return null;
        Map<String, Object> m = members.get(memberId);
        if (m == null && createIfMissing) {
            m = new HashMap<>();
            members.put(memberId, m);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> owner, String key, boolean createIfMissing) {
        if (owner == null) return null;
        Object v = owner.get(key);
        if (!(v instanceof List)) {
            if (!createIfMissing) return null;
            v = new ArrayList<String>();
            owner.put(key, v);
        }
        return (List<String>) v;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Boolean> quest(boolean createIfMissing) {
        Map<String, Object> store = store(createIfMissing);
        if (store == null) return null;
        Object q = store.get(K_QUEST);
        if (!(q instanceof Map)) {
            if (!createIfMissing) return null;
            q = new HashMap<String, Boolean>();
            store.put(K_QUEST, q);
        }
        return (Map<String, Boolean>) q;
    }

    public static boolean hasAnyFrame() {
        Map<String, Map<String, Object>> members = members(false);
        return members != null && !members.isEmpty();
    }

    public static boolean exists() {
        return hasAnyFrame();
    }

    public static boolean isMgmtOpened() {
        return getQuestFlag(QUEST_MGMT_OPENED);
    }

    public static void noteMgmtOpened() {
        setQuestFlag(QUEST_MGMT_OPENED, true);
    }

    public static boolean isFrame(String memberId) {
        return member(memberId, false) != null;
    }

    public static List<String> getFrameMemberIds() {
        Map<String, Map<String, Object>> members = members(false);
        if (members == null) return Collections.emptyList();
        return new ArrayList<>(members.keySet());
    }

    public static void createFrameState(String memberId) {
        Map<String, Object> m = member(memberId, true);
        if (m == null) return;
        if (!(m.get(K_FORM) instanceof String)) m.put(K_FORM, ArondightIds.FORM_BASE);
        List<String> unlocked = stringList(m, K_UNLOCKED, true);
        if (!unlocked.contains(ArondightIds.FORM_BASE)) unlocked.add(ArondightIds.FORM_BASE);
        stringList(m, K_WOVEN, true);
        if (!(m.get(K_RESPECS) instanceof Integer)) m.put(K_RESPECS, 0);
    }

    public static boolean moveFrameState(String fromId, String toId) {
        if (fromId == null || toId == null || fromId.equals(toId)) return false;
        Map<String, Map<String, Object>> members = members(false);
        if (members == null || members.containsKey(toId)) return false;
        Map<String, Object> m = members.remove(fromId);
        if (m == null) return false;
        m.remove(K_SHOULDER_FITS);
        members.put(toId, m);
        return true;
    }

    public static void removeFrameState(String memberId) {
        Map<String, Map<String, Object>> members = members(false);
        if (members != null) members.remove(memberId);
    }

    public static FleetMemberAPI findPlayerFrame() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return null;
        for (FleetMemberAPI m : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (isFrame(m.getId())) return m;
        }
        return null;
    }

    public static String getCurrentForm(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m == null) return null;
        Object f = m.get(K_FORM);
        return f instanceof String ? (String) f : ArondightIds.FORM_BASE;
    }

    public static void setCurrentForm(String memberId, String hullId) {
        Map<String, Object> m = member(memberId, true);
        if (m != null) m.put(K_FORM, hullId);
    }

    public static Set<String> getUnlockedForms(String memberId) {
        List<String> unlocked = stringList(member(memberId, false), K_UNLOCKED, false);
        if (unlocked == null) return Collections.emptySet();
        return new LinkedHashSet<>(unlocked);
    }

    public static boolean isFormUnlocked(String memberId, String hullId) {
        List<String> unlocked = stringList(member(memberId, false), K_UNLOCKED, false);
        return unlocked != null && unlocked.contains(hullId);
    }

    public static void lockForm(String memberId, String hullId) {
        List<String> unlocked = stringList(member(memberId, false), K_UNLOCKED, false);
        if (unlocked != null) unlocked.remove(hullId);
    }

    public static void unlockForm(String memberId, String hullId) {
        Map<String, Object> m = member(memberId, true);
        if (m == null) return;
        List<String> unlocked = stringList(m, K_UNLOCKED, true);
        if (!unlocked.contains(hullId)) unlocked.add(hullId);
    }

    public static Set<String> getWovenNodes(String memberId) {
        List<String> woven = stringList(member(memberId, false), K_WOVEN, false);
        if (woven == null) return Collections.emptySet();
        return new LinkedHashSet<>(woven);
    }

    public static int getWovenCount(String memberId) {
        List<String> woven = stringList(member(memberId, false), K_WOVEN, false);
        return woven == null ? 0 : woven.size();
    }

    public static boolean isNodeWoven(String memberId, String nodeId) {
        List<String> woven = stringList(member(memberId, false), K_WOVEN, false);
        return woven != null && woven.contains(nodeId);
    }

    public static void addWovenNode(String memberId, String nodeId) {
        Map<String, Object> m = member(memberId, true);
        if (m == null) return;
        List<String> woven = stringList(m, K_WOVEN, true);
        if (!woven.contains(nodeId)) woven.add(nodeId);
    }

    public static void clearWovenNodes(String memberId) {
        List<String> woven = stringList(member(memberId, false), K_WOVEN, false);
        if (woven != null) woven.clear();
    }

    public static void removeWovenNode(String memberId, String nodeId) {
        List<String> woven = stringList(member(memberId, false), K_WOVEN, false);
        if (woven != null) woven.remove(nodeId);
    }

    public static int getRespecCount(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m == null) return 0;
        Object r = m.get(K_RESPECS);
        return r instanceof Integer ? (Integer) r : 0;
    }

    public static void incrementRespecCount(String memberId) {
        Map<String, Object> m = member(memberId, true);
        if (m != null) m.put(K_RESPECS, getRespecCount(memberId) + 1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> spentNodeLedger(String memberId, boolean createIfMissing) {
        Map<String, Object> m = member(memberId, createIfMissing);
        if (m == null) return null;
        Object v = m.get(K_SPENT_SP);
        if (v instanceof Map) return (Map<String, Integer>) v;
        if (!createIfMissing) return null;
        Map<String, Integer> ledger = new HashMap<>();
        m.put(K_SPENT_SP, ledger);
        return ledger;
    }

    @SuppressWarnings("unchecked")
    public static int getSpentNodeSp(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m == null) return 0;
        Object v = m.get(K_SPENT_SP);
        if (!(v instanceof Map)) return 0;
        int total = 0;
        for (Object amount : ((Map<String, Object>) v).values()) {
            if (amount instanceof Integer) total += (Integer) amount;
        }
        return total;
    }

    public static void addSpentNodeSp(String memberId, String nodeId, int amount) {
        Map<String, Integer> ledger = spentNodeLedger(memberId, true);
        if (ledger == null) return;
        Integer prior = ledger.get(nodeId);
        ledger.put(nodeId, (prior == null ? 0 : prior) + amount);
    }

    public static void clearSpentNodeSp(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m != null) m.remove(K_SPENT_SP);
    }

    public static int removeSpentNodeSp(String memberId, String nodeId) {
        Map<String, Integer> ledger = spentNodeLedger(memberId, false);
        if (ledger == null) return 0;
        Integer amount = ledger.remove(nodeId);
        return amount == null ? 0 : amount;
    }

    public static int getSpentFormSp(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m == null) return 0;
        Object v = m.get(K_SPENT_FORM_SP);
        return v instanceof Integer ? (Integer) v : 0;
    }

    public static void addSpentFormSp(String memberId, int amount) {
        Map<String, Object> m = member(memberId, true);
        if (m != null) m.put(K_SPENT_FORM_SP, getSpentFormSp(memberId) + amount);
    }

    public static void reduceSpentFormSp(String memberId, int amount) {
        int left = getSpentFormSp(memberId) - amount;
        Map<String, Object> m = member(memberId, false);
        if (m == null) return;
        if (left > 0) {
            m.put(K_SPENT_FORM_SP, left);
        } else {
            m.remove(K_SPENT_FORM_SP);
        }
    }

    public static void clearSpentFormSp(String memberId) {
        Map<String, Object> m = member(memberId, false);
        if (m != null) m.remove(K_SPENT_FORM_SP);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> shoulderFits(String memberId, boolean createIfMissing) {
        Map<String, Object> m = member(memberId, createIfMissing);
        if (m == null) return null;
        Object fits = m.get(K_SHOULDER_FITS);
        if (!(fits instanceof Map)) {
            if (!createIfMissing) return null;
            fits = new HashMap<String, String>();
            m.put(K_SHOULDER_FITS, fits);
        }
        return (Map<String, String>) fits;
    }

    public static String getShoulderFit(String memberId, String slotId) {
        Map<String, String> fits = shoulderFits(memberId, false);
        return fits == null ? null : fits.get(slotId);
    }

    public static void setShoulderFit(String memberId, String slotId, String weaponId) {
        if (weaponId == null) {
            Map<String, String> fits = shoulderFits(memberId, false);
            if (fits != null) fits.remove(slotId);
        } else {
            Map<String, String> fits = shoulderFits(memberId, true);
            if (fits != null) fits.put(slotId, weaponId);
        }
    }

    public static void clearShoulderFits(String memberId) {
        Map<String, String> fits = shoulderFits(memberId, false);
        if (fits != null) fits.clear();
    }

    public static boolean getQuestFlag(String flag) {
        Map<String, Boolean> quest = quest(false);
        return quest != null && Boolean.TRUE.equals(quest.get(flag));
    }

    public static void setQuestFlag(String flag, boolean value) {
        Map<String, Boolean> quest = quest(true);
        if (quest != null) quest.put(flag, value);
    }

    public static boolean areTreesUnlocked() {
        return getQuestFlag(ArondightIds.QUEST_M2_COMPLETE);
    }
}
