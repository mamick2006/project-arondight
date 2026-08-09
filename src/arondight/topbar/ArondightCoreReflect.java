package arondight.topbar;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

final class ArondightCoreReflect {

  private ArondightCoreReflect() {
  }

  private static final MethodHandle GET_DECLARED_FIELDS;
  private static final MethodHandle FIELD_GET_NAME;
  private static final MethodHandle FIELD_SET_ACCESSIBLE;
  private static final MethodHandle FIELD_GET;
  private static final MethodHandle FIELD_GET_MODIFIERS;
  private static final MethodHandle GET_DECLARED_METHODS;
  private static final MethodHandle METHOD_GET_NAME;
  private static final MethodHandle METHOD_GET_PARAM_COUNT;
  private static final MethodHandle METHOD_GET_PARAM_TYPES;
  private static final MethodHandle METHOD_SET_ACCESSIBLE;
  private static final MethodHandle METHOD_INVOKE;
  private static final MethodHandle MODIFIER_IS_STATIC;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      ClassLoader sys = ClassLoader.getSystemClassLoader();
      Class<?> fieldCls = Class.forName("java.lang.reflect.Field", false, sys);
      Class<?> fieldArr = Class.forName("[Ljava.lang.reflect.Field;", false, sys);
      Class<?> methodCls = Class.forName("java.lang.reflect.Method", false, sys);
      Class<?> methodArr = Class.forName("[Ljava.lang.reflect.Method;", false, sys);
      Class<?> modifierCls = Class.forName("java.lang.reflect.Modifier", false, sys);
      GET_DECLARED_FIELDS = lookup.findVirtual(Class.class, "getDeclaredFields",
          MethodType.methodType(fieldArr));
      FIELD_GET_NAME = lookup.findVirtual(fieldCls, "getName", MethodType.methodType(String.class));
      FIELD_SET_ACCESSIBLE = lookup.findVirtual(fieldCls, "setAccessible",
          MethodType.methodType(void.class, boolean.class));
      FIELD_GET = lookup.findVirtual(fieldCls, "get", MethodType.methodType(Object.class, Object.class));
      FIELD_GET_MODIFIERS = lookup.findVirtual(fieldCls, "getModifiers", MethodType.methodType(int.class));
      GET_DECLARED_METHODS = lookup.findVirtual(Class.class, "getDeclaredMethods",
          MethodType.methodType(methodArr));
      METHOD_GET_NAME = lookup.findVirtual(methodCls, "getName", MethodType.methodType(String.class));
      METHOD_GET_PARAM_COUNT = lookup.findVirtual(methodCls, "getParameterCount",
          MethodType.methodType(int.class));
      METHOD_GET_PARAM_TYPES = lookup.findVirtual(methodCls, "getParameterTypes",
          MethodType.methodType(Class[].class));
      METHOD_SET_ACCESSIBLE = lookup.findVirtual(methodCls, "setAccessible",
          MethodType.methodType(void.class, boolean.class));
      METHOD_INVOKE = lookup.findVirtual(methodCls, "invoke",
          MethodType.methodType(Object.class, Object.class, Object[].class));
      MODIFIER_IS_STATIC = lookup.findStatic(modifierCls, "isStatic",
          MethodType.methodType(boolean.class, int.class));
    } catch (Throwable t) {
      throw new RuntimeException("arondight: core-tab reflection bootstrap failed", t);
    }
  }

  private static Object coreField;
  private static Class<?> coreFieldClass;
  private static Object childrenMethod;
  private static Class<?> childrenClass;

  private static final Object METHOD_ABSENT = new Object();
  private static final Map<String, Object> METHOD_CACHE = new HashMap<String, Object>();
  private static Class<?> campaignEngineClass;

  static UIPanelAPI getCore() throws Exception {
    SectorAPI sector = Global.getSector();
    if (sector == null) {
      return null;
    }
    CampaignUIAPI ui = sector.getCampaignUI();
    if (ui == null) {
      return null;
    }
    Object f = coreFieldFor(ui.getClass());
    Object core;
    try {
      core = FIELD_GET.invoke(f, ui);
    } catch (Throwable t) {
      throw asException(t);
    }
    if (core == null) {
      return null;
    }
    return (UIPanelAPI) core;
  }

  static UIPanelAPI getHostCore() throws Exception {
    SectorAPI sector = Global.getSector();
    if (sector == null) {
      return null;
    }
    CampaignUIAPI ui = sector.getCampaignUI();
    if (ui == null) {
      return null;
    }
    Object dialog = invokeNoArgOrNull("getEncounterDialog", ui);
    if (dialog != null) {
      Object dialogCore = invokeNoArgOrNull("getCoreUI", dialog);
      if (dialogCore instanceof UIPanelAPI) {
        return (UIPanelAPI) dialogCore;
      }
    }
    return getCore();
  }

  static boolean isChild(UIPanelAPI core, UIComponentAPI panel) throws Exception {
    if (core == null || panel == null) {
      return false;
    }
    for (Object child : childrenList(core)) {
      if (child == panel) {
        return true;
      }
    }
    return false;
  }

  static List<?> childrenList(UIPanelAPI panel) throws Exception {
    if (panel == null) {
      return java.util.Collections.emptyList();
    }
    Object m = childrenMethodFor(panel.getClass());
    Object result;
    try {
      result = METHOD_INVOKE.invoke(m, panel, new Object[0]);
    } catch (Throwable t) {
      throw asException(t);
    }
    if (!(result instanceof List)) {
      throw new NoSuchMethodException("arondight: children accessor returned "
          + (result == null ? "null" : result.getClass().getName()) + ", not a List");
    }
    return (List<?>) result;
  }

  static Object getCurrentTab(UIPanelAPI core) throws Exception {
    if (core == null) {
      return null;
    }
    return invokeNoArg("getCurrentTab", core);
  }

  static Object invokeNoArg(String name, Object target) throws Exception {
    if (target == null) {
      return null;
    }
    Object m = findMethodCached(target.getClass(), name, 0);
    if (m == null) {
      throw new NoSuchMethodException("arondight: no " + name + "() on " + target.getClass().getName());
    }
    try {
      return METHOD_INVOKE.invoke(m, target, new Object[0]);
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  static Object invokeOneArg(String name, Object target, Object arg) throws Exception {
    if (target == null) {
      return null;
    }
    Object m = findMethodCached(target.getClass(), name, 1);
    if (m == null) {
      throw new NoSuchMethodException("arondight: no " + name + "(x) on " + target.getClass().getName());
    }
    try {
      return METHOD_INVOKE.invoke(m, target, new Object[] { arg });
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  private static Object invokeNoArgOrNull(String name, Object target) throws Exception {
    if (target == null) {
      return null;
    }
    Object m = findMethodCached(target.getClass(), name, 0);
    if (m == null) {
      return null;
    }
    try {
      return METHOD_INVOKE.invoke(m, target, new Object[0]);
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  static Object intelTabData() throws Exception {
    if (campaignEngineClass == null) {
      campaignEngineClass = Class.forName("com.fs.starfarer.campaign.CampaignEngine");
    }
    Object getInstance = findMethodCached(campaignEngineClass, "getInstance", 0);
    if (getInstance == null) {
      throw new NoSuchMethodException("arondight: CampaignEngine.getInstance() not found");
    }
    Object engine;
    try {
      engine = METHOD_INVOKE.invoke(getInstance, null, new Object[0]);
    } catch (Throwable t) {
      throw asException(t);
    }
    if (engine == null) {
      return null;
    }
    Object uiData = invokeNoArg("getUIData", engine);
    if (uiData == null) {
      return null;
    }
    return invokeNoArg("getIntelData", uiData);
  }

  static int getSelectedTabIndex() throws Exception {
    Object itd = intelTabData();
    if (itd == null) {
      return -1;
    }
    Object r = invokeNoArg("getSelectedTabIndex", itd);
    return r instanceof Integer ? ((Integer) r).intValue() : -1;
  }

  static void setSelectedTabIndex(int index) throws Exception {
    Object itd = intelTabData();
    if (itd == null) {
      return;
    }
    invokeOneArg("setSelectedTabIndex", itd, Integer.valueOf(index));
  }

  private static Object coreFieldFor(Class<?> cls) throws Exception {
    if (coreField != null && coreFieldClass == cls) {
      return coreField;
    }
    Object f = findField(cls, "core");
    if (f == null) {
      throw new NoSuchFieldException("arondight: campaign 'core' field not found on " + cls.getName());
    }
    coreField = f;
    coreFieldClass = cls;
    return f;
  }

  private static Object childrenMethodFor(Class<?> cls) throws Exception {
    if (childrenMethod != null && childrenClass == cls) {
      return childrenMethod;
    }
    Object m = findMethod(cls, "getChildrenNonCopy");
    if (m == null) {
      m = findMethod(cls, "getChildrenCopy");
    }
    if (m == null) {
      throw new NoSuchMethodException("arondight: no children accessor on " + cls.getName());
    }
    childrenMethod = m;
    childrenClass = cls;
    return m;
  }

  private static Object findField(Class<?> cls, String name) throws Exception {
    try {
      for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
        Object[] fields = (Object[]) GET_DECLARED_FIELDS.invoke(c);
        for (Object f : fields) {
          if (name.equals(FIELD_GET_NAME.invoke(f))) {
            FIELD_SET_ACCESSIBLE.invoke(f, true);
            return f;
          }
        }
      }
      return null;
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  private static Object findMethod(Class<?> cls, String name) throws Exception {
    return findMethod(cls, name, 0);
  }

  private static Object findMethod(Class<?> cls, String name, int paramCount) throws Exception {
    try {
      for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
        Object[] methods = (Object[]) GET_DECLARED_METHODS.invoke(c);
        for (Object m : methods) {
          if (name.equals(METHOD_GET_NAME.invoke(m))
              && (int) METHOD_GET_PARAM_COUNT.invoke(m) == paramCount) {
            METHOD_SET_ACCESSIBLE.invoke(m, true);
            return m;
          }
        }
      }
      return null;
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  private static Object findMethodCached(Class<?> cls, String name, int paramCount) throws Exception {
    String key = cls.getName() + '#' + name + '#' + paramCount;
    Object cached = METHOD_CACHE.get(key);
    if (cached == METHOD_ABSENT) {
      return null;
    }
    if (cached != null) {
      return cached;
    }
    Object m = findMethod(cls, name, paramCount);
    METHOD_CACHE.put(key, m == null ? METHOD_ABSENT : m);
    return m;
  }

  static Object methodNamed(Class<?> cls, String name, int paramCount) throws Exception {
    return findMethodCached(cls, name, paramCount);
  }

  static Class<?>[] paramTypes(Object method) throws Exception {
    try {
      return (Class<?>[]) METHOD_GET_PARAM_TYPES.invoke(method);
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  static Object invokeMethod(Object method, Object target, Object[] args) throws Exception {
    try {
      return METHOD_INVOKE.invoke(method, target, args);
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  static Object samProxy(Class<?> iface, MethodHandle impl) {
    return MethodHandleProxies.asInterfaceInstance(iface.asSubclass(Object.class), impl);
  }

  static List<Integer> instanceIntFieldValues(Object target) throws Exception {
    List<Integer> out = new ArrayList<Integer>();
    if (target == null) {
      return out;
    }
    try {
      for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
        Object[] fields = (Object[]) GET_DECLARED_FIELDS.invoke(c);
        for (Object f : fields) {
          int mods = (int) FIELD_GET_MODIFIERS.invoke(f);
          if ((boolean) MODIFIER_IS_STATIC.invoke(mods)) {
            continue;
          }
          FIELD_SET_ACCESSIBLE.invoke(f, true);
          Object v = FIELD_GET.invoke(f, target);
          if (v instanceof Integer) {
            out.add((Integer) v);
          }
        }
      }
      return out;
    } catch (Throwable t) {
      throw asException(t);
    }
  }

  private static Exception asException(Throwable t) {
    return t instanceof Exception ? (Exception) t : new RuntimeException(t);
  }
}
