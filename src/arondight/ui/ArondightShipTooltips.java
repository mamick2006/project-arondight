package arondight.ui;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import arondight.util.ArondightLog;
import arondight.util.ArondightReflect;

final class ArondightShipTooltips {

  interface TipNote {
    String text();

    boolean isWarn();
  }

  private static final float NOTE_PAD_X = 6f;
  private static final float NOTE_H = 20f;

  private static volatile boolean broken;
  private static MethodHandle createExpanded;

  private ArondightShipTooltips() {
  }

  static boolean isWorking() {
    return !broken;
  }

  static boolean attachExpanded(ButtonAPI hit, FleetMemberAPI member,
      MutableCharacterStatsAPI stats, final TipNote note) {
    if (broken || hit == null || member == null || stats == null) {
      return false;
    }
    try {
      if (createExpanded == null) {
        ClassLoader loader = ArondightShipTooltips.class.getClassLoader();
        Class<?> tipCls = Class.forName("com.fs.starfarer.ui.impl.StandardTooltipV2",
            false, loader);
        Class<?> memberCls = Class.forName("com.fs.starfarer.campaign.fleet.FleetMember",
            false, loader);
        Class<?> statsCls = Class.forName("com.fs.starfarer.campaign.CharacterStats",
            false, loader);
        createExpanded = MethodHandles.publicLookup().findStatic(tipCls,
            "createFleetMemberExpandedTooltip",
            MethodType.methodType(tipCls, memberCls, statsCls));
      }
      final Object tip = createExpanded.invoke(member, stats);
      if (tip == null) {
        throw new IllegalStateException("factory returned null");
      }
      if (note != null) {
        ArondightReflect.invoke(tip, "setBeforeShowing", new Runnable() {
          private Object lastRow;

          @Override
          public void run() {
            try {
              UIPanelAPI panel = (UIPanelAPI) tip;
              if (lastRow != null) {
                panel.removeComponent((com.fs.starfarer.api.ui.UIComponentAPI) lastRow);
                lastRow = null;
              }
              String state = note.text();
              if (state == null || state.isEmpty()) {
                return;
              }
              float w = panel.getPosition().getWidth();
              float h = panel.getPosition().getHeight();
              float rowW = w - 2f * NOTE_PAD_X;
              CustomPanelAPI row = Global.getSettings().createCustom(rowW, NOTE_H, null);
              TooltipMakerAPI el = row.createUIElement(rowW, NOTE_H, false);
              el.addPara(state, note.isWarn() ? ArondightUi.warn() : ArondightUi.gray(), 0f);
              float rowH = Math.max(NOTE_H, el.getHeightSoFar());
              row.getPosition().setSize(rowW, rowH);
              row.addUIElement(el).inTL(0f, 0f);
              panel.getPosition().setSize(w, h + rowH + 2f);
              panel.addComponent(row).inTL(NOTE_PAD_X, h);
              lastRow = row;
            } catch (Throwable t) {
              markBroken(t);
            }
          }
        });
      }
      ArondightReflect.invoke(hit, "setTooltip", 0f, tip);
      return true;
    } catch (Throwable t) {
      markBroken(t);
      return false;
    }
  }

  private static void markBroken(Throwable t) {
    broken = true;
    ArondightLog.warnOnce("native_ship_tooltip", "the native fleet-member hover broke ("
        + t + "), likely an obfuscation rename in a game update; the form tiles fall"
        + " back to a plain card (F2 codex kept) and nothing else is affected");
  }
}
