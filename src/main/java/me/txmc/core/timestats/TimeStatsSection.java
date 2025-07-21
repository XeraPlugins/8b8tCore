package me.txmc.core.timestats;

import java.util.HashMap;

import org.bukkit.configuration.ConfigurationSection;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.timestats.commands.PlaytimeCommand;
import me.txmc.core.timestats.commands.SeenCommand;
import me.txmc.core.timestats.commands.JoindateCommand;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
@RequiredArgsConstructor
public class TimeStatsSection implements Section {

    private final Main plugin;
    private ConfigurationSection config;
    private static HashMap<String, PlayerStats> playerStats = new HashMap<String, PlayerStats>();
    private static Database db;

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        plugin.getCommand("seen").setExecutor(new SeenCommand());
        plugin.getCommand("joindate").setExecutor(new JoindateCommand());
        plugin.getCommand("playtime").setExecutor(new PlaytimeCommand());

        db = new Database(plugin.getDataFolder().getAbsolutePath(), this);

    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "TimeStats";
    }

    public static String formatMS(long playtime) {
        long ms = playtime;
        long seconds = playtime / 1000 / 20;
        long minutes = playtime / 60 / 20;
        long hours = playtime / 60 / 60 / 20;
        long days = playtime / 24 / 60 / 60 / 20;

        return String.format("%d : %02d : %02d : %02d",
                days, hours % 24, minutes % 60, seconds % 60);
    }

    public static void addPlayerStats(String name, PlayerStats ps) {

        playerStats.put(name, ps);
    }

    public static PlayerStats getPlayerStats(String name) {
        return playerStats.get(name);
    }

    public static void removePlayerStats(String name) {
        playerStats.remove("name");
    }

    public static Database getDB() {
        return db;
    }
}
