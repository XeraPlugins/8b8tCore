package me.txmc.core.patch.epc;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Limits the number of chests, trapped chests, and barrels that can be placed in a chunk.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft, including chest and barrel placement restrictions.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Preventing the placement of chests, trapped chests, and barrels if the maximum count per chunk is exceeded</li>
 *     <li>Removing excess chests, trapped chests, and barrels from a chunk when it is loaded</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/09 01:28 AM
 */
public class ChestLimiter implements Listener {
    private final int MAX_CHEST_PER_CHUNK;
    private final NamespacedKey chestCountKey;

    public ChestLimiter(JavaPlugin plugin) {
        this.MAX_CHEST_PER_CHUNK = plugin.getConfig().getInt("Patch.ChestLimitPerChunk", 192);
        this.chestCountKey = new NamespacedKey(plugin, "chest_count");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        recalculateChests(chunk);
    }

    private int recalculateChests(Chunk chunk) {
        int count = 0;
        for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
            if (isChest(state.getType())) {
                count++;
            }
        }
        chunk.getPersistentDataContainer().set(chestCountKey, PersistentDataType.INTEGER, count);
        return count;
    }

    private boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!isChest(block.getType())) return;

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int currentCount = pdc.getOrDefault(chestCountKey, PersistentDataType.INTEGER, 0);

        if (currentCount >= MAX_CHEST_PER_CHUNK) {
            event.setCancelled(true);
            return;
        }

        pdc.set(chestCountKey, PersistentDataType.INTEGER, currentCount + 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isChest(block.getType())) {
            updateCount(block.getChunk(), -1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (isChest(block.getType())) {
            updateCount(block.getChunk(), -1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        for (Block block : blocks) {
            if (isChest(block.getType())) {
                updateCount(block.getChunk(), -1);
            }
        }
    }

    private void updateCount(Chunk chunk, int delta) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int current = pdc.getOrDefault(chestCountKey, PersistentDataType.INTEGER, -1);
        if (current == -1) {
            recalculateChests(chunk);
        } else {
            pdc.set(chestCountKey, PersistentDataType.INTEGER, Math.max(0, current + delta));
        }
    }
}