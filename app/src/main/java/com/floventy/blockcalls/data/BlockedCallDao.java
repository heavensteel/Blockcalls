package com.floventy.blockcalls.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for blocked call logs.
 */
@Dao
public interface BlockedCallDao {
    
    @Insert
    void insert(BlockedCall blockedCall);
    
    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    LiveData<List<BlockedCall>> getAllBlockedCalls();
    
    @Query("DELETE FROM blocked_calls")
    void deleteAll();
    
    @Query("SELECT COUNT(*) FROM blocked_calls")
    int getCount();
}
