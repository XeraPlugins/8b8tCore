package me.txmc.core.timestats.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import me.txmc.core.timestats.TimeStatsSection;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import me.txmc.core.timestats.NameLookup;

import java.util.Date;

import org.bukkit.plugin.Plugin;

/**
 *
 * @author 5aks
 * @since 7/18/2025 3:40 PM This file was created as a part of 8b8tCore
 *
 */
@RequiredArgsConstructor
public class SinceCommand implements CommandExecutor {

    private final TimeStatsSection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        OfflinePlayer fromOff = (OfflinePlayer) sender;
        Player fromOn = (Player) sender;
        if (args.length == 0) {
            Date since = since(fromOff);
            sendPrefixedLocalizedMessage(fromOn, "since_self");
            sendMessage(sender, "   " + since);
            return true;
        }
        if (args.length > 1) {
            sendPrefixedLocalizedMessage(fromOn, "too_many_arguments");
            return true;
        }
        if (!(args[0].length() <= 25)) {
            sendPrefixedLocalizedMessage(fromOn, "name_too_long");
            return true;
        }
        if (!args[0].matches("[A-Za-z0-9_]+")) {
            sendPrefixedLocalizedMessage(fromOn, "bad_char");
            return true;
        }
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }
        OfflinePlayer to = Bukkit.getOfflinePlayer(args[0]);
        String[] toName = new String[1];
        toName[0] = to.getName();
        if (NameLookup.nameLookUp(toName[0])) {
            if (to != fromOff) {
                if (to.hasPlayedBefore()) {
                    Date since = since(to);
                    sendPrefixedLocalizedMessage(fromOn.getPlayer(), "since", toName[0]);
                    sendMessage(sender, "   " + since);
                    return true;
                }
                sendPrefixedLocalizedMessage(fromOn, "never_joined");
                return true;
            }
            sendPrefixedLocalizedMessage(fromOn, "since_self");
            return true;

        }
        sendPrefixedLocalizedMessage(fromOn, "player_not_exist");
        return true;
    }

    private Date since(OfflinePlayer to) {

        return new Date(to.getFirstPlayed());
    }

}
