package com.bringsalavat.pijon.util;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Centralised helper for checking and requesting the special permissions
 * that Pijon needs: Usage Stats, Overlay, and Notifications.
 */
public class PermissionHelper {

    private PermissionHelper() {
        // Utility class
    }

    /**
     * Check whether the app has been granted PACKAGE_USAGE_STATS permission.
     */
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Open the system settings screen where the user can grant Usage Stats access.
     */
    public static void requestUsageStatsPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Check whether the app can draw overlays over other apps.
     */
    public static boolean hasOverlayPermission(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * Open the system settings screen where the user can grant overlay permission.
     */
    public static void requestOverlayPermission(Context context) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Check whether we need to request notification permission (Android 13+).
     */
    public static boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
