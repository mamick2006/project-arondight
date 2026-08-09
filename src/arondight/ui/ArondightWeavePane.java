package arondight.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import arondight.ArondightStrings;
import arondight.form.ArondightTransformer;
import arondight.state.ArondightState;
import arondight.util.ArondightSettings;
import arondight.weave.ArondightNode;
import arondight.weave.ArondightWeave;

public final class ArondightWeavePane {

  public static final String BTN_NODE_PREFIX = "arondight_btn_weave_";
  public static final String BTN_RESPEC = "arondight_btn_respec";

  public static final String ICON_CATEGORY = "arondight_nodes";

  private static final float PAD = 10f;
  private static final float ICON = 32f;
  private static final float NODE_W = 44f;
  private static final float NODE_H = 44f;
  private static final float SCROLL_GUTTER = 16f;
  private static final float HEADER_H = 32f;

  private ArondightWeavePane() {
  }

  public static String nodeName(String nodeId) {
    ArondightNode n = ArondightWeave.getNode(nodeId);
    return n != null && n.getName() != null ? n.getName() : nodeId;
  }

  public static void add(CustomPanelAPI host, float x, float w, float h,
      FleetMemberAPI member, String memberId) {
    CustomPanelAPI pane = host.createCustomPanel(w, h, ArondightUi.consoleRow());
    host.addComponent(pane).inTL(x, 0f);

    TooltipMakerAPI head = pane.createUIElement(w - 2f * PAD, 24f, false);
    head.addSectionHeading("The Weave", Alignment.MID, 0f);
    pane.addUIElement(head).inTL(PAD, 4f);

    List<ArondightNode> all = ArondightWeave.getAllNodes();
    if (all.isEmpty()) {
      TooltipMakerAPI empty = pane.createUIElement(w - 2f * PAD, 30f, false);
      empty.addPara(ArondightStrings.weave("weave_empty"), ArondightUi.gray(), 4f);
      pane.addUIElement(empty).inTL(PAD, HEADER_H + 2f);
      return;
    }
    String rootId = ArondightWeave.getRootNodeId();
    Map<String, List<String>> childrenOf = new HashMap<String, List<String>>();
    for (ArondightNode n : all) {
      childrenOf.put(n.getId(), ArondightWeave.getChildNodeIds(n.getId()));
    }
    buildRadial(pane, w, h, rootId, childrenOf, all, member, memberId);
  }

  private static TooltipMakerAPI liveScroller;

  public static TooltipMakerAPI liveWeaveScrollerForTest() {
    return liveScroller;
  }

  static void dropLiveScroller() {
    liveScroller = null;
  }

  public static float driveWeaveScrollForTest(float yOffset) {
    TooltipMakerAPI el = liveScroller;
    if (el == null) {
      return Float.NaN;
    }
    try {
      com.fs.starfarer.api.ui.ScrollPanelAPI sp = el.getExternalScroller();
      if (sp == null) {
        return 0f;
      }
      sp.setYOffset(yOffset);
      return sp.getYOffset();
    } catch (Throwable t) {
      return Float.NaN;
    }
  }

  private static Color weaveEdgeColor(ArondightNode node, FleetMemberAPI member,
      String memberId) {
    if (memberId != null && ArondightState.isNodeWoven(memberId, node.getId())) {
      return ArondightUi.good();
    }
    if (node.isAvailable()
        && ArondightWeave.getWeaveBlockReason(member, node.getId()) == null) {
      return ArondightUi.selectWhite();
    }
    return new Color(70, 78, 82);
  }

  private static void addNodeBox(CustomPanelAPI canvas, float x, float y, float w, float h,
      ArondightNode node, FleetMemberAPI member, String memberId) {
    boolean woven = memberId != null && ArondightState.isNodeWoven(memberId, node.getId());
    boolean actionable = false;
    if (!woven && node.isAvailable()) {
      actionable = ArondightWeave.getWeaveBlockReason(member, node.getId()) == null;
    }
    ArondightNodeChrome.State state = woven ? ArondightNodeChrome.State.ACTIVATED
        : actionable ? ArondightNodeChrome.State.ACTIVATABLE
            : ArondightNodeChrome.State.LOCKED;
    CustomPanelAPI box = canvas.createCustomPanel(w, h,
        new ArondightNodeChrome(state, ICON_CATEGORY, node.getId(), ICON));
    canvas.addComponent(box).inTL(x, y);

    if (state == ArondightNodeChrome.State.LOCKED) {
      CustomPanelAPI dim = box.createCustomPanel(w, h, new ArondightDimLayer());
      box.addComponent(dim).inTL(0f, 0f);
    }

    TooltipMakerAPI overlay = box.createUIElement(w, h, false);
    ButtonAPI hit = overlay.addButton("", BTN_NODE_PREFIX + node.getId(),
        ArondightUi.CLEAR, ArondightUi.CLEAR, Alignment.MID, null, w, h, 0f);
    ArondightUi.setEnabledSayingWhy(hit, actionable);
    overlay.addTooltipToPrevious(nodeTip(node.getId()),
        TooltipMakerAPI.TooltipLocation.LEFT, false);
    box.addUIElement(overlay).inTL(0f, 0f);
  }

  private static final float ROOT_BOX = 48f;
  private static final float ENTRY_GAP = 22f;
  private static final float AXIS_GAP = 26f;
  private static final float RADIAL_HGAP = 20f;
  private static final float LABEL_BAND = 16f;
  private static final float CORNER_MARGIN = 10f;
  private static final float DIAG_STEP_MAX = 96f;

  private static final int Q_NW = 0;
  private static final int Q_NE = 1;
  private static final int Q_SW = 2;
  private static final int Q_SE = 3;

  private static int quadIndex(ArondightWeave.Quadrant q) {
    switch (q) {
      case NW: return Q_NW;
      case NE: return Q_NE;
      case SW: return Q_SW;
      default: return Q_SE;
    }
  }

  private static String quadrantLabel(int quadrant) {
    switch (quadrant) {
      case Q_NW: return ArondightWeave.Quadrant.NW.getLabel();
      case Q_NE: return ArondightWeave.Quadrant.NE.getLabel();
      case Q_SW: return ArondightWeave.Quadrant.SW.getLabel();
      default: return ArondightWeave.Quadrant.SE.getLabel();
    }
  }

  private static void buildRadial(CustomPanelAPI pane, float w, float h, String rootId,
      Map<String, List<String>> childrenOf, List<ArondightNode> all,
      FleetMemberAPI member, String memberId) {
    float canvasW = w - 2f * PAD - SCROLL_GUTTER;
    float viewH = h - HEADER_H - 4f;

    String[] entryByQuad = new String[4];
    List<String> rootKids = childrenOf.get(rootId);
    if (rootKids != null) {
      for (String kid : rootKids) {
        ArondightWeave.Quadrant q = ArondightWeave.getQuadrant(kid);
        if (q != null) {
          entryByQuad[quadIndex(q)] = kid;
        }
      }
    }
    int maxD = 0;
    for (int q = 0; q < 4; q++) {
      if (entryByQuad[q] == null) {
        continue;
      }
      Map<String, Integer> ds = new HashMap<String, Integer>();
      depth(entryByQuad[q], childrenOf, 0, ds);
      for (Integer d : ds.values()) {
        maxD = Math.max(maxD, d);
      }
    }
    float half = viewH / 2f - LABEL_BAND;
    float fixed = ROOT_BOX / 2f + ENTRY_GAP + NODE_H;
    float lstep = maxD <= 0 ? 0f
        : Math.max(NODE_H + 8f, Math.min(NODE_H + 44f, (half - fixed) / maxD));
    float inkH = 2f * (fixed + maxD * lstep + LABEL_BAND);
    boolean scrolls = inkH > viewH + 0.5f;
    float canvasH = scrolls ? inkH : viewH;
    float cx = canvasW / 2f;
    float cy = canvasH / 2f;

    Map<String, float[]> rects = new HashMap<String, float[]>();
    rects.put(rootId, new float[] { cx - ROOT_BOX / 2f, cy - ROOT_BOX / 2f });
    for (int q = 0; q < 4; q++) {
      if (entryByQuad[q] != null) {
        layQuadrant(q, entryByQuad[q], childrenOf, cx, cy, lstep, rects);
      }
    }
    float orphanX = 4f;
    for (ArondightNode n : all) {
      if (!n.getId().equals(rootId) && !rects.containsKey(n.getId())) {
        rects.put(n.getId(), new float[] { orphanX, canvasH - LABEL_BAND - NODE_H });
        orphanX += NODE_W + 8f;
      }
    }

    ArondightGraphLayer graph = new ArondightGraphLayer();
    CustomPanelAPI canvas = pane.createCustomPanel(canvasW, canvasH, graph);
    addQuadrantLabels(canvas, canvasW, canvasH);

    for (int q = 0; q < 4; q++) {
      String entry = entryByQuad[q];
      if (entry == null) {
        continue;
      }
      boolean north = q == Q_NW || q == Q_NE;
      boolean east = q == Q_NE || q == Q_SE;
      ArondightNode en = ArondightWeave.getNode(entry);
      float[] er = rects.get(entry);
      if (en == null || er == null) {
        continue;
      }
      float exitX = cx + (east ? 1f : -1f) * ROOT_BOX / 4f;
      float exitY = north ? cy - ROOT_BOX / 2f : cy + ROOT_BOX / 2f;
      float targetY = north ? er[1] + NODE_H : er[1];
      graph.addElbow(exitX, exitY, er[0] + NODE_W / 2f, targetY, (exitY + targetY) / 2f,
          weaveEdgeColor(en, member, memberId));
      addQuadEdges(graph, q, entry, childrenOf, rects, member, memberId);
    }

    for (ArondightNode n : all) {
      if (n.getId().equals(rootId)) {
        continue;
      }
      float[] r = rects.get(n.getId());
      if (r == null) {
        continue;
      }
      addNodeBox(canvas, r[0], r[1], NODE_W, NODE_H, n, member, memberId);
    }
    addRootBox(canvas, rects.get(rootId), rootId);

    TooltipMakerAPI view = pane.createUIElement(w - 2f * PAD, viewH, scrolls);
    view.addCustom(canvas, 0f);
    pane.addUIElement(view).inTL(PAD, HEADER_H);
    liveScroller = view;
  }

  private static void addRootBox(CustomPanelAPI canvas, float[] r, String rootId) {
    if (r == null) {
      return;
    }
    CustomPanelAPI box = canvas.createCustomPanel(ROOT_BOX, ROOT_BOX,
        new ArondightNodeChrome(ArondightNodeChrome.State.ACTIVATED, ICON_CATEGORY,
            rootId, ICON));
    canvas.addComponent(box).inTL(r[0], r[1]);
    ArondightNode node = ArondightWeave.getNode(rootId);
    final String name = node != null && node.getName() != null ? node.getName()
        : "Morphogenic Root";
    final String desc = node != null ? node.getDescription() : null;
    TooltipMakerAPI overlay = box.createUIElement(ROOT_BOX, ROOT_BOX, false);
    ButtonAPI hit = overlay.addButton("", "arondight_btn_noop_root",
        ArondightUi.CLEAR, ArondightUi.CLEAR, Alignment.MID, null, ROOT_BOX, ROOT_BOX, 0f);
    ArondightUi.setEnabledSayingWhy(hit, false);
    overlay.addTooltipToPrevious(new ArondightUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        t.addTitle(name);
        if (desc != null && !desc.isEmpty()) {
          ArondightUi.addHlPara(t, desc, 10f);
        }
        t.addPara(ArondightStrings.weave("weave_root_hover"), ArondightUi.gray(), 10f);
      }
    }, TooltipMakerAPI.TooltipLocation.LEFT, false);
    box.addUIElement(overlay).inTL(0f, 0f);
  }

  private static void layQuadrant(int quadrant, String entry,
      Map<String, List<String>> childrenOf, float cx, float cy, float lstep,
      Map<String, float[]> out) {
    boolean north = quadrant == Q_NW || quadrant == Q_NE;
    boolean east = quadrant == Q_NE || quadrant == Q_SE;
    Map<String, Float> cols = new HashMap<String, Float>();
    float widthUnits = tidy(entry, childrenOf, 0f, cols);
    float unit = NODE_W + RADIAL_HGAP;
    float blockW = widthUnits * unit - RADIAL_HGAP;
    float innerX = east ? cx + AXIS_GAP : cx - AXIS_GAP - blockW;
    Map<String, Integer> depths = new HashMap<String, Integer>();
    depth(entry, childrenOf, 0, depths);
    int maxDq = 0;
    for (Integer d : depths.values()) {
      maxDq = Math.max(maxDq, d);
    }
    float spare = cx - CORNER_MARGIN - AXIS_GAP - blockW;
    float diag = maxDq <= 0 ? 0f
        : Math.max(0f, Math.min(DIAG_STEP_MAX, spare / maxDq));
    for (Map.Entry<String, Float> e : cols.entrySet()) {
      Integer d = depths.get(e.getKey());
      if (d == null) {
        continue;
      }
      float x = innerX + e.getValue() * unit + (east ? 1f : -1f) * d * diag;
      float yTop = north ? cy - ROOT_BOX / 2f - ENTRY_GAP - NODE_H - d * lstep
          : cy + ROOT_BOX / 2f + ENTRY_GAP + d * lstep;
      out.put(e.getKey(), new float[] { x, yTop });
    }
  }

  private static float tidy(String id, Map<String, List<String>> childrenOf, float col,
      Map<String, Float> cols) {
    List<String> kids = childrenOf.get(id);
    if (kids == null || kids.isEmpty()) {
      cols.put(id, col);
      return 1f;
    }
    float w = 0f;
    for (String k : kids) {
      w += tidy(k, childrenOf, col + w, cols);
    }
    cols.put(id, (cols.get(kids.get(0)) + cols.get(kids.get(kids.size() - 1))) / 2f);
    return w;
  }

  private static void depth(String id, Map<String, List<String>> childrenOf, int d,
      Map<String, Integer> depths) {
    depths.put(id, d);
    List<String> kids = childrenOf.get(id);
    if (kids != null) {
      for (String k : kids) {
        depth(k, childrenOf, d + 1, depths);
      }
    }
  }

  private static void addQuadEdges(ArondightGraphLayer graph, int quadrant, String entry,
      Map<String, List<String>> childrenOf, Map<String, float[]> rects,
      FleetMemberAPI member, String memberId) {
    boolean north = quadrant == Q_NW || quadrant == Q_NE;
    Map<String, Integer> depths = new HashMap<String, Integer>();
    depth(entry, childrenOf, 0, depths);
    int maxD = 0;
    for (Integer d : depths.values()) {
      maxD = Math.max(maxD, d);
    }
    for (int d = 0; d <= maxD; d++) {
      List<float[]> edges = new ArrayList<float[]>();
      List<Color> colors = new ArrayList<Color>();
      for (Map.Entry<String, Integer> pd : depths.entrySet()) {
        if (pd.getValue() != d) {
          continue;
        }
        float[] pr = rects.get(pd.getKey());
        List<String> kids = childrenOf.get(pd.getKey());
        if (pr == null || kids == null || kids.isEmpty()) {
          continue;
        }
        List<String> placed = new ArrayList<String>();
        for (String kid : kids) {
          if (rects.get(kid) != null && ArondightWeave.getNode(kid) != null) {
            placed.add(kid);
          }
        }
        for (int i = 1; i < placed.size(); i++) {
          String k = placed.get(i);
          int j = i - 1;
          while (j >= 0 && rects.get(placed.get(j))[0] > rects.get(k)[0]) {
            placed.set(j + 1, placed.get(j));
            j--;
          }
          placed.set(j + 1, k);
        }
        int k = placed.size();
        for (int j = 0; j < k; j++) {
          float[] kr = rects.get(placed.get(j));
          float exitX = pr[0] + NODE_W / 2f
              + (k > 1 ? (j / (float) (k - 1) - 0.5f) * NODE_W / 2f : 0f);
          float y1 = north ? pr[1] : pr[1] + NODE_H;
          float y2 = north ? kr[1] + NODE_H : kr[1];
          edges.add(new float[] { exitX, kr[0] + NODE_W / 2f, y1, y2 });
          colors.add(weaveEdgeColor(ArondightWeave.getNode(placed.get(j)), member, memberId));
        }
      }
      emitLanedEdges(graph, edges, colors);
    }
  }

  private static void emitLanedEdges(ArondightGraphLayer graph, List<float[]> edges,
      List<Color> colors) {
    int n = edges.size();
    boolean[] laned = new boolean[n];
    for (int i = 0; i < n; i++) {
      if (laned[i]) {
        continue;
      }
      List<Integer> cluster = new ArrayList<Integer>();
      cluster.add(i);
      laned[i] = true;
      boolean grew = true;
      while (grew) {
        grew = false;
        for (int j = 0; j < n; j++) {
          if (laned[j]) {
            continue;
          }
          for (int c = 0; c < cluster.size(); c++) {
            if (spansOverlap(edges.get(cluster.get(c)), edges.get(j))) {
              cluster.add(j);
              laned[j] = true;
              grew = true;
              break;
            }
          }
        }
      }
      for (int a = 1; a < cluster.size(); a++) {
        int e = cluster.get(a);
        int b = a - 1;
        while (b >= 0 && spanWidth(edges.get(cluster.get(b))) < spanWidth(edges.get(e))) {
          cluster.set(b + 1, cluster.get(b));
          b--;
        }
        cluster.set(b + 1, e);
      }
      int m = cluster.size();
      for (int a = 0; a < m; a++) {
        float[] e = edges.get(cluster.get(a));
        float parentY = e[2];
        float childY = e[3];
        float midY = m == 1 ? (parentY + childY) / 2f
            : parentY + (childY - parentY) * (a + 1) / (m + 1);
        graph.addElbow(e[0], parentY, e[1], childY, midY, colors.get(cluster.get(a)));
      }
    }
  }

  private static float spanWidth(float[] edge) {
    return Math.abs(edge[1] - edge[0]);
  }

  private static boolean spansOverlap(float[] a, float[] b) {
    float aLo = Math.min(a[0], a[1]);
    float aHi = Math.max(a[0], a[1]);
    float bLo = Math.min(b[0], b[1]);
    float bHi = Math.max(b[0], b[1]);
    return aLo <= bHi && bLo <= aHi;
  }

  private static void addQuadrantLabels(CustomPanelAPI canvas, float w, float h) {
    float colW = w / 2f - 12f;
    addCornerLabel(canvas, 6f, 2f, colW, Alignment.LMID, quadrantLabel(Q_NW));
    addCornerLabel(canvas, w / 2f + 6f, 2f, colW, Alignment.RMID, quadrantLabel(Q_NE));
    addCornerLabel(canvas, 6f, h - 16f, colW, Alignment.LMID, quadrantLabel(Q_SW));
    addCornerLabel(canvas, w / 2f + 6f, h - 16f, colW, Alignment.RMID, quadrantLabel(Q_SE));
  }

  private static void addCornerLabel(CustomPanelAPI canvas, float x, float y, float w,
      Alignment align, String text) {
    TooltipMakerAPI el = canvas.createUIElement(w, 14f, false);
    el.setParaFont("graphics/fonts/victor10.fnt");
    LabelAPI l = el.addPara(text, 1f);
    l.setAlignment(align);
    l.setColor(ArondightUi.gray());
    el.setParaFontDefault();
    canvas.addUIElement(el).inTL(x, y);
  }

  private static ArondightUi.Tip nodeTip(final String nodeId) {
    return new ArondightUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        ArondightNode node = ArondightWeave.getNode(nodeId);
        if (node == null) {
          t.addPara(nodeId, ArondightUi.gray(), 0f);
          return;
        }
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        String memberId = member != null ? member.getId() : null;
        if (memberId == null) {
          List<String> ids = ArondightState.getFrameMemberIds();
          memberId = ids.isEmpty() ? null : ids.get(0);
        }
        boolean woven = memberId != null && ArondightState.isNodeWoven(memberId, nodeId);
        t.addTitle(node.getName());
        if (woven) {
          t.addPara(ArondightStrings.weave("weave_hover_woven"), ArondightUi.gray(), 10f);
        } else if (!node.isAvailable()) {
          t.addPara(ArondightStrings.weave("weave_hover_unavailable"),
              ArondightUi.warn(), 10f);
        } else {
          String reason = ArondightWeave.getWeaveBlockReason(member, nodeId);
          if (reason != null) {
            t.addPara(reason, ArondightUi.warn(), 10f);
          } else {
            t.addPara(ArondightStrings.weave("weave_hover_click"),
                ArondightUi.gray(), 10f);
          }
        }
        ArondightUi.addHlPara(t, node.getDescription(), 10f);
      }
    };
  }

  static ArondightUi.Tip respecTip() {
    return new ArondightUi.Tip() {
      @Override
      public void createTooltip(TooltipMakerAPI t, boolean expanded, Object param) {
        FleetMemberAPI member = ArondightState.findPlayerFrame();
        t.addTitle("Respec");
        List<String> relock = ArondightWeave.getRespecRelockForms(member);
        if (relock.isEmpty()) {
          t.addPara(ArondightStrings.weave("respec_hover_unwind"),
              ArondightUi.text(), 10f);
        } else {
          StringBuilder names = new StringBuilder();
          for (int i = 0; i < relock.size(); i++) {
            if (i > 0) {
              names.append(", ");
            }
            names.append(ArondightTransformer.getFormDisplayName(relock.get(i)));
          }
          t.addPara(ArondightStrings.weave("respec_hover_unwind_relock", names),
              ArondightUi.text(), 10f);
        }
        String landing = ArondightWeave.getRespecLandingForm(member);
        if (landing != null) {
          String landingName = ArondightTransformer.getFormDisplayName(landing);
          ArondightUi.addHlPara(t, ArondightStrings.console("respec_landing", landingName),
              6f, landingName);
        }
        int refund = ArondightWeave.getRespecRefund(member);
        String spentOn = relock.isEmpty() ? ArondightStrings.weave("respec_spent_on_nodes")
            : ArondightStrings.weave("respec_spent_on_nodes_and_forms");
        if (refund > 0) {
          ArondightUi.addHlPara(t, refund == 1
              ? ArondightStrings.weave("respec_hover_refund_one", refund, spentOn)
              : ArondightStrings.weave("respec_hover_refund_many", refund, spentOn),
              6f, "" + refund);
        } else {
          t.addPara(ArondightStrings.weave("respec_hover_refund_zero", spentOn),
              ArondightUi.text(), 6f);
        }
        int woven = member == null ? 0 : ArondightState.getWovenCount(member.getId());
        if (woven > 0) {
          t.addPara(ArondightStrings.weave("respec_hover_shoulders"),
              ArondightUi.text(), 6f);
        }
        if (landing != null || woven > 0) {
          t.addPara(woven > 0
              ? ArondightStrings.weave("respec_hover_op_strip_as_well")
              : ArondightStrings.weave("respec_hover_op_strip"),
              ArondightUi.text(), 6f);
        }
        String reason = ArondightWeave.getRespecBlockReason(member);
        if (reason != null) {
          t.addPara(reason, ArondightUi.warn(), 10f);
        }
        t.addStoryPointUseInfo(10f, ArondightSettings.respecSpCost(),
            ArondightSettings.respecBonusXp(), true);
      }
    };
  }
}
