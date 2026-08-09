package arondight.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ArondightReflect {

    private static MethodHandle methodGetName;
    private static MethodHandle methodInvoke;
    private static MethodHandle methodParamCount;
    private static boolean bootFailed = false;

    static {
        try {
            Class<?> methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            methodGetName = lookup.findVirtual(methodClass, "getName",
                    MethodType.methodType(String.class));
            methodInvoke = lookup.findVirtual(methodClass, "invoke",
                    MethodType.methodType(Object.class, Object.class, Object[].class));
            methodParamCount = lookup.findVirtual(methodClass, "getParameterCount",
                    MethodType.methodType(int.class));
        } catch (Throwable t) {
            bootFailed = true;
            ArondightLog.warnOnce("reflect_boot", "reflection bootstrap failed (" + t
                    + "); every spec-clone weave node self-disables, SP is never charged for them");
        }
    }

    private ArondightReflect() {}

    public static boolean isWorking() {
        return !bootFailed;
    }

    public static Object invoke(Object target, String methodName, Object... args) throws Throwable {
        if (bootFailed) throw new IllegalStateException("reflection bootstrap failed earlier");
        Object methodsObj = target.getClass().getMethods();
        Object[] methods = (Object[]) methodsObj;
        for (Object m : methods) {
            if (!methodName.equals(methodGetName.invoke(m))) continue;
            if ((int) methodParamCount.invoke(m) != args.length) continue;
            return methodInvoke.invoke(m, target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "/" + args.length);
    }

    public static Object construct(Class<?> cls, MethodType ctorType, Object... args) throws Throwable {
        MethodHandle ctor = MethodHandles.publicLookup().findConstructor(cls, ctorType);
        return ctor.invokeWithArguments(args);
    }
}
