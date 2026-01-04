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
import me.txmc.core.Reloadable;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class ChestLimiter implements Listener, Reloadable {
    private volatile int maxChestPerChunk;
    private final NamespacedKey chestCountKey;
    private final JavaPlugin plugin;

    public ChestLimiter(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        this.chestCountKey = new NamespacedKey(plugin, "chest_count");
    }

    @Override
    public void reloadConfig() {
        this.maxChestPerChunk = plugin.getConfig().getInt("Patch.ChestLimitPerChunk", 192);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        if (!chunk.getPersistentDataContainer().has(chestCountKey, PersistentDataType.INTEGER)) {
            recalculateChestsAsync(chunk);
        }
    }

    private void recalculateChestsAsync(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        Location chunkLoc = new Location(world, chunkX << 4, 64, chunkZ << 4);

        Bukkit.getRegionScheduler().run(plugin, chunkLoc, task -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) return;

            Chunk currentChunk = world.getChunkAt(chunkX, chunkZ);
            int count = 0;
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;

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

            currentChunk.getPersistentDataContainer().set(chestCountKey, PersistentDataType.INTEGER, count);
        });
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

        if (currentCount >= maxChestPerChunk) {
            event.setCancelled(true);
            sendPrefixedLocalizedMessage(event.getPlayer(), "chest_limit_reached");
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
        Map<Long, List<Block>> byChunk = new HashMap<>();

        for (Block block : blocks) {
            if (isChest(block.getType())) {
                long key = getChunkKey(block.getChunk());
                byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(block);
            }
        }

        for (Map.Entry<Long, List<Block>> entry : byChunk.entrySet()) {
            List<Block> chestBlocks = entry.getValue();
            if (chestBlocks.isEmpty()) continue;

            Block first = chestBlocks.get(0);
            int delta = -chestBlocks.size();
            Location loc = first.getLocation();

            Bukkit.getRegionScheduler().run(plugin, loc, task -> {
                if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    updateCount(loc.getChunk(), delta);
                }
            });
        }
    }

    private long getChunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
    }

    private void updateCount(Chunk chunk, int delta) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        int current = pdc.getOrDefault(chestCountKey, PersistentDataType.INTEGER, -1);
        if (current == -1) {
            recalculateChestsAsync(chunk);
        } else {
            pdc.set(chestCountKey, PersistentDataType.INTEGER, Math.max(0, current + delta));
        }
    }
}