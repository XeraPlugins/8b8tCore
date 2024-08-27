package me.txmc.core.patch.listeners;

import me.txmc.core.util.MapCreationLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapView;

import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Handles the initialization of new maps in the game.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft, including map-related features.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Logging information when a new map is created</li>
 *     <li>Identifying players within a specific radius around the map's center</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/27 10:30 AM
 */
public class MapCreationListener implements Listener {

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView mapView = event.getMap();
        int x = mapView.getCenterX();
        int z = mapView.getCenterZ();
        int id = event.getMap().getId();

        int radius = 64;
        int minX = x - radius;
        int maxX = x + radius;
        int minZ = z - radius;
        int maxZ = z + radius;

        String players = Bukkit.getOnlinePlayers().stream()
                .filter(player -> {
                    int playerX = player.getLocation().getBlockX();
                    int playerZ = player.getLocation().getBlockZ();
                    return playerX >= minX && playerX <= maxX && playerZ >= minZ && playerZ <= maxZ;
                })
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        String message = String.format(
                "Map created with ID: %d at coordinates X: %d, Z: %d. Players in range: %s",
                id, x, z, players.isEmpty() ? "None" : players
        );

        MapCreationLogger.getLogger().log(Level.WARNING, message);
    }
}
