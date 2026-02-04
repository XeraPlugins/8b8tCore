package me.txmc.core.patch.listeners;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.txmc.core.Main;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    private static final String KEY_AUTHOR = "map_author";

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView map = event.getMap();
        if (map != null) {
            map.setTrackingPosition(true);
            map.setUnlimitedTracking(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onMapCreateAttempt(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        
        boolean hasMapInMain = player.getInventory().getItemInMainHand().getType() == Material.MAP;
        boolean hasMapInOff = player.getInventory().getItemInOffHand().getType() == Material.MAP;

        if (!hasMapInMain && !hasMapInOff) return;

        String signatureText = getNearbyPlayerNames(player);
        String utcTime = getCurrentUtcTime();

        scheduleRetryScan(player, signatureText, utcTime, 0);
    }

    private void scheduleRetryScan(Player player, String signatureText, String utcTime, int attempt) {
        if (attempt > 5) return;

        Consumer<ScheduledTask> scanTask = (task) -> {
            if (!player.isOnline()) return;
            
            boolean signedSomething = scanInventoryAndSign(player, signatureText, utcTime);
            
            if (!signedSomething) {
                long nextDelay = (attempt == 0) ? 5L : 10L;
                scheduleRetryScan(player, signatureText, utcTime, attempt + 1);
            }
        };

        long initialDelay = (attempt == 0) ? 1L : 0L; 
        
        if (initialDelay > 0) {
            player.getScheduler().runDelayed(plugin, scanTask, null, initialDelay);
        } else {
             player.getScheduler().runDelayed(plugin, scanTask, null, 10L);
        }
    }

    private boolean scanInventoryAndSign(Player player, String signatureText, String timeString) {
        boolean success = false;

        if (attemptSign(player.getItemOnCursor(), signatureText, timeString)) success = true;

        if (attemptSign(player.getInventory().getItemInOffHand(), signatureText, timeString)) success = true;

        for (ItemStack is : player.getInventory().getStorageContents()) {
            if (attemptSign(is, signatureText, timeString)) {
                success = true;
            }
        }
        return success;
    }

    private boolean attemptSign(ItemStack item, String signatureText, String timeString) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey authorKey = new NamespacedKey(plugin, KEY_AUTHOR);
        
        if (!meta.getPersistentDataContainer().has(authorKey, PersistentDataType.STRING)) {
            
            int mapId = -1;
            if (meta instanceof MapMeta mm && mm.hasMapId()) {
                mapId = mm.getMapId();
                
                MapView view = Bukkit.getMap(mapId);
                if (view != null) {
                    view.setTrackingPosition(true);
                    view.setUnlimitedTracking(false);
                }
            }
            
            applySignature(item, meta, signatureText, timeString, mapId);
            return true;
        }
        return false;
    }

    private String getNearbyPlayerNames(Player player) {
        List<String> names = new ArrayList<>();
        names.add(player.getName());
        try {
            player.getNearbyEntities(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS).stream()
                    .filter(e -> e instanceof Player && !e.getUniqueId().equals(player.getUniqueId()))
                    .map(e -> ((Player) e).getName())
                    .forEach(names::add);
        } catch (Exception ignored) {
            // Ignore any errors during nearby entity scanning
        }

        return String.join(", @", names);
    }
    
    private String getCurrentUtcTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a, MMMM d, yyyy");
        return ZonedDateTime.now(ZoneId.of("UTC")).format(formatter);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame frame) {
                ItemStack frameItem = frame.getItem();
                if (frameItem.getType() == Material.FILLED_MAP) {
                    sanitizeFrameMap(frameItem);
                }
            }
        }
    }

    private void sanitizeFrameMap(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta mm)) return;
        if (!mm.hasMapId()) return;

        MapView view = Bukkit.getMap(mm.getMapId());
        if (view != null && view.isUnlimitedTracking()) {
            view.setTrackingPosition(true);
            view.setUnlimitedTracking(false);
        }
    }

    private void applySignature(ItemStack item, ItemMeta meta, String players, String timeString, int mapId) {
        Component authorComp = Component.text("by @" + players, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        Component timeComp = Component.text("Created: " + timeString + " UTC", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        
        boolean visuallySigned = !lore.isEmpty() && lore.get(0).toString().contains("by @");

        if (!visuallySigned) {
            lore.add(0, authorComp);
            lore.add(1, timeComp);
        }

        meta.lore(lore);
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, KEY_AUTHOR), PersistentDataType.STRING, players);
        meta.getPersistentDataContainer().set(
             new NamespacedKey(plugin, "map_created"), PersistentDataType.STRING, timeString);

        if (mapId != -1) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "map_id"), PersistentDataType.INTEGER, mapId);
        }

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "map_metadata"), PersistentDataType.STRING, "Authenticated by 8b8tCore Signature System");

        item.setItemMeta(meta);
    }
}