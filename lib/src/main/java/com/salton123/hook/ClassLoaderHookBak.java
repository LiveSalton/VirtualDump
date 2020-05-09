package com.salton123.hook;

import com.salton123.log.XLog;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * User: wujinsheng1@yy.com
 * Date: 2020/5/8 15:34
 * ModifyTime: 15:34
 * Description:
 */
public class ClassLoaderHookBak {
    private static String TAG = "ClassLoaderHook";

    public static void hook() {
        DexposedManager.getIntance().hookMethod(
                ClassLoader.class,
                "loadClass",
                new DexposedManager.HookMethodCallback<ClassLoader>() {

                    @Override
                    public void hookMethodBefore(ClassLoader classLoader, XC_MethodHook.MethodHookParam param) {
                        for (Object item : param.args) {
                            XLog.i(TAG, item.toString());
                        }
                        XLog.i(TAG, param.getResult().getClass().getName());
                        Object dexCache = XposedHelpers.getObjectField(param.getResult().getClass(), "dexCache");
                    }

                    @Override
                    public void hookMothodAfter(ClassLoader classLoader, XC_MethodHook.MethodHookParam param) {

                    }

                    @Override
                    public void hookConstruct(ClassLoader classLoader, XC_MethodHook.MethodHookParam param) {

                    }

                }, String.class);
    }
}
