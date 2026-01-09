package me.txmc.core.patch.listeners;

import me.txmc.core.util.GlobalUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static org.apache.logging.log4j.LogManager.getLogger;

public class NbtBanPatch implements Listener {
    private final int MAX_ITEM_SIZE_BYTES;
    private final JavaPlugin plugin;

    public NbtBanPatch(JavaPlugin plugin) {
        this.plugin = plugin;
        this.MAX_ITEM_SIZE_BYTES = plugin.getConfig().getInt("NbtBanItemChecker.maxItemSizeAllowed", 48000);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        sanitizeInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sanitizeInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityAdd(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        
        if (entity instanceof Item item) {
            ItemStack stack = item.getItemStack();
            if (isIllegalItem(stack)) {
                item.setItemStack(new ItemStack(Material.AIR));
                item.setItemStack(new ItemStack(Material.AIR));
                entity.getScheduler().run(me.txmc.core.Main.getInstance(), (task) -> entity.remove(), null);
            }
            return;
        } 
        
        if (entity instanceof ItemFrame itemFrame) {
            ItemStack stack = itemFrame.getItem();
            if (isIllegalItem(stack)) {
                itemFrame.setItem(new ItemStack(Material.AIR));
                itemFrame.setItem(new ItemStack(Material.AIR));
                entity.getScheduler().run(me.txmc.core.Main.getInstance(), (task) -> entity.remove(), null);
            }
            return;
        }

        if (entity.customName() != null) {
            net.kyori.adventure.text.Component name = entity.customName();
            if (GlobalUtils.getComponentDepth(name) > 20 || GlobalUtils.getStringContent(name).length() > 500) {
                entity.customName(null); 
                entity.customName(null); 
                entity.getScheduler().run(me.txmc.core.Main.getInstance(), (task) -> entity.remove(), null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isIllegalItem(item)) {
            event.setCancelled(true);
            event.getPlayer().getInventory().setItemInMainHand(null);
            event.getPlayer().getInventory().setItemInMainHand(null); 
            sendPrefixedLocalizedMessage(event.getPlayer(), "nbtPatch_deleted_item", getItemName(item));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
            if (state instanceof org.bukkit.block.CreatureSpawner spawner) {
                if (spawner.getSpawnCount() > 100 || spawner.getRequiredPlayerRange() > 100) {
                     spawner.getBlock().setType(Material.AIR);
                }
            } else if (state instanceof org.bukkit.block.Beehive beehive) {
                if (beehive.getEntityCount() > 5) {
                    state.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof FallingBlock || entity.getType() == org.bukkit.entity.EntityType.FALLING_BLOCK) {
            event.setCancelled(true);
            if (event.getSpawner() != null) {
                event.getSpawner().getBlock().setType(Material.AIR);
            }
        } else if (entity instanceof Item || entity instanceof ItemFrame) {
             event.setCancelled(true);
             if (event.getSpawner() != null) {
                event.getSpawner().getBlock().setType(Material.AIR); 
            }
        }
    }

    private boolean isIllegalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        try {
            if (isBundle(item.getType())) {
                if (checkBundleRecursion(item, 0)) {
                    return true;
                }
            }
            
            if (item.hasItemMeta()) {
                 int size = calculateItemSize(item);
                 if (size > MAX_ITEM_SIZE_BYTES) {
                     return true;
                 }
            }
        } catch (Exception e) {
            return false;
        }
        
        return false;
    }

    private void sanitizeInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean modified = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item == null || item.getType() == Material.AIR) continue;

            if (!item.hasItemMeta()) continue;

            int itemSize = calculateItemSize(item);

            if (itemSize > MAX_ITEM_SIZE_BYTES) {
                String itemName = getItemName(item);
                getLogger().warn("NBT Patch: Prevented overloaded NBT item from {} | Size: {} bytes | Type: {}", 
                    player.getName(), itemSize, itemName);

                player.getInventory().setItem(i, null);
                
                sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", itemName);
                modified = true;
                continue;
            }

            if (isBundle(item.getType())) {
                if (checkBundleRecursion(item, 0)) {
                    String itemName = getItemName(item);
                    getLogger().warn("NBT Patch: Prevented bundle crash from {} | Type: {}", player.getName(), itemName);
                    player.getInventory().setItem(i, null);
                    sendPrefixedLocalizedMessage(player, "nbtPatch_deleted_item", itemName);
                    modified = true;
                }
            }
        }
        
        if (modified) player.updateInventory();
    }

    // PaperMC decided to deprecate this method apparently is discouraged to use.
    // This is the only performant way to check recursive NBT size without using NMS/OBC.
    // If we use YAML serialization, it will be very slow. Very, very slow.
    private int calculateItemSize(ItemStack item) {
        return GlobalUtils.calculateItemSize(item);
    }

    public static String getItemName(ItemStack itemStack) {
        if (itemStack == null) return "Unknown";
        try {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return GlobalUtils.getStringContent(meta.displayName());
            }
        } catch (Exception ignored) {}
        return itemStack.getType().name();
    }
    
    private boolean checkBundleRecursion(ItemStack item, int depth) {
        if (item == null || !isBundle(item.getType())) return false;
        if (depth >= 1) return true;
        try {
            if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                for (ItemStack inner : bundleMeta.getItems()) {
                    if (inner == null) continue;
                    if (isBundle(inner.getType())) {
                        return true; 
                    }
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    private boolean isBundle(Material type) {
        return type.name().endsWith("BUNDLE") || type.name().contains("SHULKER");
    }
}