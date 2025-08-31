package me.txmc.core.vote.command;

import me.txmc.core.vote.VoteEntry;
import me.txmc.core.vote.VoteSection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m + MindComplexity
 * @since 2025/08/28
 * This file was created as a part of 8b8tCore
 */
public class VoteCommand implements CommandExecutor {
    private final VoteSection voteSection;
    
    public VoteCommand(VoteSection voteSection) {
        this.voteSection = voteSection;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Testing for Vote command. Commented out incase of any problems later.  
        // Debug: Always show this message to confirm command is working
        // sendMessage(sender, "&eVoteCommand executed! Args: " + String.join(" ", args));
    
        /*
        if (args.length > 0 && sender.isOp()) {
            if (args[0].equalsIgnoreCase("test") && args.length >= 2) {
                String targetPlayer = args[1];
                Player target = Bukkit.getPlayerExact(targetPlayer);
                
                // Try to register the vote (checks if voter role has expired)
                boolean voteAccepted = voteSection.registerVote(targetPlayer);
                
                if (!voteAccepted) {
                    long remainingDays = voteSection.getRemainingVoterDays(targetPlayer);
                    sendMessage(sender, "&c" + targetPlayer + " still has voter role for " + remainingDays + " more days");
                    return true;
                }
                
                if (target != null && target.isOnline()) {
                    // Test online vote - reward immediately
                    voteSection.rewardPlayer(target);
                    sendMessage(sender, "&aTest vote given to online player: " + targetPlayer + " (voter role granted for 30 days)");
                } else {
                    // Test offline vote - just announce
                    voteSection.announceVote(targetPlayer);
                    sendMessage(sender, "&aTest offline vote registered for: " + targetPlayer + " (voter role granted for 30 days)");
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("clear") && args.length >= 2) {
                String targetPlayer = args[1].toLowerCase();
                voteSection.markAsRewarded(targetPlayer);
                sendMessage(sender, "&cCleared offline votes for: " + targetPlayer);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("check") && args.length >= 2) {
                String targetPlayer = args[1].toLowerCase();
                VoteEntry entry = voteSection.getToReward().get(targetPlayer);
                if (entry != null) {
                    long daysOld = (System.currentTimeMillis() - entry.getTimestamp()) / (24L * 60L * 60L * 1000L);
                    sendMessage(sender, "&e" + targetPlayer + " has " + entry.getCount() + " pending offline votes (" + daysOld + " days old)");
                } else {
                    sendMessage(sender, "&e" + targetPlayer + " has 0 pending offline votes");
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("cleanup")) {
                voteSection.cleanupExpiredVotes();
                sendMessage(sender, "&aExpired vote cleanup completed");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("save")) {
                voteSection.getIo().save(voteSection.getToReward());
                sendMessage(sender, "&aManually saved offline votes to JSON");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("list")) {
                var toReward = voteSection.getToReward();
                if (toReward.isEmpty()) {
                    sendMessage(sender, "&eNo pending votes found");
                } else {
                    sendMessage(sender, "&ePending votes:");
                    toReward.forEach((name, entry) -> {
                        long daysOld = (System.currentTimeMillis() - entry.getTimestamp()) / (24L * 60L * 60L * 1000L);
                        long remainingDays = voteSection.getRemainingVoterDays(name);
                        String status = entry.getCount() > 0 ? "unrewarded" : "voter role (" + remainingDays + " days left)";
                        sendMessage(sender, "&f- " + name + ": " + status + " (voted " + daysOld + " days ago)");
                    });
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("migrate") && args.length >= 3) {
                String targetPlayer = args[1].toLowerCase();
                try {
                    int daysAgo = Integer.parseInt(args[2]);
                    voteSection.migrateLegacyPlayer(targetPlayer, daysAgo);
                    sendMessage(sender, "&aMigrated legacy player " + targetPlayer + " (voted " + daysAgo + " days ago)");
                } catch (NumberFormatException e) {
                    sendMessage(sender, "&cInvalid number: " + args[2]);
                }
                return true;
            }
            
            if (args[0].equalsIgnoreCase("scanlegacy")) {
                int migratedCount = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String name = player.getName().toLowerCase();
                    if (!voteSection.getToReward().containsKey(name) && voteSection.hasVoterRole(name)) {
                        int defaultDaysRemaining = voteSection.getConfig().getInt("LegacyPlayerDefaultDaysRemaining", 20);
                        voteSection.migrateLegacyPlayer(name, defaultDaysRemaining);
                        migratedCount++;
                    }
                }
                sendMessage(sender, "&aMigrated " + migratedCount + " legacy players who are currently online");
                return true;
            }
        }
        */
        
        if (sender instanceof Player player) {
            sendPrefixedLocalizedMessage(player, "vote_info");
        } else {
            sendMessage(sender, "&cThis command is player only");
        }
        return true;
    }
}
