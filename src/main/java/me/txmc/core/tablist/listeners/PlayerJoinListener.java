package me.txmc.core.tablist.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tablist.TabSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author 254n_m
 * @since 2023/12/17 11:58 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class PlayerJoinListener implements Listener {
    private final TabSection main;
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(main.getPlugin(), task -> {
            if (event.getPlayer().isOnline()) main.setTab(event.getPlayer());
        }, null, 5L);
    }
}
