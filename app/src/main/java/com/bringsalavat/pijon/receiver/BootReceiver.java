package com.bringsalavat.pijon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.service.PijonMonitorService;

/**
 * Receives system broadcasts to restart the monitoring service:
 * - BOOT_COMPLETED: after device reboot
 * - MY_PACKAGE_REPLACED: after Pijon is updated via Play Store or sideload
 *
 * Only restarts the service if Pijon was previously enabled by the user.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        boolean shouldRestart = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);

        if (shouldRestart) {
            PrefsManager prefs = new PrefsManager(context);
            if (prefs.isGlobalEnabled() && !prefs.getSelectedApps().isEmpty()) {
                Log.d(TAG, "Restarting Pijon service after: " + action);
                Intent serviceIntent = new Intent(context, PijonMonitorService.class);
                context.startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "Pijon disabled or no apps selected — skipping restart");
            }
        }
    }
}
