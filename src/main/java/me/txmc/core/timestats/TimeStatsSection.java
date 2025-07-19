package me.txmc.core.timestats;

import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.timestats.commands.PlaytimeCommand;
import me.txmc.core.timestats.commands.SeenCommand;
import me.txmc.core.timestats.commands.SinceCommand;
import me.txmc.core.tpa.commands.TPACommand;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
@RequiredArgsConstructor
public class TimeStatsSection implements Section {

    public final Main plugin;
    private ConfigurationSection config;

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        plugin.getCommand("seen").setExecutor(new SeenCommand(this));
        plugin.getCommand("since").setExecutor(new SinceCommand(this));
        plugin.getCommand("playtime").setExecutor(new PlaytimeCommand(this));
    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "TimeStats";
    }

    public String getPlaytime(OfflinePlayer player) {
        long ticks = (long) player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long seconds = ticks / 20;
        long minutes = ticks / 20 / 60;
        long hours = ticks / 20 / 60 / 60;
        long days = ticks / 20 / 60 / 60 / 24;

        return String.format("&r&3%d : %02d : %02d : %02d&r",
                days, hours % 24, minutes % 60, seconds % 60);
    }

    public String formatMS(Long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        return (String.format("&r&3%d : %02d : %02d : %02d", days, hours % 24, minutes % 60, seconds % 60) + "&r");
    }

}
