package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/18 4:23 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPAAcceptCommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player to) {
            Player from = main.findTpRequester(to);
            if (from == null) {
                sendPrefixedLocalizedMessage(to, "tpa_no_request_found");
                return true;
            }
            from.teleportAsync(to.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            sendPrefixedLocalizedMessage(from, "tpa_teleporting");
            sendPrefixedLocalizedMessage(to, "tpa_teleporting");
            main.removeRequest(to);
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }
}
