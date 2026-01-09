package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player leave events to cleanup TPA data.
 * Optimized to avoid iterating over all online players.
 *
 * @author 254n_m
 * @author MindComplexity (aka Libalpm)
 * @since 2023/12/18
 */
@RequiredArgsConstructor
public class LeaveListener implements Listener {
    private final TPASection main;

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        main.cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        main.cleanupPlayer(event.getPlayer().getUniqueId());
    }
}
