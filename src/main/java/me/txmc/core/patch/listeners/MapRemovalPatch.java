package me.txmc.core.patch.listeners;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class MapRemovalPatch implements Listener {

    private final Set<Integer> RESTRICTED_MAP_IDS = new HashSet<>();

    public MapRemovalPatch(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        List<Integer> restrictedIds = config.getIntegerList("RestrictedMapIDs");
        RESTRICTED_MAP_IDS.addAll(restrictedIds);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        removeRestrictedMaps(player.getInventory(), player);
        removeRestrictedMaps(player.getEnderChest(), player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        removeRestrictedMaps(event.getInventory(), player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        removeRestrictedMaps(event.getInventory(), player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        removeRestrictedMaps(event.getInitiator(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryEvent(InventoryEvent event) {
        removeRestrictedMaps(event.getInventory(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        if (isRestrictedMap(item)) {
            event.setCancelled(true);
            event.getItem().remove();
            sendPrefixedLocalizedMessage(player, "mapart_deleted_inventory");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame itemFrame) {
                ItemStack item = itemFrame.getItem();
                if (isRestrictedMap(item)) {
                    itemFrame.setItem(null);

                    for (Player player : chunk.getWorld().getPlayers()) {
                        if (player.getLocation().getChunk().equals(chunk)) {
                            sendPrefixedLocalizedMessage(player, "mapart_deleted_frame");
                        }
                    }
                }
            }
        }
    }

    private void removeRestrictedMaps(Inventory inventory, @Nullable Player player) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isRestrictedMap(item)) {
                inventory.setItem(i, null);

                if (player != null) {
                    sendPrefixedLocalizedMessage(player, "mapart_deleted_inventory");
                }
            }
        }
    }

    private boolean isRestrictedMap(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        if (item.hasItemMeta() && item.getItemMeta() instanceof MapMeta meta) {
            return RESTRICTED_MAP_IDS.contains(meta.getMapId());
        }
        return false;
    }
}
