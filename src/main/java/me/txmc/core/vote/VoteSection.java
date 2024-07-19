package me.txmc.core.vote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.home.HomeManager;
import me.txmc.core.util.GlobalUtils;
import me.txmc.core.vote.command.VoteCommand;
import me.txmc.core.vote.io.VoteIO;
import me.txmc.core.vote.listeners.VoteListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.executeCommand;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/20 6:54 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class VoteSection implements Section {
    private final Main plugin;
    private VoteIO io;
    @Getter private HashMap<String, Integer> toReward;
    @Getter private ConfigurationSection config;

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        plugin.getCommand("vote").setExecutor(new VoteCommand());
        try {
            File votesFile = new File(plugin.getSectionDataFolder(this), "OfflineVotes.json");
            if (!votesFile.exists()) votesFile.createNewFile();
            io = new VoteIO(votesFile);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to create OfflineVotes.json. Please see stacktrace below for more info");
            t.printStackTrace();
        }
        toReward = io.load();
        plugin.register(new VoteListener(this));
    }

    @Override
    public void disable() {
        io.save(toReward);
        toReward.clear();
    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public String getName() {
        return "Vote";
    }

    public void rewardPlayer(Player player) {
        if (!player.hasPermission("8b8tCore.viewdistance.6")) {
            executeCommand("lp user %s group add voter", player.getName());
            executeCommand("lp user %%s group remove default", player.getName());
        }
        //int maxHomes = ((HomeManager) plugin.getSectionByName("Home")).getMaxHomes(player);
        //if ((maxHomes + 2) >= config.getInt("MaxHomesByVoting")) return;
        sendPrefixedLocalizedMessage(player, "vote_thanks");
        //executeCommand(config.getString("AddHomePermission"), (maxHomes + 2));
    }

    public void registerOfflineVote(String username) {
        toReward.putIfAbsent(username, 1);
        toReward.computeIfPresent(username, (n, votes) -> votes + 1);
    }
    public void markAsRewarded(String username) {
        toReward.remove(username);
    }
    public Optional<Integer> getToRewardEntry(Player player) {
        return toReward.containsKey(player.getName()) ? Optional.of(toReward.get(player.getName())) : Optional.empty();
    }
}
