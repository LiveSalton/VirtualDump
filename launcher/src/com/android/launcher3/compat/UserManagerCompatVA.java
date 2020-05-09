package com.android.launcher3.compat;

import android.os.UserHandle;

import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * author: weishu on 18/2/11.
 */

public class UserManagerCompatVA extends UserManagerCompat {
    private VUserManager mUserManager;

    public UserManagerCompatVA() {
        this.mUserManager = VUserManager.get();
    }

    @Override
    public void enableAndResetCache() {

    }

    @Override
    public List<UserHandle> getUserProfiles() {
        List<VUserInfo> users = mUserManager.getUsers();
        List<UserHandle> result = new ArrayList<>();
        for (VUserInfo user : users) {
            result.add(fromUserId(user.id));
        }
        return result;
    }

    @Override
    public long getSerialNumberForUser(UserHandle user) {
        VUserHandle vUserHandle = new VUserHandle(toUserId(user));
        return mUserManager.getSerialNumberForUser(vUserHandle);
    }

    @Override
    public UserHandle getUserForSerialNumber(long serialNumber) {
        VUserHandle vUser = mUserManager.getUserForSerialNumber(serialNumber);
        return fromUserId(vUser.getIdentifier());
    }

    @Override
    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandle user) {
        return String.format(Locale.getDefault(), "%s[%d]", label, toUserId(user));
    }

    @Override
    public long getUserCreationTime(UserHandle user) {
        int userId = toUserId(user);
        VUserInfo userInfo = mUserManager.getUserInfo(userId);
        return userInfo.creationTime;
    }

    @Override
    public boolean isQuietModeEnabled(UserHandle user) {
        return false;
    }

    @Override
    public boolean isUserUnlocked(UserHandle user) {
        return false;
    }

    @Override
    public boolean isDemoUser() {
        return false;
    }
}
