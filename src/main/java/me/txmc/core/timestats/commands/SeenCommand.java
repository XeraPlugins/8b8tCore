package me.txmc.core.timestats.commands;

import org.bukkit.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.txmc.core.timestats.PlayerStats;
import me.txmc.core.timestats.TimeStatsSection;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
public class SeenCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }

        Player from = (Player) sender;
        String fromName = from.getName();
        if (args.length == 0) {
            sendPrefixedLocalizedMessage(from, "seen_syntax");
            return true;
        }
        if (!args[0].matches("[A-Za-z0-9_]+")) {                                                                              //Get rid of unaccepted username characters.
            sendPrefixedLocalizedMessage(from, "bad_char");
            return true;
        }
        if (args.length > 1) {
            sendPrefixedLocalizedMessage(from, "too_many_arguments");                                                           //Check for too many aguments.
            return true;
        }
        if (!(args[0].length() <= 25)) {
            sendPrefixedLocalizedMessage(from, "name_too_long");                                                                //Check for oversized username.
            return true;
        }
        if ((args.length == 1 && args[0].equals(fromName))) {
            sendPrefixedLocalizedMessage(from, "seen_self");
            return true;
        }
        String toName = args[0];
        for (Player p : Bukkit.getOnlinePlayers()) {                                                                                //Compare name to each online player.
            if (p.getName().equals(toName)) {
                sendPrefixedLocalizedMessage(from, "seen_online", toName);
                return true;
            }
        }
        PlayerStats playerStats = TimeStatsSection.getPlayerStats(toName);

        if (playerStats != null) {
            long timeSince = playerStats.getSeen();
            Instant instantSince = Instant.ofEpochMilli(timeSince);
            ZonedDateTime zonedSince = instantSince.atZone(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            String formatted = formatter.format(zonedSince);
            String seen = TimeStatsSection.formatMS(playerStats.getSeen());
            sendPrefixedLocalizedMessage(from, "seen", toName);
            sendMessage(from, "     &3" + formatted + "&r");
            return true;
        }
        playerStats = TimeStatsSection.getDB().fetchPlayer(toName);                                                                 //Otherwise check that DB for offline players.
        if (playerStats != null) {
            long timeSince = playerStats.getSeen();
            Instant instantSince = Instant.ofEpochMilli(timeSince);
            ZonedDateTime zonedSince = instantSince.atZone(ZoneId.of("America/New_York"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            String formatted = formatter.format(zonedSince);
            String seen = TimeStatsSection.formatMS(playerStats.getSeen());
            sendPrefixedLocalizedMessage(from, "seen", toName);
            sendMessage(from, "     &3" + formatted + "&r");
            return true;
        }
        sendPrefixedLocalizedMessage(from, "never_joined");
        return true;

    }

}
