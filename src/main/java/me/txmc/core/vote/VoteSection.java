package me.txmc.core.vote;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import org.bukkit.Bukkit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.vote.command.VoteCommand;
import me.txmc.core.vote.io.VoteSQLiteStorage;
import me.txmc.core.vote.listeners.VotifierListener;
import me.txmc.core.vote.listeners.JoinListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;


/**
 * Handles the Voter system's primary logic + cleanup. 
 * @author MindComplexity (Revamped System) Old System by 254_m.
 * @since 2025/08/30
 * This file was created as a part of 8b8tCore
 */

@RequiredArgsConstructor
public class VoteSection implements Section {
    private final Main plugin;
    @Getter private VoteSQLiteStorage sqliteStorage;
    @Getter private HashMap<String, VoteEntry> toReward;
    @Getter private ConfigurationSection config;

    @Override
    public void enable() {        
        config = plugin.getSectionConfig(this);
        plugin.getLogger().info("VoteSection: Config loaded: " + (config != null ? "SUCCESS" : "NULL"));
        
        plugin.getCommand("vote").setExecutor(new VoteCommand(this));
        plugin.getLogger().info("VoteSection: Vote command registered");
        
        try {
            File votesFile = new File(plugin.getSectionDataFolder(this), "votes.db");
            // plugin.getLogger().info("VoteSection: SQLite database path: " + votesFile.getAbsolutePath());
            sqliteStorage = new VoteSQLiteStorage(votesFile);
            // plugin.getLogger().info("VoteSection: SQLite storage initialized");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage. See stacktrace:", t);
        }
        
        toReward = sqliteStorage.load();
        // plugin.getLogger().info("VoteSection: Loaded " + toReward.size() + " pending votes");
        
        cleanupExpiredVotes();
        
        plugin.register(new VotifierListener(this));
        plugin.register(new JoinListener(this));
        plugin.getLogger().info("VoteSection: Listeners registered");
        
        plugin.getExecutorService().scheduleAtFixedRate(() -> {
            if (sqliteStorage != null && toReward != null) {
                plugin.getLogger().info("VoteSection: Auto-saving " + toReward.size() + " votes");
                sqliteStorage.save(toReward);
            }
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        plugin.getLogger().info("VoteSection: Successfully enabled!");
    }

    @Override
    public void disable() {
        if (sqliteStorage != null) {
            sqliteStorage.save(toReward);
            sqliteStorage.close();
        }
        toReward.clear();
    }

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "Vote";
    }

  
    public void announceVote(String voterName) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPrefixedLocalizedMessage(p, "vote_announcement", voterName);
        }
    }

    /**
     * Execute reward commands.
     */
    private void executeRewards(Player player) {
        for (String cmd : config.getStringList("Rewards")) {
            String toRun = String.format(cmd, player.getName());
            try {
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        toRun
                );
            } catch (Exception ex) {
                plugin.getLogger().warning(
                        "Failed to run vote reward command: " + toRun + " – " + ex.getMessage()
                );
            }
        }
    }

    /**
     * Reward a player for voting
     */
    public void rewardPlayer(Player player) {
        sendPrefixedLocalizedMessage(player, "vote_thanks");

        announceVote(player.getName());

        executeRewards(player);
        
        VoteEntry entry = toReward.get(player.getName().toLowerCase());
        if (entry != null && entry.getCount() > 0) {
            entry.decrementVote();
            if (sqliteStorage != null) {
                sqliteStorage.save(toReward);
            }
        }
    }

    /**
     * Reward offline votes without announcements. But thank them later for supporting the server.
     */
    public void rewardOfflineVotes(Player player, int voteCount) {
        sendPrefixedLocalizedMessage(player, "vote_thanks");
        
        for (int i = 0; i < voteCount; i++) {
            executeRewards(player);
        }
        
        markAsRewarded(player.getName().toLowerCase());
        
        if (sqliteStorage != null) {
            sqliteStorage.save(toReward);
        }
    }

    /**
     * Check if a player's voter role has expired
     */
    public boolean hasVoterRoleExpired(String username) {
        VoteEntry entry = toReward.get(username.toLowerCase());
        if (entry == null) return true;
        
        int expirationDays = config.getInt("VoterRoleExpirationDays", 30);
        if (expirationDays <= 0) return false;
        
        long expirationTime = entry.getTimestamp() + (expirationDays * 24L * 60L * 60L * 1000L);
        return System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Get remaining voter role time in days
     */
    public long getRemainingVoterDays(String username) {
        VoteEntry entry = toReward.get(username.toLowerCase());
        if (entry == null) return 0;
        
        int expirationDays = config.getInt("VoterRoleExpirationDays", 30);
        if (expirationDays <= 0) return -1;
        
        long expirationTime = entry.getTimestamp() + (expirationDays * 24L * 60L * 60L * 1000L);
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining / (24L * 60L * 60L * 1000L)); 
    }
    
    /**
     * Remove voter role from player using configurable expiration command
     */
    public void removeVoterRole(String username) {
        try {
            String expirationCommand = config.getString("ExpirationCommand", "lp user %s group remove voter");
            String commandToRun = String.format(expirationCommand, username);
            plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                commandToRun
            );
            // plugin.getLogger().info("Executed expiration command for " + username + ": " + commandToRun);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute expiration command for " + username + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if player has voter role by checking bukkit permissions.
     */
    public boolean hasVoterRole(String username) {
        try {
            Player player = Bukkit.getPlayerExact(username);
            if (player != null && player.isOnline()) {
                boolean hasRole = player.hasPermission("group.voter") || player.hasPermission("voter");
                // plugin.getLogger().info("Checked online player " + username + " for voter role: " + hasRole);
                return hasRole;
            }
            // plugin.getLogger().info("Cannot check voter role for offline player " + username + " - will check on join");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check voter role for " + username + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Auto-migrate legacy player if they have voter role but aren't tracked.
     */
    public void checkAndMigrateLegacyPlayer(String username) {
        if (!config.getBoolean("EnableLegacyPlayerMigration", true)) {
            return;
        }
        
        if (toReward.containsKey(username.toLowerCase())) {
            return;
        }
        
        Player player = Bukkit.getPlayerExact(username);
        if (player != null && player.isOnline()) {
            if (hasVoterRole(username)) {
                int defaultDaysRemaining = config.getInt("LegacyPlayerDefaultDaysRemaining", 20);
                migrateLegacyPlayer(username, defaultDaysRemaining);
                // plugin.getLogger().info("Auto-migrated legacy player " + username + " with " + defaultDaysRemaining + " days remaining");
            }
        }
    }
    
    /**
     * Register a vote for online or offline players.
     */
    public boolean registerVote(String username) {
        plugin.getLogger().info("VoteSection: Attempting to register vote for " + username);
        
        Player player = Bukkit.getPlayerExact(username);
        if (player != null && player.isOnline()) {
            checkAndMigrateLegacyPlayer(username);
        }
        
        VoteEntry existingEntry = toReward.get(username.toLowerCase());
        if (existingEntry != null) {
            extendVoterRole(username, existingEntry);
            long remainingDays = getRemainingVoterDays(username);
            // plugin.getLogger().info("VoteSection: Extended voter role for " + username + ". Now has " + remainingDays + " days remaining");
        } else {
            toReward.put(username.toLowerCase(), new VoteEntry(1));
            // plugin.getLogger().info("Created new voter role for " + username + " for " + config.getInt("VoterRoleExpirationDays", 30) + " days");
        }
        
        plugin.getLogger().info("VoteSection: Vote registered for " + username + ". Total tracked votes: " + toReward.size());
        
        if (sqliteStorage != null) {
            // plugin.getLogger().info("VoteSection: Saving votes to SQLite...");
            sqliteStorage.save(toReward);
        } else {
            // plugin.getLogger().warning("VoteSection: SQLiteStorage is null! Cannot save votes!");
        }
        
        return true;
    }
    
    /**
     * Extend voter role expiration by adding more time instead of resetting
     */
    public void extendVoterRole(String username, VoteEntry existingEntry) {
        int expirationDays = config.getInt("VoterRoleExpirationDays", 30);
        
        long currentExpirationTime = existingEntry.getTimestamp() + (expirationDays * 24L * 60L * 60L * 1000L);
        
        long baseTime = Math.max(currentExpirationTime, System.currentTimeMillis());
        
        long newTimestamp = baseTime;
        
        VoteEntry newEntry = new VoteEntry(existingEntry.getCount() + 1, newTimestamp);
        toReward.put(username.toLowerCase(), newEntry);
        
        long totalDaysRemaining = (baseTime + (expirationDays * 24L * 60L * 60L * 1000L) - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L);
        plugin.getLogger().info("Extended voter role for " + username + " by " + expirationDays + " days. Total remaining: " + totalDaysRemaining + " days");
    }
    
    /**
     * Migrate a legacy player who already has voter role to our tracking system
     */
    public void migrateLegacyPlayer(String username, long daysRemaining) {
        long currentTime = System.currentTimeMillis();
        int totalExpirationDays = config.getInt("VoterRoleExpirationDays", 30);
        

        long daysRemainingMillis = daysRemaining * 24L * 60L * 60L * 1000L;
        long totalExpirationMillis = totalExpirationDays * 24L * 60L * 60L * 1000L;
        long timestamp = currentTime + daysRemainingMillis - totalExpirationMillis;
        
 
        VoteEntry entry = new VoteEntry(0, timestamp); 
        toReward.put(username.toLowerCase(), entry);
        
        if (sqliteStorage != null) {
            sqliteStorage.save(toReward);
        }
        
        plugin.getLogger().info("Migrated legacy player " + username + " to tracking system (" + daysRemaining + " days remaining)");
    }



    /**
     * Returns pending offline vote count for a player.
     */
    public Optional<Integer> getToRewardEntry(Player player) {
        VoteEntry entry = toReward.get(player.getName().toLowerCase());
        return entry != null ? Optional.of(entry.getCount()) : Optional.empty();
    }

    /**
     * Remove a player from offline reward map.
     */
    public void markAsRewarded(String username) {
        toReward.remove(username.toLowerCase());
    }

    /**
     * Cleaner for expired votes enteries and voter roles if they expire.
     */
    public void cleanupExpiredVotes() {
        int offlineExpirationDays = config.getInt("OfflineVoteExpirationDays", 7);
        int voterRoleExpirationDays = config.getInt("VoterRoleExpirationDays", 30);
        
        int removedCount = 0;
        int rolesRemovedCount = 0;
        
        var iterator = toReward.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String username = entry.getKey();
            VoteEntry voteEntry = entry.getValue();
            
            if (hasVoterRoleExpired(username)) {
                removeVoterRole(username);
                rolesRemovedCount++;
                iterator.remove();
                removedCount++;
                plugin.getLogger().info("Removed " + username + " from voting database after role expiration");
            }
            else if (offlineExpirationDays > 0 && voteEntry.getCount() > 0 && voteEntry.isExpired(offlineExpirationDays)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " expired vote entries");
        }
        if (rolesRemovedCount > 0) {
            plugin.getLogger().info("Removed " + rolesRemovedCount + " expired voter roles");
        }
        
        if ((removedCount > 0 || rolesRemovedCount > 0) && sqliteStorage != null) {
            sqliteStorage.save(toReward);
        }
    }
}
