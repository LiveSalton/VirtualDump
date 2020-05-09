package com.salton123.hook

import com.salton123.log.XLog
import de.robv.android.xposed.XC_MethodHook

/**
 * User: wujinsheng1@yy.com
 * Date: 2020/5/8 15:34
 * ModifyTime: 15:34
 * Description:
 */
object ClassLoaderHook {
    private val TAG = "ClassLoaderHook"

    fun hook() {
        DexposedManager.getIntance().hookMethod(
                ClassLoader::class.java,
                "loadClass",
                object : DexposedManager.HookMethodCallback<ClassLoader> {

                    override fun hookMethodBefore(classLoader: ClassLoader, param: XC_MethodHook.MethodHookParam) {
                        // for (Object item : param.args) {
                        //     XLog.i(TAG, item.toString());
                        // }
//                        val dexCache = XposedHelpers.getObjectField(param.result.javaClass, "dexCache")
                        XLog.i(TAG, "${param.args},${param.result}")
                    }

                    override fun hookMothodAfter(classLoader: ClassLoader, param: XC_MethodHook.MethodHookParam) {
                    }

                    override fun hookConstruct(classLoader: ClassLoader, param: XC_MethodHook.MethodHookParam) {
                    }
                }, String::class.java)
    }
}
