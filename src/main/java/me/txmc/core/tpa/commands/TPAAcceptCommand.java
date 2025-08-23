package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
            Player requester = null;

            if (args.length == 0) {
                requester = main.getLastRequest(requested);
                if (requester == null) {
                    sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                    return true;
                }
            }
            else if (args.length == 1) {
                Player potentialRequester = Bukkit.getPlayer(args[0]);
                if (potentialRequester != null && main.hasRequested(potentialRequester, requested)) {
                    requester = potentialRequester;
                } else if (potentialRequester != null && main.hasHereRequested(potentialRequester, requested)) {
                    requester = potentialRequester;
                }

                if (requester == null) {
                    sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                    return true;
                }
            }
            else {
                sendPrefixedLocalizedMessage(requested, "tpa_syntax");
                return true;
            }

            if (main.hasHereRequested(requester, requested)) {
                acceptTPAHere(requested, requester);
            } else if (main.hasRequested(requester, requested)) {
                acceptTPA(requested, requester);
            }
        } else {
            sendMessage(sender, "&cYou must be a player");
        }
        return true;
    }

    private void acceptTPA(Player requested, @Nullable Player requester) {
        if (requester == null || !requester.isOnline() || requested == null || !requested.isOnline()) {
            sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
            main.removeRequest(requester, requested);
            return;
        }
        main.plugin.lastLocations.put(requester, requester.getLocation());
        requester.teleportAsync(requested.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        sendPrefixedLocalizedMessage(requester, "tpa_teleporting");
        sendPrefixedLocalizedMessage(requested, "tpa_teleporting");
        main.removeRequest(requester, requested);
    }

    private void acceptTPAHere(Player requested, @Nullable Player requester) {
        if (requester == null || !requester.isOnline() || requested == null || !requested.isOnline()) {
            sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
            main.removeHereRequest(requester, requested);
            return;
        }

        main.plugin.lastLocations.put(requested, requested.getLocation());
        requested.teleportAsync(requester.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        sendPrefixedLocalizedMessage(requester, "tpa_teleporting");
        sendPrefixedLocalizedMessage(requested, "tpa_teleporting");
        main.removeHereRequest(requester, requested);
    }
}
