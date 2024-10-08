package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/18 9:49 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPADenyCommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player requested) {
            Player requester = null;

            if (args.length == 0) {
                requester = main.getLastRequest(requested);
                denyTPA(requested, requester);
            }
            else if (args.length == 1) {
                requester = Bukkit.getPlayer(args[0]);

                if (requester != null && main.hasRequested(requester, requested)) {
                    denyTPA(requested, requester);
                } else {
                    sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
                }
            }

            else {
                sendPrefixedLocalizedMessage(requested, "tpa_syntax");
            }
        } else {
            sendMessage(sender, "&cYou must be a player");
        }
        return true;
    }

    private void denyTPA(Player requested, @Nullable Player requester) {
        if (requester == null) {
            sendPrefixedLocalizedMessage(requested, "tpa_no_request_found");
            return;
        }

        sendPrefixedLocalizedMessage(requester, "tpa_request_denied_from", requested.getName());
        sendPrefixedLocalizedMessage(requested, "tpa_request_denied_to", requester.getName());

        if(main.hasRequested(requester, requested)) main.removeRequest(requester, requested);
        if(main.hasHereRequested(requester, requested)) main.removeHereRequest(requester, requested);
    }
}
