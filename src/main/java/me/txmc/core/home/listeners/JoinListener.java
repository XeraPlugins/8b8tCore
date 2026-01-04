package me.txmc.core.home.listeners;

import lombok.AllArgsConstructor;
import me.txmc.core.home.HomeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listener to load and save player home data on join/leave.
 * Optimized to use UUIDs for map keys to prevent memory leaks.
 *
 * @author 254n_m
 * @author MindComplexity (aka Libalpm)
 * @since 2023/12/19
 */
@AllArgsConstructor
public class JoinListener implements Listener {
    private final HomeManager main;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        main.homes().put(uuid, main.storage().load(uuid));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        main.storage().save(main.homes().get(uuid), uuid);
        main.homes().remove(uuid);
        main.plugin().lastLocations.remove(uuid);
    }
}
