package com.bringsalavat.pijon.model;

import android.graphics.drawable.Drawable;

/**
 * Represents an installed app that can be monitored by Pijon.
 * Used to populate the app selection list.
 */
public class MonitoredApp {

    private final String packageName;
    private final String appName;
    private final Drawable icon;
    private boolean enabled;

    public MonitoredApp(String packageName, String appName, Drawable icon, boolean enabled) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.enabled = enabled;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
