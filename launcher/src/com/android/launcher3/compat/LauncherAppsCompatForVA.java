package com.android.launcher3.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.launcher3.util.PackageUserKey;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstalledAppInfo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * author: weishu on 18/2/9.
 */

public class LauncherAppsCompatForVA extends LauncherAppsCompatVL {
    private static final String TAG = "LauncherAppsCompatForVA";

    private final VirtualCore mVirtualCore;

    private VirtualCore.PackageObserver mPackageObserver;

    private boolean showSystemApp;

    LauncherAppsCompatForVA() {
        super(VirtualCore.get().getContext());
        mVirtualCore = VirtualCore.get();
        showSystemApp = isLauncherEnable(mVirtualCore.getContext());
    }

    private boolean isLauncherEnable(Context context) {
        if (context == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }
        if (pm.getComponentEnabledSetting(new ComponentName(context.getPackageName(), "vxp.launcher")) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        }
        return false;
    }

    @Override
    public List<LauncherActivityInfo> getActivityList(String packageName, UserHandle user) {
        int vuserId = UserManagerCompat.toUserId(user);
        List<LauncherActivityInfo> result = new ArrayList<>();
        if (showSystemApp) {
            try {
                result.addAll(super.getActivityList(packageName, user));
            } catch (Throwable ignored) {
                Log.w(TAG, "add super failed", ignored);
            }
        }

        if (packageName == null) {
            List<InstalledAppInfo> installedApps = mVirtualCore.getInstalledAppsAsUser(vuserId, 0);
            for (InstalledAppInfo installedApp : installedApps) {
                List<LauncherActivityInfo> activityListForPackage = getActivityListForPackage(installedApp.packageName);
                assert activityListForPackage != null;
                result.addAll(activityListForPackage);
            }
        } else {
            List<LauncherActivityInfo> activityListForPackage = getActivityListForPackage(packageName);
            result.addAll(activityListForPackage);
        }
        return result;
    }

    @Override
    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle user) {
        Context context = mVirtualCore.getContext();
        int vuserId = UserManagerCompat.toUserId(user);

        VPackageManager pm = VPackageManager.get();
        List<ResolveInfo> ris = null;
        try {
            ris = pm.queryIntentActivities(intent, intent.resolveType(context), 0, vuserId);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // reuse the intent instance
            intent.removeCategory(Intent.CATEGORY_INFO);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(intent.getPackage());
            ris = pm.queryIntentActivities(intent, intent.resolveType(context), 0, vuserId);
        }
        if (ris == null || ris.size() <= 0) {
            if (showSystemApp) {
                try {
                    return super.resolveActivity(intent, user);
                } catch (Throwable e) {
                    return null;
                }
            } else {
                return null;
            }
        }


        try {
            return makeLauncherActivityInfo(context, ris.get(0), Process.myUserHandle());
        } catch (Throwable e) {
            Log.e(TAG, "create launcherActivityInfo failed", e);
            if (showSystemApp) {
                try {
                    return super.resolveActivity(intent, user);
                } catch (Throwable e2) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @Override
    public void startActivityForProfile(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        if (showSystemApp) {
            try {
                super.startActivityForProfile(component, user, sourceBounds, opts);
            } catch (Throwable e) {
                Log.e(TAG, "startActivityForProfile", e);
            }
        }
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags, UserHandle user) {
        InstalledAppInfo installedAppInfo = mVirtualCore.getInstalledAppInfo(packageName, flags);
        if (installedAppInfo == null) {
            if (showSystemApp) {
                try {
                    return super.getApplicationInfo(packageName, flags, user);
                } catch (Throwable e) {
                    Log.e(TAG, "getApplicationInfo", e);
                    return null;
                }
            } else {
                return null;
            }
        }
        return installedAppInfo.getApplicationInfo(0);
    }

    @Override
    public void showAppDetailsForProfile(ComponentName component, UserHandle user, Rect sourceBounds, Bundle opts) {
        if (showSystemApp) {
            try {
                super.showAppDetailsForProfile(component, user, sourceBounds, opts);
            } catch (Throwable e) {
                Log.e(TAG, "showAppDetailsForProfile", e);
            }
        }
    }

    @Override
    public void addOnAppsChangedCallback(OnAppsChangedCallbackCompat listener) {
        mPackageObserver = new VirtualCore.PackageObserver() {
            @Override
            public void onPackageInstalled(String packageName) throws RemoteException {
                listener.onPackageAdded(packageName, UserManagerCompat.fromUserId(0));
            }

            @Override
            public void onPackageUninstalled(String packageName) throws RemoteException {
                listener.onPackageRemoved(packageName, UserManagerCompat.fromUserId(0));
            }

            @Override
            public void onPackageInstalledAsUser(int userId, String packageName) throws RemoteException {
                UserHandle userHandle = UserManagerCompat.fromUserId(userId);
                listener.onPackageAdded(packageName, userHandle);
            }

            @Override
            public void onPackageUninstalledAsUser(int userId, String packageName) throws RemoteException {
                UserHandle userHandle = UserManagerCompat.fromUserId(userId);
                listener.onPackageRemoved(packageName, userHandle);
            }
        };
        try {
            mVirtualCore.registerObserver(mPackageObserver);
        } catch (Throwable e) {
            // register error, may to early?
            new Handler().postDelayed(() -> {
                try {
                    mVirtualCore.registerObserver(mPackageObserver);
                } catch (Throwable ignored) {
                    ignored.printStackTrace();
                }
            }, 1000);
        }
        if (showSystemApp) {
            try {
                super.addOnAppsChangedCallback(listener);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat listener) {
        if (mPackageObserver != null) {
            mVirtualCore.unregisterObserver(mPackageObserver);
        }
        if (showSystemApp) {
            try {
                super.removeOnAppsChangedCallback(listener);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean isPackageEnabledForProfile(String packageName, UserHandle user) {
        if (mVirtualCore.isAppInstalled(packageName)) {
            return true;
        }

        if (showSystemApp) {
            try {
                return super.isPackageEnabledForProfile(packageName, user);
            } catch (Throwable throwable) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isActivityEnabledForProfile(ComponentName component, UserHandle user) {
        return true;
    }

    @Override
    public List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(@Nullable PackageUserKey packageUser) {
        try {
            return super.getCustomShortcutActivityList(packageUser);
        } catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    private List<LauncherActivityInfo> getActivityListForPackage(String packageName) {
        List<LauncherActivityInfo> result = new ArrayList<>();
        for (VUserInfo vUserInfo : VUserManager.get().getUsers()) {
            result.addAll(getActivityListForPackageAsUser(packageName, vUserInfo.id));
        }
        return result;
    }

    private List<LauncherActivityInfo> getActivityListForPackageAsUser(String packageName, int vuid) {

        List<LauncherActivityInfo> result = new ArrayList<>();
        Context context = mVirtualCore.getContext();
        VPackageManager pm = VPackageManager.get();
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, vuid);

        // Otherwise, try to find a main launcher activity
        if (ris == null || ris.size() <= 0) {
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, vuid);
        }

        if (ris == null || ris.size() == 0) {
            return result;
        }

        // remove the alias-activity
        ResolveInfo first = ris.get(0);
        Iterator<ResolveInfo> iterator = ris.iterator();
        while (iterator.hasNext()) {
            ResolveInfo next = iterator.next();
            if (next.activityInfo.targetActivity != null) {
                // alias, remove it
                iterator.remove();
                continue;
            }
            if (!next.activityInfo.enabled) {
                // disabled component ,remove it
                iterator.remove();
            }
        }

        // if it is all alias, keep one.
        if (ris.size() == 0) {
            ris.add(first);
        }

        for (ResolveInfo resolveInfo : ris) {
            try {
                UserHandle userHandle = UserManagerCompat.fromUserId(vuid);
                result.add(makeLauncherActivityInfo(context, resolveInfo, userHandle));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static LauncherActivityInfo makeLauncherActivityInfo(Context context, ResolveInfo resolveInfo, UserHandle userHandle) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Constructor<LauncherActivityInfo> constructor = LauncherActivityInfo.class.getDeclaredConstructor(Context.class, ActivityInfo.class, UserHandle.class);
                constructor.setAccessible(true);
                return constructor.newInstance(context, resolveInfo.activityInfo, userHandle);
            } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Constructor<LauncherActivityInfo> constructor = LauncherActivityInfo.class.getDeclaredConstructor(Context.class,
                        ResolveInfo.class, UserHandle.class, long.class);
                constructor.setAccessible(true);
                return constructor.newInstance(context, resolveInfo, userHandle, System.currentTimeMillis());
            } else {
                throw new RuntimeException("can not construct launcher activity info");
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
