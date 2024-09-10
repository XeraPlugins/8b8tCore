package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Handles the /tpacancel command, which allows players to cancel teleportation requests.
 *
 * <p>This command is part of the TPA system in the 8b8tCore plugin and provides functionality for players to cancel
 * their teleportation requests.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Canceling all teleportation requests made by the player or to the player if no arguments are provided</li>
 *     <li>Canceling a specific teleportation request if a player name is provided as an argument</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/09/08 02:37 PM
 */
@RequiredArgsConstructor
public class TPACancelCommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

                    if (main.hasRequested(player, onlinePlayer)) {
                        sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", onlinePlayer.getName());
                        sendPrefixedLocalizedMessage(onlinePlayer, "tpa_request_cancelled_to", player.getName());
                        main.removeRequest(player, onlinePlayer);
                    }

                    if (main.hasHereRequested(player, onlinePlayer)) {
                        sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", onlinePlayer.getName());
                        sendPrefixedLocalizedMessage(onlinePlayer, "tpa_request_cancelled_to", player.getName());
                        main.removeHereRequest(player, onlinePlayer);
                    }
                }

            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sendPrefixedLocalizedMessage(player, "tpa_player_not_online", args[0]);
                    return true;
                }

                if (main.hasRequested(player, target)) {
                    cancelRequest(player, target);
                    sendPrefixedLocalizedMessage(player, "tpa_request_cancelled_from", target.getName());
                    sendPrefixedLocalizedMessage(target, "tpa_request_cancelled_to", player.getName());
                } else {
                    sendPrefixedLocalizedMessage(player, "tpa_no_request_found");
                }
            }
        } else {
            sendMessage(sender, "&cYou must be a player");
        }
        return true;
    }

    private void cancelRequest(Player sender, Player receiver) {
        main.removeRequest(sender, receiver);
    }
}
