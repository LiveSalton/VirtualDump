package com.salton123.hook;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;

/**
 * User: newSalton@outlook.com
 * Date: 2020/1/16 15:00
 * ModifyTime: 15:00
 * Description:
 */
public class DexposedManager {

    private final static String TAG = DexposedManager.class.getSimpleName();
    private volatile static DexposedManager sIntance = null;

    private DexposedManager() {
    }

    public static DexposedManager getIntance() {
        if (sIntance == null) {
            synchronized (DexposedManager.class) {
                if (sIntance == null) {
                    sIntance = new DexposedManager();
                }
            }
        }
        return sIntance;
    }

    /**
     * hook 构造方法
     *
     * @param clazz
     * @param callback
     * @param <T>
     */
    public <T> void hookConstructs(Class<T> clazz, final HookConstructorCallback<T> callback) {
        DexposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (callback != null) {
                    callback.hookConstruct((T) param.thisObject, param);
                }
            }
        });
    }

    /**
     * hook 某一个方法
     *
     * @param clazz
     * @param methodName
     * @param callback
     * @param <T>
     */
    public <T> void hookMethod(final Class<T> clazz,
                               final String methodName,
                               final HookMethodCallback<T> callback,
                               final Class... args) {
        Object[] objs = new Object[args != null ? args.length + 1 : 1];
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                objs[i] = args[i];
            }
        }
        //设置最后一个元素的为 callback
        objs[objs.length - 1] = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (callback != null) {
                    callback.hookMethodBefore((T) param.thisObject, param);
                }
            }

            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (callback != null) {
                    callback.hookMothodAfter((T) param.thisObject, param);
                }
            }
        };
        DexposedBridge.findAndHookMethod(clazz, methodName, objs);
    }

    /**
     * 构造回调
     *
     * @param <T>
     */
    public interface HookConstructorCallback<T> {
        void hookConstruct(T t, XC_MethodHook.MethodHookParam param);
    }

    /**
     * 方法回调
     *
     * @param <T>
     */
    public interface HookMethodCallback<T> extends HookConstructorCallback<T> {
        void hookMethodBefore(T t, XC_MethodHook.MethodHookParam param);

        void hookMothodAfter(T t, XC_MethodHook.MethodHookParam param);
    }
}
