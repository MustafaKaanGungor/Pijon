package com.bringsalavat.pijon.model;

/**
 * Aggregated statistics for a single app across a set of usage log entries.
 * Compute from a list of UsageLog records; do not persist.
 */
public class AppStats {

    public String packageName;
    public String appName;
    public int total;

    // Q1 — feeling counts
    public int feelingPos;
    public int feelingNeu;
    public int feelingNeg;

    // Q2 — good-use counts
    public int goodUseYes;
    public int goodUseUnsure;
    public int goodUseNo;

    // Total monitored time (sum of interval durations logged)
    public int totalDurationSeconds;

    /** Returns the percentage (0–100) for a count out of this app's total sessions. */
    public int pct(int count) {
        return total == 0 ? 0 : Math.round(count * 100f / total);
    }
}
