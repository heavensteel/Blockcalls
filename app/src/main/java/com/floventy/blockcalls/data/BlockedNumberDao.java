package com.floventy.blockcalls.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for blocked numbers.
 */
@Dao
public interface BlockedNumberDao {
    
    @Insert
    void insert(BlockedNumber blockedNumber);
    
    @androidx.room.Update
    void update(BlockedNumber blockedNumber);

    @Delete
    void delete(BlockedNumber blockedNumber);
    
    @Query("SELECT * FROM blocked_numbers ORDER BY dateAdded DESC")
    LiveData<List<BlockedNumber>> getAllBlockedNumbers();
    
    @Query("SELECT * FROM blocked_numbers")
    List<BlockedNumber> getAllBlockedNumbersSync();
    
    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    void deleteById(int id);
    
    @Query("SELECT COUNT(*) FROM blocked_numbers WHERE pattern = :pattern")
    int countByPattern(String pattern);

    @Query("SELECT * FROM blocked_numbers WHERE isEnabled = 1")
    List<BlockedNumber> getAllEnabledBlockedNumbersSync();
}
