package me.txmc.core.patch.listeners;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import lombok.extern.slf4j.Slf4j;
import me.txmc.core.Main;
import me.txmc.core.util.MapCreationLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;
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

@Slf4j
public class MapCreationListener implements Listener {
    private final Main plugin;

    public MapCreationListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView mapView = event.getMap();
        int x = mapView.getCenterX();
        int z = mapView.getCenterZ();
        int id = mapView.getId();

        int radius = 65;
        int minX = x - radius;
        int maxX = x + radius;
        int minZ = z - radius;
        int maxZ = z + radius;

        Location mapLocation = new Location(mapView.getWorld(), x, 64, z);

        // Process players immediately instead of having to schedulue it out.
        // You don't really want to stress out the scheduler by spamming requests.
        List<Player> playersInZone = Bukkit.getOnlinePlayers().stream()
                .filter(player -> {
                    int playerX = player.getLocation().getBlockX();
                    int playerZ = player.getLocation().getBlockZ();
                    return playerX >= minX && playerX <= maxX && playerZ >= minZ && playerZ <= maxZ;
                })
                .collect(Collectors.toList());

        if (playersInZone.isEmpty()) {
            return;
        }

        String playersSign = playersInZone.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", @"));

        Bukkit.getRegionScheduler().runDelayed(plugin, mapLocation, task -> {
            for (Player player : playersInZone) {
                Bukkit.getRegionScheduler().runDelayed(plugin, player.getLocation(), t -> {
                    findMapInInventoryAndSign(player, id, playersSign);
                }, 1L);
            }
        }, 1L);

        String message = String.format(
                "Map created with ID: %d at coordinates X: %d, Z: %d. Players in range: %s",
                id, x, z, playersSign.isEmpty() ? "anonymous" : playersSign
        );
        MapCreationLogger.getLogger().log(Level.WARNING, message);
    }
    
    private void findMapInInventoryAndSign(Player player, int mapId, String playersSign) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) main.getItemMeta();
            if (meta != null && meta.hasMapId() && meta.getMapId() == mapId) {
                signMap(main, playersSign, mapId);
                player.getInventory().setItemInMainHand(main);
                MapCreationLogger.getLogger().log(java.util.logging.Level.WARNING, "signed main-hand map id " + mapId + " for " + player.getName());
                return;
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) off.getItemMeta();
            if (meta != null && meta.hasMapId() && meta.getMapId() == mapId) {
                signMap(off, playersSign, mapId);
                player.getInventory().setItemInOffHand(off);
                MapCreationLogger.getLogger().log(java.util.logging.Level.WARNING, "signed off-hand map id " + mapId + " for " + player.getName());
                return;
            }
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) item.getItemMeta();
                if (meta != null && meta.hasMapId() && meta.getMapId() == mapId) {
                    signMap(item, playersSign, mapId);
                    player.getInventory().setItem(i, item);
                    MapCreationLogger.getLogger().log(java.util.logging.Level.WARNING, "signed inventory map id " + mapId + " for " + player.getName());
                    break;
                }
            }
        }
    }

    private void signMap(ItemStack item, String players, int mapId) {
        ItemMeta meta = item.getItemMeta();
        String signature = ChatColor.GRAY + "by @" + players;

        if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
            boolean hasAuthor = false;
            for (String line : lore) { if (line != null && line.contains("by @")) { hasAuthor = true; break; } }
            if (!hasAuthor) {
                lore.add(0, signature);
            }
            meta.setLore(lore);

            NamespacedKey key = new NamespacedKey(plugin, "map_author");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, players);
            NamespacedKey keyId = new NamespacedKey(plugin, "map_id");
            meta.getPersistentDataContainer().set(keyId, PersistentDataType.INTEGER, mapId);
            item.setItemMeta(meta);
        }
    }
}