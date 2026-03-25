package com.floventy.blockcalls.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

/**
 * Room database for the BlockCalls application.
 * Contains blocked numbers and blocked call logs.
 */
@Database(entities = {BlockedNumber.class, BlockedCall.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static AppDatabase instance;
    
    public abstract BlockedNumberDao blockedNumberDao();
    public abstract BlockedCallDao blockedCallDao();
    
    /**
     * Default blocking rules added on first database creation.
     */
    private static final String[] DEFAULT_PATTERNS = {"0850*", "0212*", "0216*"};
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "block_calls_db"
            )
            .fallbackToDestructiveMigration()
            .addCallback(new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    // Insert default blocking rules on first DB creation
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase database = getInstance(context);
                        long now = System.currentTimeMillis();
                        for (String pattern : DEFAULT_PATTERNS) {
                            if (database.blockedNumberDao().countByPattern(pattern) == 0) {
                                BlockedNumber blockedNumber = new BlockedNumber(pattern, true, now);
                                blockedNumber.setEnabled(true);
                                database.blockedNumberDao().insert(blockedNumber);
                            }
                        }
                    });
                }
            })
            .build();
        }
        return instance;
    }
}
