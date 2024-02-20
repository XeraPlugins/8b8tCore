package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (sender instanceof Player requested) {
            if (args.length == 0) {
                Player requester = main.getLastRequest(requested);
                acceptTPA(requested, requester);
            } else if (args.length == 1) {
                Player requester = main.hasRequested(Bukkit.getPlayer(args[0]), requested) ? Bukkit.getPlayer(args[0]) : null;
                acceptTPA(requested, requester);
            } else sendPrefixedLocalizedMessage(requested, "tpa_syntax");
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }

    private void acceptTPA(Player requested, @Nullable Player requester) {
        if (requester == null) {
            sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
            return;
        }

        requester.teleportAsync(requested.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        sendPrefixedLocalizedMessage(requester, "tpa_teleporting");
        sendPrefixedLocalizedMessage(requested, "tpa_teleporting");
        main.removeRequest(requester, requested);
    }
}
