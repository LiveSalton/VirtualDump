package com.google.android.apps.nexuslauncher.utils;

import java.lang.reflect.Field;

/**
 * author: weishu on 18/3/2.
 */

public class BuildUtil {

    private static final boolean ENABLE_DYNAMIC_ID = false;
    private static final String DEFAULT_ID = "io.va.exposed";

    public static String getApplicationId() {
        if (!ENABLE_DYNAMIC_ID) {
            return DEFAULT_ID;
        }
        final String buildClass = "io.virtualapp.BuildConfig";
        try {
            Class<?> clazz = Class.forName(buildClass);
            Field applicationId = clazz.getDeclaredField("APPLICATION_ID");
            return (String) applicationId.get(null);
        } catch (Throwable e) {
            return DEFAULT_ID;
        }
    }
}
