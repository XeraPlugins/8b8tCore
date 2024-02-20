package me.txmc.core.vote.command;

import me.txmc.core.util.GlobalUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2024/02/02 4:19 PM
 * This file was created as a part of 8b8tCore
 */
public class VoteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player player) {
            sendPrefixedLocalizedMessage(player, "vote_info");
        } else sender.sendMessage("This command is player only");
        return true;
    }
}
