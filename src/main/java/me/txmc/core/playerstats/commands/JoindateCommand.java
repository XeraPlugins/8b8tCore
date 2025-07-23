package me.txmc.core.playerstats.commands;

import org.bukkit.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.txmc.core.playerstats.PlayerStatsSection;
import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.bukkit.command.FormattedCommandAlias;

import me.txmc.core.playerstats.PlayerStats;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
public class JoindateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }

        Player from = (Player) sender;
        String fromName = from.getName();
        PlayerStats fromStats = PlayerStatsSection.getDB().fetchPlayer(fromName);
        long joindate = fromStats.getJoindate();
        Instant instantJoined = Instant.ofEpochMilli(joindate);
        ZonedDateTime zonedJoined = instantJoined.atZone(ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedJoined = formatter.format(zonedJoined);

        if (args.length == 0 || (args.length == 1 && args[0].equals(fromName))) {                               //Check for empty arguments or own name
            sendPrefixedLocalizedMessage(from, "joined_self");
            sendMessage(from, "     &3" + formattedJoined + "&r");
            return true;
        }
        if (!args[0].matches("[A-Za-z0-9_]+")) {                                                                //Get rid of unaccepted username characters
            sendPrefixedLocalizedMessage(from, "bad_char");
            return true;
        }
        if (args.length > 1) {                                                                                  //Check for too many arguments
            sendPrefixedLocalizedMessage(from, "too_many_arguments");
            return true;
        }
        if (!(args[0].length() <= 25)) {                                                                        //Check for oversized name
            sendPrefixedLocalizedMessage(from, "name_too_long");
            return true;
        }

        String toName = args[0];
        for (Player p : Bukkit.getOnlinePlayers()) {                                                            //Compare name to each online player
            if (p.getName().equals(toName)) {
                joindate = PlayerStatsSection.getPlayerStats(toName).getJoindate();
                instantJoined = Instant.ofEpochMilli(joindate);
                zonedJoined = instantJoined.atZone(ZoneId.of("America/New_York"));
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
                formattedJoined = formatter.format(zonedJoined);

                sendPrefixedLocalizedMessage(from, "joined", toName);
                sendMessage(from, "     &3" + formattedJoined + "&r");
                return true;
            }
        }
        PlayerStats playerStats = PlayerStatsSection.getDB().fetchPlayer(toName);                                 //Otherwise check that DB for offline players.
        if (playerStats != null) {
            joindate = playerStats.getJoindate();
            instantJoined = Instant.ofEpochMilli(joindate);
            zonedJoined = instantJoined.atZone(ZoneId.of("America/New_York"));
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            formattedJoined = formatter.format(zonedJoined);

            sendPrefixedLocalizedMessage(from, "joined", toName);
            sendMessage(from, "     &3" + formattedJoined + "&r");
            return true;
        } else {
            sendPrefixedLocalizedMessage(from, "never_joined");
            return true;
        }
    }
}
