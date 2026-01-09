package me.txmc.core.patch.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.txmc.core.Main;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Map Creation Listener.
 * This file is apart of the 8b8tCore plugin.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/01/08
 */
@Slf4j
@RequiredArgsConstructor
public class MapCreationListener implements Listener {
    
    private final Main plugin;
    private static final int SCAN_RADIUS = 64;

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView mapView = event.getMap();
        World world = mapView.getWorld();
        
        if (world == null) return;

        int x = mapView.getCenterX();
        int z = mapView.getCenterZ();
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Location center = new Location(world, x, 64, z);

        Bukkit.getRegionScheduler().run(plugin, center, task -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) return;

            java.util.Collection<Player> nearbyPlayers = world.getNearbyPlayers(center, SCAN_RADIUS);
            
            if (nearbyPlayers.isEmpty()) return;

            String playerNames = nearbyPlayers.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", @"));

            for (Player player : nearbyPlayers) {
                signPlayerMap(player, mapView.getId(), playerNames);
            }
        });
    }

    private void signPlayerMap(Player player, int mapId, String signatureText) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (signIfMatches(mainHand, mapId, signatureText, player)) return;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (signIfMatches(offHand, mapId, signatureText, player)) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                signIfMatches(item, mapId, signatureText, player);
            }
        }
    }

    private boolean signIfMatches(ItemStack item, int mapId, String signatureText, Player player) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof MapMeta mapMeta)) return false;

        if (!mapMeta.hasMapId() || mapMeta.getMapId() != mapId) return false;

        NamespacedKey key = new NamespacedKey(plugin, "map_id");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return true;
        }

        applySignature(item, meta, signatureText, mapId);
        
        log.debug("Signed map {} for {}", mapId, player.getName());
        
        return true;
    }

    private void applySignature(ItemStack item, ItemMeta meta, String players, int mapId) {
        Component signature = Component.text("by @" + players, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        
        boolean alreadySigned = lore.stream()
                .map(GlobalUtils::getStringContent)
                .anyMatch(line -> line.contains("by @"));

        if (!alreadySigned) {
            lore.add(0, signature);
        }
        
        meta.lore(lore);

        NamespacedKey keyAuthor = new NamespacedKey(plugin, "map_author");
        meta.getPersistentDataContainer().set(keyAuthor, PersistentDataType.STRING, players);
        
        NamespacedKey keyId = new NamespacedKey(plugin, "map_id");
        meta.getPersistentDataContainer().set(keyId, PersistentDataType.INTEGER, mapId);

        item.setItemMeta(meta);
    }
}