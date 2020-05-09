package com.android.launcher3.popup;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.InfoDropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.widget.WidgetsBottomSheet;
import com.lody.virtual.client.core.VirtualCore;

import java.util.List;

import static com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import static com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 * <p>
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut extends ItemInfo {
    private final int mIconResId;
    private final int mLabelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        mIconResId = iconResId;
        mLabelResId = labelResId;
    }

    public Drawable getIcon(Context context) {
        return context.getResources().getDrawable(mIconResId, context.getTheme());
    }

    public String getLabel(Context context) {
        return context.getString(mLabelResId);
    }

    public abstract View.OnClickListener getOnClickListener(final Launcher launcher,
                                                            final ItemInfo itemInfo);

    public static class Custom extends SystemShortcut {

        public Custom() {
            super(R.drawable.ic_edit_no_shadow, R.string.action_preferences);
        }

        @Override
        public View.OnClickListener getOnClickListener(Launcher launcher, ItemInfo itemInfo) {
            return null;
        }
    }

    public static class Widgets extends SystemShortcut {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            final List<WidgetItem> widgets = launcher.getWidgetsForPackageUser(new PackageUserKey(
                    itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
            if (widgets == null) {
                return null;
            }
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AbstractFloatingView.closeAllOpenViews(launcher);
                    WidgetsBottomSheet widgetsBottomSheet =
                            (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
                                    R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
                    widgetsBottomSheet.populateAndShow(itemInfo);
                    launcher.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                            ControlType.WIDGETS_BUTTON, view);
                }
            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                                                       final ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Rect sourceBounds = launcher.getViewBounds(view);
                    Bundle opts = launcher.getActivityLaunchOptions(view);
                    InfoDropTarget.startDetailsActivityForInfo(itemInfo, launcher, null, sourceBounds, opts);
                    launcher.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                            ControlType.APPINFO_TARGET, view);
                }
            };
        }
    }

    public static class KillApp extends SystemShortcut {

        public KillApp() {
            super(R.drawable.ic_kill_app, R.string.kill_app_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(Launcher launcher, ItemInfo itemInfo) {
            final String packageName = itemInfo.getTargetComponent().getPackageName();

            return v -> {
                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(launcher);
                if (topView instanceof PopupContainerWithArrow) {
                    // 1. first close the menu
                    topView.close(true);
                    // 2. show alert

                    AlertDialog alertDialog = new AlertDialog.Builder(launcher)
                            .setTitle(R.string.home_menu_kill_title)
                            .setMessage(launcher.getResources().getString(R.string.home_menu_kill_content, itemInfo.title))
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                try {
                                    UserHandle user = itemInfo.user;
                                    int userId = UserManagerCompat.toUserId(user);
                                    VirtualCore.get().killApp(packageName, userId);
                                } catch (Throwable e) {
                                    Toast.makeText(launcher, "Kill " + itemInfo.title + " Failed, please try again.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create();
                    try {
                        alertDialog.show();
                    } catch (Throwable ignored) {
                        // BadTokenException.
                    }
                }
            };
        }
    }

    public static class ClearApp extends SystemShortcut {

        public ClearApp() {
            super(R.drawable.ic_clear_app, R.string.clear_app_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher, ItemInfo itemInfo) {
            final String packageName = itemInfo.getTargetComponent().getPackageName();
            return v -> {
                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(launcher);
                if (topView instanceof PopupContainerWithArrow) {
                    // 1. first close the menu
                    topView.close(true);
                    // 2. show alert

                    AlertDialog alertDialog = new AlertDialog.Builder(launcher)
                            .setTitle(R.string.home_menu_clear_title)
                            .setMessage(launcher.getString(R.string.home_menu_clear_content, itemInfo.title))
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                UserHandle user = itemInfo.user;
                                int userId = UserManagerCompat.toUserId(user);
                                VirtualCore.get().clearPackageAsUser(userId, packageName);
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .create();
                    try {
                        alertDialog.show();
                    } catch (Throwable ignored) {
                        // BadTokenException.
                    }
                }
            };
        }
    }

    public static class CreateDesktopShortcut extends SystemShortcut {

        public CreateDesktopShortcut() {
            super(R.drawable.ic_create_shortcut, R.string.create_shortcut_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(Launcher launcher, ItemInfo itemInfo) {
            final String packageName = itemInfo.getTargetComponent().getPackageName();

            return v -> {
                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(launcher);
                if (!(topView instanceof PopupContainerWithArrow)) {
                    return;
                }
                // first close the menu
                topView.close(true);

                UserHandle user = itemInfo.user;
                int userId = UserManagerCompat.toUserId(user);

                VirtualCore.OnEmitShortcutListener listener = new VirtualCore.OnEmitShortcutListener() {
                    @Override
                    public Bitmap getIcon(Bitmap originIcon) {
                        return originIcon;
                    }

                    @Override
                    public String getName(String originName) {
                        if (userId == 0) {
                            return originName + "(VXP)";
                        } else {
                            return "[" + (userId + 1) + "]" + originName;
                        }
                    }
                };

                Intent splashIntent = new Intent();
                splashIntent.setComponent(new ComponentName(VirtualCore.get().getHostPkg(), "io.virtualapp.home.LoadingActivity"));
                if (VirtualCore.get().createShortcut(userId, packageName, splashIntent, listener)) {
                    if (!Utilities.ATLEAST_OREO) {
                        Toast.makeText(VirtualCore.get().getContext(),
                                R.string.create_shortcut_success, Toast.LENGTH_SHORT).show();
                    }
                }
            };
        }
    }
}
