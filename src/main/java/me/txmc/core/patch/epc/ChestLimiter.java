package me.txmc.core.patch.epc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class ChestLimiter implements Listener {
    private final int MAX_CHEST_PER_CHUNK;
    private final NamespacedKey chestCountKey;
    private final JavaPlugin plugin;

    public ChestLimiter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.MAX_CHEST_PER_CHUNK = plugin.getConfig().getInt("Patch.ChestLimitPerChunk", 192);
        this.chestCountKey = new NamespacedKey(plugin, "chest_count");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        if (!chunk.getPersistentDataContainer().has(chestCountKey, PersistentDataType.INTEGER)) {
            recalculateChests(chunk);
        }
    }

    private int recalculateChests(Chunk chunk) {
        int count = 0;
        World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        // Scan all blocks without loading tile entities
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Material type = world.getBlockAt(baseX + x, y, baseZ + z).getType();
                    if (isChest(type)) {
                        count++;
                    }
                }
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
        Map<Chunk, Integer> chunkDeltas = new HashMap<>();
        
        for (Block block : blocks) {
            if (isChest(block.getType())) {
                Chunk chunk = block.getChunk();
                chunkDeltas.put(chunk, chunkDeltas.getOrDefault(chunk, 0) - 1);
            }
        }

        for (Map.Entry<Chunk, Integer> entry : chunkDeltas.entrySet()) {
            Chunk chunk = entry.getKey();
            int delta = entry.getValue();
            updateCountScheduled(chunk, delta);
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

    private void updateCountScheduled(Chunk chunk, int delta) {
        Location chunkLoc = chunk.getBlock(0, 64, 0).getLocation();
        
        try {
            Bukkit.getRegionScheduler().run(plugin, chunkLoc, task -> {
                updateCount(chunk, delta);
            });
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            updateCount(chunk, delta);
        }
    }
}