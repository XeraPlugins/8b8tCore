package me.txmc.core.playerstats;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginLogger;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.playerstats.commands.PlaytimeCommand;
import me.txmc.core.playerstats.commands.SeenCommand;
import me.txmc.core.playerstats.commands.JoindateCommand;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
@RequiredArgsConstructor
public class PlayerStatsSection implements Section {

    private final Main plugin;
    private ConfigurationSection config;
    private static HashMap<String, PlayerStats> playerStats = new HashMap<String, PlayerStats>();
    private static Database db;
    private Logger log;

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public void enable() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        config = plugin.getSectionConfig(this);
        plugin.getCommand("seen").setExecutor(new SeenCommand());
        plugin.getCommand("joindate").setExecutor(new JoindateCommand());
        plugin.getCommand("playtime").setExecutor(new PlaytimeCommand());

        db = new Database(plugin.getDataFolder().getAbsolutePath(), this);
        //Setup logger
        log = new PluginLogger(plugin);
        //Setup H2 Database
        ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        for (Driver driver : drivers) {
            try {
                DriverManager.registerDriver(driver);
            } catch (SQLException ex) {
                log.log(new LogRecord(Level.SEVERE, ex.getMessage()));
            }
        }
        plugin.downloadDatabaseDrivers(plugin.getDataFolder());

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
