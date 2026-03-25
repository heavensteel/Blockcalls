package com.floventy.blockcalls.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a blocked number pattern in the database.
 * Supports both exact numbers and wildcard patterns (e.g., "0850 *****").
 */
@Entity(tableName = "blocked_numbers")
public class BlockedNumber {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String pattern;
    private boolean isWildcard;
    private long dateAdded;
    private boolean isEnabled;
    
    public BlockedNumber(String pattern, boolean isWildcard, long dateAdded) {
        this.pattern = pattern;
        this.isWildcard = isWildcard;
        this.dateAdded = dateAdded;
        this.isEnabled = true;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public boolean isWildcard() {
        return isWildcard;
    }
    
    public void setWildcard(boolean wildcard) {
        isWildcard = wildcard;
    }
    
    public long getDateAdded() {
        return dateAdded;
    }
    
    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
