package com.bringsalavat.pijon.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a single usage log entry.
 * Recorded each time the user responds to a Pijon pop-up reminder.
 *
 * feeling / goodUse values: 1 = positive, 0 = neutral, -1 = negative
 */
@Entity(tableName = "usage_log")
public class UsageLog {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String packageName;
    private String appName;
    private long timestamp;       // System.currentTimeMillis() when the pop-up was shown
    private int durationSeconds;  // Timer interval that triggered this reminder

    // Q1: "How did this last X make me feel?"
    private int feeling;          // 1=positive, 0=neutral, -1=negative

    // Q2: "Was this a good use of my time?"
    private int goodUse;          // 1=yes, 0=unsure, -1=no

    public UsageLog(String packageName, String appName, long timestamp,
                    int durationSeconds, int feeling, int goodUse) {
        this.packageName = packageName;
        this.appName = appName;
        this.timestamp = timestamp;
        this.durationSeconds = durationSeconds;
        this.feeling = feeling;
        this.goodUse = goodUse;
    }

    // --- Getters & Setters ---

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getFeeling() { return feeling; }
    public void setFeeling(int feeling) { this.feeling = feeling; }

    public int getGoodUse() { return goodUse; }
    public void setGoodUse(int goodUse) { this.goodUse = goodUse; }
}
