package com.bringsalavat.pijon.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.bringsalavat.pijon.model.UsageLog;

/**
 * Room database for Pijon. Currently holds the usage_log table.
 */
@Database(entities = {UsageLog.class}, version = 2, exportSchema = false)
public abstract class PijonDatabase extends RoomDatabase {

    private static volatile PijonDatabase INSTANCE;

    public abstract UsageLogDao usageLogDao();

    public static PijonDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PijonDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            PijonDatabase.class,
                            "pijon_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
