package me.txmc.core.timestats.commands;

import org.bukkit.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import me.txmc.core.timestats.PlayerStats;
import me.txmc.core.timestats.TimeStatsSection;

import static java.lang.System.currentTimeMillis;
import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
public class PlaytimeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }
        Player from = (Player) sender;
        String fromName = from.getName();
        if (!args[0].matches("[A-Za-z0-9_]+")) {
            sendPrefixedLocalizedMessage(from, "bad_char");                                                                             //Get rid of unaccepted username characters
            return true;
        }
        if (args.length == 0 || (args.length == 1 && args[0].equals(fromName))) {                                                       //Check for empty arguments or own name

            PlayerStats playerStats = TimeStatsSection.getPlayerStats(fromName);
            long now = currentTimeMillis();
            long seen = playerStats.getSeen();
            long playtime = playerStats.getPlaytime() + (now - seen);
            String output = (TimeStatsSection.formatMS(playtime));
            sendPrefixedLocalizedMessage(from, "playtime", fromName);
            sendMessage(sender, "     &3" + output + "&r");
            return true;
        }
        if (args.length > 1) {
            sendPrefixedLocalizedMessage(from, "too_many_arguments");                                                               //Check for too many aguments
            return true;
        }
        if (!(args[0].length() <= 25)) {
            sendPrefixedLocalizedMessage(from, "name_too_long");                                                                    //Check for oversized username
            return true;
        }

        String toName = args[0];
        for (Player p : Bukkit.getOnlinePlayers()) {                                                                                    //Compare name to each online player

            if (p.getName().equals(toName)) {
                PlayerStats playerStats = TimeStatsSection.getPlayerStats(toName);
                long now = currentTimeMillis();
                long seen = playerStats.getSeen();
                long playtime = playerStats.getPlaytime() + (now - seen);
                String output = TimeStatsSection.formatMS(playtime);
                sendPrefixedLocalizedMessage(from, "playtime", toName);
                sendMessage(from, "     &3" + output + "&r");
                return true;
            }
            PlayerStats playerStats = TimeStatsSection.getDB().fetchPlayer(toName);
            if (playerStats != null) {
                long now = currentTimeMillis();
                long seen = playerStats.getSeen();
                long playtime = playerStats.getPlaytime() + (now - seen);
                String output = TimeStatsSection.formatMS(playtime);

                sendPrefixedLocalizedMessage(from, "playtime", toName);
                sendMessage(from, "     &3" + output + "&r");
                return true;
            }
        }

        sendPrefixedLocalizedMessage(from, "never_joined");
        return true;
    }
}
