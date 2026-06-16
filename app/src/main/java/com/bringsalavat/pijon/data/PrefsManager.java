package com.bringsalavat.pijon.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around SharedPreferences for all Pijon user settings.
 */
public class PrefsManager {

    private static final String PREFS_NAME = "pijon_prefs";

    private static final String KEY_SELECTED_APPS = "selected_apps";
    private static final String KEY_TIMER_DURATION_SECONDS = "timer_duration_seconds";
    private static final String KEY_GLOBAL_ENABLED = "global_enabled";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    /** Default timer: 5 minutes = 300 seconds */
    private static final int DEFAULT_TIMER_DURATION_SECONDS = 300;

    /** Preset durations in seconds for the settings seekbar */
    public static final int[] TIMER_PRESETS = {
            10, 15, 30, 60, 120, 180, 300, 600, 900, 1800, 3600
    };

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Selected Apps ---

    public Set<String> getSelectedApps() {
        return new HashSet<>(prefs.getStringSet(KEY_SELECTED_APPS, new HashSet<>()));
    }

    public void setSelectedApps(Set<String> apps) {
        prefs.edit().putStringSet(KEY_SELECTED_APPS, apps).apply();
    }

    public void addSelectedApp(String packageName) {
        Set<String> apps = getSelectedApps();
        apps.add(packageName);
        setSelectedApps(apps);
    }

    public void removeSelectedApp(String packageName) {
        Set<String> apps = getSelectedApps();
        apps.remove(packageName);
        setSelectedApps(apps);
    }

    public boolean isAppSelected(String packageName) {
        return getSelectedApps().contains(packageName);
    }

    // --- Timer Duration (in seconds) ---

    public int getTimerDurationSeconds() {
        return prefs.getInt(KEY_TIMER_DURATION_SECONDS, DEFAULT_TIMER_DURATION_SECONDS);
    }

    public void setTimerDurationSeconds(int seconds) {
        prefs.edit().putInt(KEY_TIMER_DURATION_SECONDS, seconds).apply();
    }

    /**
     * Format a duration in seconds to a human-readable string.
     * e.g. 10 → "10 seconds", 60 → "1 minute", 90 → "1 min 30 sec", 300 → "5 minutes"
     */
    public static String formatDuration(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " seconds";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (seconds == 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return minutes + " min " + seconds + " sec";
    }

    // --- Global Enabled ---

    public boolean isGlobalEnabled() {
        return prefs.getBoolean(KEY_GLOBAL_ENABLED, false);
    }

    public void setGlobalEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GLOBAL_ENABLED, enabled).apply();
    }

    // --- Sound ---

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    // --- Vibration ---

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }
}
