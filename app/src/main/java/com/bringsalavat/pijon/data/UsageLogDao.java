package com.bringsalavat.pijon.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.bringsalavat.pijon.model.UsageLog;

import java.util.List;

/**
 * Data Access Object for usage log entries.
 */
@Dao
public interface UsageLogDao {

    @Insert
    void insert(UsageLog log);

    @Query("SELECT * FROM usage_log ORDER BY timestamp DESC")
    List<UsageLog> getAllLogs();

    @Query("SELECT * FROM usage_log WHERE packageName = :packageName ORDER BY timestamp DESC")
    List<UsageLog> getLogsForApp(String packageName);

    @Query("SELECT * FROM usage_log WHERE timestamp >= :fromMs ORDER BY timestamp DESC")
    List<UsageLog> getLogsSince(long fromMs);

    @Query("SELECT COUNT(*) FROM usage_log")
    int getLogCount();

    @Query("DELETE FROM usage_log")
    void deleteAll();
}
