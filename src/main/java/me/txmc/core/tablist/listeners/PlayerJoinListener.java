package me.txmc.core.tablist.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tablist.TabSection;
import me.txmc.core.util.FoliaCompat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

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
        Player player = event.getPlayer();
        FoliaCompat.scheduleDelayed(player, main.getPlugin(), () -> {
            if (player.isOnline()) main.setTab(player);
        }, 5L);
    }
}
