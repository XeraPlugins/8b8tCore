package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
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
        if (main.getRequests(event.getPlayer()) == null) return;
        if (main.getRequests(event.getPlayer()).isEmpty()) return;
        for (Player player : main.getRequests(event.getPlayer())) {
            sendPrefixedLocalizedMessage(player, "tpa_to_left", event.getPlayer().getName());
        }
    }
    @EventHandler
    public void onKick(PlayerKickEvent event) {
        if (main.getRequests(event.getPlayer()) == null) return;
        if (main.getRequests(event.getPlayer()).isEmpty()) return;
        for (Player player : main.getRequests(event.getPlayer())) {
            sendPrefixedLocalizedMessage(player, "tpa_to_left", event.getPlayer().getName());
        }
    }
}
