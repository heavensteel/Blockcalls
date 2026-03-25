package com.floventy.blockcalls.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a blocked call log entry.
 * Records when a call was blocked and which pattern matched it.
 */
@Entity(tableName = "blocked_calls")
public class BlockedCall {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String phoneNumber;
    private long timestamp;
    private String matchedPattern;
    
    public BlockedCall(String phoneNumber, long timestamp, String matchedPattern) {
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
        this.matchedPattern = matchedPattern;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMatchedPattern() {
        return matchedPattern;
    }
    
    public void setMatchedPattern(String matchedPattern) {
        this.matchedPattern = matchedPattern;
    }
}
