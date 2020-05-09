/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.PopupMenu;

import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.ButtonPreference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {

    /** Hidden field Settings.Secure.NOTIFICATION_BADGING */
    public static final String NOTIFICATION_BADGING = "notification_badging";
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String WALLPAPER_FILE = "wallpaper.png";

    private static final int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new LauncherSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment {

        private SystemDisplayRotationLockObserver mRotationLockObserver;
        private IconBadgingObserver mIconBadgingObserver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);

            ContentResolver resolver = getActivity().getContentResolver();

            // Setup allow rotation preference
            Preference rotationPref = findPreference(Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                // Launcher supports rotation by default. No need to show this setting.
                getPreferenceScreen().removePreference(rotationPref);
            } else {
                mRotationLockObserver = new SystemDisplayRotationLockObserver(rotationPref, resolver);

                // Register a content observer to listen for system setting changes while
                // this UI is active.
                mRotationLockObserver.register(Settings.System.ACCELEROMETER_ROTATION);

                // Initialize the UI once
                rotationPref.setDefaultValue(Utilities.getAllowRotationDefaultValue(getActivity()));
            }

            Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);
            if (iconShapeOverride != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
                } else {
                    getPreferenceScreen().removePreference(iconShapeOverride);
                }
            }

            ButtonPreference changeWallpaper = (ButtonPreference) findPreference("change_wallpaper");

            if (changeWallpaper != null) {
                changeWallpaper.setOnPreferenceClickListener(preference -> {

                    try {
                        View v = changeWallpaper.getView();
                        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.inflate(R.menu.change_wallpaper);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            int i = item.getItemId();
                            if (i == R.id.action_wallpaper_restore) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.getFileStreamPath("wallpaper.png").delete();
                                }
                            } else if (i == R.id.action_wallpaper_set) {
                                Intent intent = new Intent(Intent.ACTION_PICK, null);
                                final String IMAGE_UNSPECIFIED = "image/*";//随意图片类型
                                intent.setDataAndType(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        IMAGE_UNSPECIFIED);

                                try {
                                    startActivityForResult(intent, RESULT_LOAD_IMAGE);
                                } catch (Throwable ignored) {
                                }
                            }
                            return false;
                        });
                        popupMenu.show();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return false;
                });
            }
        }

        private static class WallpaperChooseTask extends AsyncTask<Void, Void, Void> {

            WeakReference<ProgressDialog> mRef;
            WeakReference<Context> mActivityRef;
            String picturePath;
            WallpaperChooseTask(ProgressDialog dialog, String path) {
                mRef = new WeakReference<>(dialog);
                mActivityRef = new WeakReference<>(dialog.getContext());
                picturePath = path;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                try {
                    ProgressDialog progressDialog = mRef.get();
                    if (progressDialog == null) {
                        return;
                    }
                    progressDialog.show();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Context context = mActivityRef.get();
                if (context == null) {
                    return null;
                }
                FileOutputStream fos = null;
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeFile(picturePath);
                    File fileStreamPath = context.getFileStreamPath(WALLPAPER_FILE);
                    fos = new FileOutputStream(fileStreamPath);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    ProgressDialog progressDialog = mRef.get();
                    if (progressDialog == null) {
                        return;
                    }
                    progressDialog.hide();
                } catch (Throwable ignored) {
                }
            }
        }
        @Override
        public void onDestroy() {
            if (mRotationLockObserver != null) {
                mRotationLockObserver.unregister();
                mRotationLockObserver = null;
            }
            if (mIconBadgingObserver != null) {
                mIconBadgingObserver.unregister();
                mIconBadgingObserver = null;
            }
            super.onDestroy();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK && requestCode == RESULT_LOAD_IMAGE) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                try {
                    Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    if (cursor == null) {
                        return;
                    }
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();

                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setTitle(R.string.wallpaper_changing);
                    progressDialog.setCancelable(false);
                    new WallpaperChooseTask(progressDialog, picturePath).execute();

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Content observer which listens for system auto-rotate setting changes, and enables/disables
     * the launcher rotation setting accordingly.
     */
    private static class SystemDisplayRotationLockObserver extends SettingsObserver.System {

        private final Preference mRotationPref;

        public SystemDisplayRotationLockObserver(
                Preference rotationPref, ContentResolver resolver) {
            super(resolver);
            mRotationPref = rotationPref;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            mRotationPref.setEnabled(enabled);
            mRotationPref.setSummary(enabled
                    ? R.string.allow_rotation_desc : R.string.allow_rotation_blocked_desc);
        }
    }

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    private static class IconBadgingObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final ButtonPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;
        private boolean serviceEnabled = true;

        public IconBadgingObserver(ButtonPreference badgingPref, ContentResolver resolver,
                FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }
            mBadgingPref.setWidgetFrameVisible(!serviceEnabled);
            mBadgingPref.setOnPreferenceClickListener(serviceEnabled && Utilities.ATLEAST_OREO ? null : this);
            mBadgingPref.setSummary(summary);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (!Utilities.ATLEAST_OREO && serviceEnabled) {
                ComponentName cn = new ComponentName(preference.getContext(), NotificationListener.class);
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(":settings:fragment_args_key", cn.flattenToString());
                preference.getContext().startActivity(intent);
            } else {
                new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
            }
            return true;
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(":settings:fragment_args_key", cn.flattenToString());
            getActivity().startActivity(intent);
        }
    }
}
