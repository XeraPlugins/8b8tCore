package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/18 10:12 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class LeaveListener implements Listener {
    private final TPASection main;

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        handlePlayerLeave(event.getPlayer());
    }

    private void handlePlayerLeave(Player player) {

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {

            if (main.hasRequested(onlinePlayer, player)) {
                sendPrefixedLocalizedMessage(onlinePlayer, "tpa_to_left", player.getName());
                main.removeRequest(onlinePlayer, player);
            }

            if (main.hasRequested(player, onlinePlayer)) {
                sendPrefixedLocalizedMessage(onlinePlayer, "tpa_to_left", player.getName());
                main.removeRequest(player, onlinePlayer);
            }

            if (main.hasHereRequested(onlinePlayer, player)) {
                sendPrefixedLocalizedMessage(onlinePlayer, "tpa_to_left", player.getName());
                main.removeHereRequest(onlinePlayer, player);
            }

            if (main.hasHereRequested(player, onlinePlayer)) {
                sendPrefixedLocalizedMessage(onlinePlayer, "tpa_to_left", player.getName());
                main.removeHereRequest(player, onlinePlayer);
            }
        }
    }
}
