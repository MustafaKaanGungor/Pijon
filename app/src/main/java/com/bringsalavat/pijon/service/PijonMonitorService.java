package com.bringsalavat.pijon.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bringsalavat.pijon.MainActivity;
import com.bringsalavat.pijon.R;
import com.bringsalavat.pijon.data.PijonDatabase;
import com.bringsalavat.pijon.data.PrefsManager;
import com.bringsalavat.pijon.model.UsageLog;
import com.bringsalavat.pijon.overlay.OverlayManager;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;

/**
 * Core foreground service that monitors which app is in the foreground,
 * manages the usage timer, and triggers the overlay pop-up when the timer expires.
 *
 * Edge cases handled:
 * - Screen off / locked: timer pauses
 * - Split screen: monitors whichever app is focused (most recent ACTIVITY_RESUMED)
 * - PiP: the PiP window is a separate activity, timer follows the focused app
 * - App uninstalled while selected: graceful handling via try-catch
 * - Service killed by OEM: START_STICKY attempts restart
 */
public class PijonMonitorService extends Service {

    private static final String TAG = "PijonMonitorService";
    private static final String CHANNEL_ID = "pijon_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long POLL_INTERVAL_MS = 1500;

    private Handler handler;
    private PrefsManager prefsManager;
    private OverlayManager overlayManager;
    private UsageStatsManager usageStatsManager;
    private PowerManager powerManager;
    private KeyguardManager keyguardManager;

    // Timer state
    private long timerRemainingMs;
    private long lastTickTime;
    private String currentMonitoredPackage;
    private boolean timerRunning = false;

    // Session tracking — wall-clock time since the monitored app was opened
    private long sessionStartTimeMs = 0;
    // True while a monitored app is currently in the foreground; false once another app takes over
    private boolean monitoredAppWasActive = false;

    // Last known foreground package (persists across polls)
    private String lastForegroundPackage = null;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                pollForegroundApp();
            } catch (Exception e) {
                Log.e(TAG, "Error in poll loop", e);
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        prefsManager = new PrefsManager(this);
        overlayManager = new OverlayManager(this);
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        resetTimer();
        handler.removeCallbacks(pollRunnable); // Prevent duplicate poll loops
        handler.post(pollRunnable);
        Log.d(TAG, "Pijon monitoring service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        overlayManager.dismiss();
        Log.d(TAG, "Pijon monitoring service stopped");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------------------------------------------------------
    // Foreground app detection
    // ---------------------------------------------------------------

    private void pollForegroundApp() {
        // Don't poll while overlay is showing — wait for user response
        if (overlayManager.isShowing()) {
            return;
        }

        // Edge case: screen off or device locked — pause the timer
        if (!isScreenOnAndUnlocked()) {
            if (timerRunning) {
                pauseTimer();
                Log.d(TAG, "Paused timer — screen off or locked");
            }
            return;
        }

        String foregroundPackage = getForegroundPackageName();
        if (foregroundPackage == null) {
            return;
        }

        // Skip our own package (e.g., if Pijon itself is in foreground)
        if (foregroundPackage.equals(getPackageName())) {
            return;
        }

        Set<String> selectedApps = prefsManager.getSelectedApps();

        if (selectedApps.contains(foregroundPackage)) {
            // Monitored app is in the foreground
            if (!foregroundPackage.equals(currentMonitoredPackage)) {
                // Switched to a different monitored app — new session
                currentMonitoredPackage = foregroundPackage;
                sessionStartTimeMs = System.currentTimeMillis();
                resetTimer();
                Log.d(TAG, "Monitoring app: " + foregroundPackage);
            } else if (!monitoredAppWasActive) {
                // Returned to the same monitored app after another app took over — new session
                sessionStartTimeMs = System.currentTimeMillis();
            }
            monitoredAppWasActive = true;

            if (!timerRunning) {
                startTimer();
            } else {
                tickTimer();
            }
        } else {
            // Non-monitored app (or home screen) — pause timer and mark session as inactive
            monitoredAppWasActive = false;
            if (timerRunning) {
                pauseTimer();
                Log.d(TAG, "Paused timer — foreground: " + foregroundPackage);
            }
        }
    }

    /**
     * Check if the screen is on AND the device is unlocked.
     * Timer should only count when the user is actively using the phone.
     */
    private boolean isScreenOnAndUnlocked() {
        boolean screenOn = powerManager.isInteractive();
        boolean unlocked = !keyguardManager.isDeviceLocked();
        return screenOn && unlocked;
    }

    /**
     * Get the package name of the app currently in the foreground.
     *
     * Primary: Uses UsageEvents.queryEvents() to find the most recent
     * MOVE_TO_FOREGROUND (API < 29) or ACTIVITY_RESUMED (API 29+) event.
     * This also works correctly for split-screen — the most recent
     * ACTIVITY_RESUMED event corresponds to the focused split.
     *
     * Fallback: Uses queryUsageStats() sorted by lastTimeUsed.
     */
    private String getForegroundPackageName() {
        long now = System.currentTimeMillis();

        // --- Primary: queryEvents (more accurate) ---
        try {
            UsageEvents events = usageStatsManager.queryEvents(now - 30_000, now);
            UsageEvents.Event event = new UsageEvents.Event();

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();
                // MOVE_TO_FOREGROUND = 1 (API 21-28), ACTIVITY_RESUMED = 7 (API 29+)
                if (type == 1 || type == 7) {
                    lastForegroundPackage = event.getPackageName();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "queryEvents failed", e);
        }

        if (lastForegroundPackage != null) {
            return lastForegroundPackage;
        }

        // --- Fallback: queryUsageStats ---
        try {
            List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 60_000,
                    now
            );
            if (stats != null && !stats.isEmpty()) {
                SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
                for (UsageStats stat : stats) {
                    sortedMap.put(stat.getLastTimeUsed(), stat);
                }
                if (!sortedMap.isEmpty()) {
                    UsageStats recent = sortedMap.get(sortedMap.lastKey());
                    if (recent != null) {
                        lastForegroundPackage = recent.getPackageName();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "queryUsageStats failed", e);
        }

        return lastForegroundPackage;
    }

    // ---------------------------------------------------------------
    // Timer management
    // ---------------------------------------------------------------

    private void resetTimer() {
        timerRemainingMs = prefsManager.getTimerDurationSeconds() * 1000L;
        timerRunning = false;
        lastTickTime = 0;
        Log.d(TAG, "Timer reset to " + prefsManager.getTimerDurationSeconds() + "s");
    }

    private void startTimer() {
        timerRunning = true;
        lastTickTime = System.currentTimeMillis();
        Log.d(TAG, "Timer started (" + (timerRemainingMs / 1000) + "s remaining)");
    }

    private void pauseTimer() {
        if (timerRunning) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastTickTime;
            timerRemainingMs -= elapsed;
            if (timerRemainingMs < 0) timerRemainingMs = 0;
            timerRunning = false;
        }
    }

    private void tickTimer() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTickTime;
        lastTickTime = now;
        timerRemainingMs -= elapsed;

        if (timerRemainingMs <= 0) {
            timerRunning = false;
            Log.d(TAG, "Timer expired! Showing overlay for " + currentMonitoredPackage);
            showOverlay();
        }
    }

    // ---------------------------------------------------------------
    // Overlay
    // ---------------------------------------------------------------

    private void showOverlay() {
        String appName = getAppDisplayName(currentMonitoredPackage);
        int intervalSeconds = prefsManager.getTimerDurationSeconds();

        // Show how long the user has been in this app since they opened it, not just the interval
        int sessionElapsedSeconds = (int) ((System.currentTimeMillis() - sessionStartTimeMs) / 1000);
        String formattedDuration = PrefsManager.formatDuration(sessionElapsedSeconds);

        overlayManager.show(appName, formattedDuration, (feeling, goodUse) -> {
            logResponse(currentMonitoredPackage, appName, intervalSeconds, feeling, goodUse);
            resetTimer();
            startTimer();
        });
    }

    /**
     * Get the display name for a package. Handles the case where the app
     * may have been uninstalled since it was selected.
     */
    private String getAppDisplayName(String packageName) {
        if (packageName == null) return "Unknown";
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // App was uninstalled — remove it from selected apps
            prefsManager.removeSelectedApp(packageName);
            return packageName;
        }
    }

    private void logResponse(String packageName, String appName,
                             int durationSeconds, int feeling, int goodUse) {
        UsageLog log = new UsageLog(
                packageName, appName,
                System.currentTimeMillis(),
                durationSeconds, feeling, goodUse
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PijonDatabase.getInstance(this).usageLogDao().insert(log);
                Log.d(TAG, "Logged: " + appName + " feeling=" + feeling + " goodUse=" + goodUse);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log response", e);
            }
        });
    }

    // ---------------------------------------------------------------
    // Notification
    // ---------------------------------------------------------------

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.notification_channel_desc));

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_pijon_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
