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

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            map.setTrackingPosition(true);
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
        
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            String names = getNearbyPlayerNames(player);
            signUnsignedMaps(player, names);
        }, null, 2L);

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            String names = getNearbyPlayerNames(player);
            signUnsignedMaps(player, names);
        }, null, 10L);
    }

    private String getNearbyPlayerNames(Player player) {
        List<String> names = new ArrayList<>();
        names.add(player.getName());
        
        player.getNearbyEntities(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS).stream()
                .filter(e -> e instanceof Player && !e.getUniqueId().equals(player.getUniqueId()))
                .map(e -> ((Player) e).getName())
                .forEach(names::add);

        return String.join(", @", names);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ItemFrame frame) {
                ItemStack frameItem = frame.getItem();
                if (frameItem != null && frameItem.getType() == Material.FILLED_MAP) {
                    sanitizeFrameMap(frame, frameItem);
                }
            }
        }
    }

    private void sanitizeFrameMap(ItemFrame frame, ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof MapMeta mm)) return;
        if (!mm.hasMapId()) return;
        
        MapView view = Bukkit.getMap(mm.getMapId());
        if (view != null) {
            view.setTrackingPosition(true);
            view.setUnlimitedTracking(false);
        }
    }

    private void signUnsignedMaps(Player player, String signatureText) {
        checkAndSign(player.getInventory().getItemInMainHand(), signatureText);
        checkAndSign(player.getInventory().getItemInOffHand(), signatureText);
        checkAndSign(player.getOpenInventory().getCursor(), signatureText);

        for (ItemStack item : player.getInventory().getContents()) {
            checkAndSign(item, signatureText);
        }
    }

    private void checkAndSign(ItemStack item, String signatureText) {
        if (item == null || item.getType() != Material.FILLED_MAP) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey authorKey = new NamespacedKey(plugin, "map_author");
        if (!meta.getPersistentDataContainer().has(authorKey, PersistentDataType.STRING)) {
            int mapId = -1;
            if (meta instanceof MapMeta mm && mm.hasMapId()) mapId = mm.getMapId();
            applySignature(item, meta, signatureText, mapId);
        }
    }

    private void applySignature(ItemStack item, ItemMeta meta, String players, int mapId) {
        Component authorComp = Component.text("by @" + players, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a, MMMM d, yyyy");
        String utcTime = ZonedDateTime.now(ZoneId.of("UTC")).format(formatter);
        Component timeComp = Component.text("Created: " + utcTime + " UTC", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false);

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        
        boolean alreadySigned = false;
        if (!lore.isEmpty()) {
            alreadySigned = lore.stream()
                .map(GlobalUtils::getStringContent)
                .anyMatch(line -> line.contains("by @"));
        }

        if (!alreadySigned) {
            lore.add(0, authorComp);
            lore.add(1, timeComp);
        }
        
        meta.lore(lore);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "map_author"), PersistentDataType.STRING, players);
        meta.getPersistentDataContainer().set(
             new NamespacedKey(plugin, "map_created"), PersistentDataType.STRING, utcTime);
        
        if (mapId != -1) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "map_id"), PersistentDataType.INTEGER, mapId);
        }
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "map_metadata"), PersistentDataType.STRING, "Authenticated by 8b8tCore Signature System");

        item.setItemMeta(meta);
    }
}