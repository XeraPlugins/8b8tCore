package me.txmc.core.vote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * stores vote count + creation timestamp so old votes can automatically expire.
 * @author MindComplexity 
 * @since 2025/08/30
 * This file was created as a part of 8b8tCore
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteEntry {
    private int count;
    private long timestamp;
    
    public VoteEntry(int count) {
        this.count = count;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Check if this vote entry has expired
     * @param expirationDays Number of days after which votes expire (0 = never expire)
     * @return true if expired
     */
    public boolean isExpired(int expirationDays) {
        if (expirationDays <= 0) return false;
        
        long expirationTime = timestamp + (expirationDays * 24L * 60L * 60L * 1000L);
        return System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Add a vote to this entry
     */
    public void addVote() {
        this.count++;
    }
    
    /**
     * Remove a vote from this entry when they are rewarded.
     */
    public void decrementVote() {
        if (this.count > 0) {
            this.count--;
        }
    }
}
