package me.txmc.core.patch.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.txmc.core.Main;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
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
        MapView map = event.getMap();
        if (map != null) {
            map.setTrackingPosition(false);
            map.setUnlimitedTracking(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMapCreateAttempt(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MAP) return;

        Player player = event.getPlayer();
        
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;
            
            String playerNames = player.getWorld().getNearbyPlayers(player.getLocation(), SCAN_RADIUS)
                    .stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", @"));

            ItemStack main = player.getInventory().getItemInMainHand();
            if (main.getType() == Material.FILLED_MAP) {
                if (main.getItemMeta() instanceof MapMeta mm && mm.hasMapId()) {
                    signPlayerMap(player, mm.getMapId(), playerNames);
                }
            }

            ItemStack off = player.getInventory().getItemInOffHand();
            if (off.getType() == Material.FILLED_MAP) {
                if (off.getItemMeta() instanceof MapMeta mm && mm.hasMapId()) {
                    signPlayerMap(player, mm.getMapId(), playerNames);
                }
            }
        }, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame frame) {
                ItemStack frameItem = frame.getItem();
                if (frameItem.getType() == Material.FILLED_MAP) {
                    sanitizeFrameMap(frame, frameItem);
                }
            }
        }
    }

    private void sanitizeFrameMap(ItemFrame frame, ItemStack item) {
        if (!(item.getItemMeta() instanceof MapMeta mm)) return;
        if (!mm.hasMapId()) return;
        
        MapView view = Bukkit.getMap(mm.getMapId());
        if (view != null) {
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
        }
    }

    private void signPlayerMap(Player player, int mapId, String signatureText) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (signIfMatches(mainHand, mapId, signatureText)) return;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (signIfMatches(offHand, mapId, signatureText)) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && signIfMatches(item, mapId, signatureText)) {
                break;
            }
        }
    }

    private boolean signIfMatches(ItemStack item, int mapId, String signatureText) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof MapMeta mapMeta)) return false;
        if (!mapMeta.hasMapId() || mapMeta.getMapId() != mapId) return false;

        NamespacedKey key = new NamespacedKey(plugin, "map_id");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return true;
        }

        applySignature(item, meta, signatureText, mapId);
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
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "map_author"), PersistentDataType.STRING, players);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "map_id"), PersistentDataType.INTEGER, mapId);

        item.setItemMeta(meta);
    }
}