package me.txmc.core.vote;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import org.bukkit.Bukkit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.vote.command.VoteCommand;
import me.txmc.core.vote.io.VoteIO;
import me.txmc.core.vote.listeners.VoteListener;
import me.txmc.core.vote.listeners.JoinListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

@RequiredArgsConstructor
public class VoteSection implements Section {
    private final Main plugin;
    @Getter private VoteIO io;
    @Getter private HashMap<String, Integer> toReward;
    @Getter private ConfigurationSection config;

    @Override
    public void enable() {
        // Load configuration section
        config = plugin.getSectionConfig(this);
        // Register vote command
        plugin.getCommand("vote").setExecutor(new VoteCommand());
        // Prepare offline vote storage
        try {
            File votesFile = new File(plugin.getSectionDataFolder(this), "OfflineVotes.json");
            if (!votesFile.exists()) votesFile.createNewFile();
            io = new VoteIO(votesFile);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create OfflineVotes.json. See stacktrace:", t);
        }
        // Load pending offline votes
        toReward = io.load();
        // Register listeners
        plugin.register(new VoteListener(this));
        plugin.register(new JoinListener(this));
    }

    @Override
    public void disable() {
        // Save any remaining offline votes
        if (io != null) io.save(toReward);
        toReward.clear();
    }

    @Override
    public void reloadConfig() {
        // Reload only the vote config section
        config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "Vote";
    }

    /**
     * Reward a player for voting.
     */
    public void rewardPlayer(Player player) {
        // Thank the voting player
        sendPrefixedLocalizedMessage(player, "vote_thanks");

        // Notify others about this vote
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getName().equals(player.getName())) {
                sendPrefixedLocalizedMessage(p, "vote_announcement", player.getName());
            }
        }

        // Execute each configured reward command (commands use '%s' for player name)
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
     * Called when a vote arrives for a player not currently online.
     */
    public void registerOfflineVote(String username) {
        toReward.putIfAbsent(username, 1);
        toReward.computeIfPresent(username, (n, votes) -> votes + 1);
    }

    /**
     * Returns pending offline vote count for a player.
     */
    public Optional<Integer> getToRewardEntry(Player player) {
        return Optional.ofNullable(toReward.get(player.getName()));
    }

    /**
     * Remove a player from offline reward map.
     */
    public void markAsRewarded(String username) {
        toReward.remove(username);
    }
}
