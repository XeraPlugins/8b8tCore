package me.txmc.core.patch.listeners;

import lombok.extern.slf4j.Slf4j;
import me.txmc.core.Main;
import me.txmc.core.util.MapCreationLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity (aka Libalpm)
 * @since 2025/01/02 10:30 AM
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
        
        if (mapView.getWorld() == null) return;

        int radius = 65;

        List<UUID> playerUUIDs = new java.util.ArrayList<>();
        List<String> playerNames = new java.util.ArrayList<>();
        
        Location mapLocation = new Location(mapView.getWorld(), x, 64, z);
        
        Bukkit.getRegionScheduler().run(plugin, mapLocation, task -> {
            for (org.bukkit.entity.Entity entity : mapLocation.getWorld()
                    .getNearbyEntities(mapLocation, radius, 256, radius)) {
                if (entity instanceof Player player) {
                    playerUUIDs.add(player.getUniqueId());
                    playerNames.add(player.getName());
                }
            }
            
            if (playerUUIDs.isEmpty()) return;
            
            String playersSign = String.join(", @", playerNames);
            
            for (UUID uuid : playerUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                
                player.getScheduler().runDelayed(plugin, playerTask -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline()) return;
                    
                    findMapInInventoryAndSign(p, id, playersSign);
                }, null, 5L);
            }
            
            String message = String.format(
                "Map created with ID: %d at coordinates X: %d, Z: %d. Players in range: %s",
                id, x, z, playersSign
            );
            MapCreationLogger.getLogger().log(Level.WARNING, message);
        });
    }
    
    private void findMapInInventoryAndSign(Player player, int mapId, String playersSign) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) main.getItemMeta();
            if (meta != null && meta.hasMapId() && meta.getMapId() == mapId) {
                signMap(main, playersSign, mapId);
                player.getInventory().setItemInMainHand(main);
                MapCreationLogger.getLogger().log(Level.WARNING, 
                    "signed main-hand map id " + mapId + " for " + player.getName());
                return;
            }
        }
        
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) off.getItemMeta();
            if (meta != null && meta.hasMapId() && meta.getMapId() == mapId) {
                signMap(off, playersSign, mapId);
                player.getInventory().setItemInOffHand(off);
                MapCreationLogger.getLogger().log(Level.WARNING, 
                    "signed off-hand map id " + mapId + " for " + player.getName());
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
                    MapCreationLogger.getLogger().log(Level.WARNING, 
                        "signed inventory map id " + mapId + " for " + player.getName());
                    return;
                }
            }
        }
    }

    private void signMap(ItemStack item, String players, int mapId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        Component signature = Component.text("by @" + players, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.hasLore() 
            ? new java.util.ArrayList<>(meta.lore()) 
            : new java.util.ArrayList<>();
            
        boolean hasAuthor = lore.stream()
            .map(c -> PlainTextComponentSerializer.plainText().serialize(c))
            .anyMatch(line -> line.contains("by @"));
            
        if (!hasAuthor) {
            lore.add(0, signature);
        }
        meta.lore(lore);

        NamespacedKey key = new NamespacedKey(plugin, "map_author");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, players);
        NamespacedKey keyId = new NamespacedKey(plugin, "map_id");
        meta.getPersistentDataContainer().set(keyId, PersistentDataType.INTEGER, mapId);
        
        item.setItemMeta(meta);
    }
}