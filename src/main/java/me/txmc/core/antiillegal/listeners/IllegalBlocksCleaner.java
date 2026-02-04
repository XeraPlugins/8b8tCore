package me.txmc.core.antiillegal.listeners;

import me.txmc.core.Main;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.txmc.core.antiillegal.IllegalConstants;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 8b8tCore IllegalBlocksCleaner
 * @author MindComplexity 01/21/2026
 * This file was created as a part of 8b8tAntiIllegal
*/

public class IllegalBlocksCleaner implements Listener {
    private final Main plugin;
    private final EnumSet<Material> illegalMaterials;
    private final int batchSize;
    private final long delayTicks;
    private final NamespacedKey scanKey;
    private final int configHash;

    public IllegalBlocksCleaner(Main plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.illegalMaterials = buildMaterialSet(config.getStringList("IllegalBlocks"));
        this.batchSize = Math.max(1, config.getInt("IllegalBlocksCleaner.Batch", 128));
        this.delayTicks = Math.max(1L, config.getLong("IllegalBlocksCleaner.DelayTicks", 5L));
        this.scanKey = new NamespacedKey(plugin, "clean_scan_hash");

        // Salt the hash with a config version. Incrementing this value triggers a 
        // global invalidation of all existing "Certified Clean" stickers, 
        int version = config.getInt("IllegalBlocksCleaner.Version", 1);
        this.configHash = illegalMaterials.hashCode() ^ (version * 31);

        plugin.getLogger().info("[AntiIllegal] System initialized. Version: " + version);
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int cx = chunk.getX();
                    int cz = chunk.getZ();
                    Location loc = new Location(world, (cx << 4) + 8, 64, (cz << 4) + 8);
                    Bukkit.getRegionScheduler().run(plugin, loc, (t) -> {
                        if (world.isChunkLoaded(cx, cz)) {
                            checkAndScan(world.getChunkAt(cx, cz));
                        }
                    });
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        checkAndScan(event.getChunk());
    }

    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Any block placement by a player breaks the chunk's certification.
        // It must be re-verified the next time it's loaded to catch sneaky bypasses.
        invalidateChunk(event.getBlock().getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        // Explosions change the state of multiple chunks; invalidate all affected regions.
        Set<Chunk> affectedChunks = new HashSet<>();
        for (Block b : event.blockList()) affectedChunks.add(b.getChunk());
        for (Chunk c : affectedChunks) invalidateChunk(c);
    }

    private void invalidateChunk(Chunk chunk) {
        chunk.getPersistentDataContainer().remove(scanKey);
    }

    private void checkAndScan(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        Integer storedHash = pdc.get(scanKey, PersistentDataType.INTEGER);

        // O(1) Header Check: If the PDC tag matches our current security hash, 
        // we trust the chunk and skip the expensive deep scan.
        if (storedHash != null && storedHash == configHash) return;

        performDeepScan(chunk);
    }

    private void performDeepScan(Chunk chunk) {
        World world = chunk.getWorld();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        
        // Return if chunks are outside the world border.
        if (Math.abs(cx) >= 1875000 || Math.abs(cz) >= 1875000) return;

        // SS in memory and perform a scan OThread.
        ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);
        World.Environment env = world.getEnvironment();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            // we use 'int' (50% less memory usage whe running a scan).
            // cmp long whloop for rel chunk offs and Y values. 
            int[] toRemove = new int[256]; 
            int foundCount = 0;

            int minSection = minY >> 4;
            int maxSection = (maxY - 1) >> 4;

            for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                // Skip 4,096 block checks instantly
                if (snap.isSectionEmpty(sectionY - minSection)) continue;

                int startY = Math.max(minY, sectionY << 4);
                int endY = Math.min(maxY - 1, (sectionY << 4) + 15);

                for (int y = startY; y <= endY; y++) {
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            Material type = snap.getBlockType(lx, y, lz);
                            if (!illegalMaterials.contains(type)) continue;

                            // Calculate GCoords only when a suspect block is hit.
                            int gx = (cx << 4) + lx;
                            int gz = (cz << 4) + lz;

                            // Run legitimacy filters to ensure we don't nuke world gen.
                            if (isLegitimateBlock(env, gx, y, gz, type, minY)) continue;

                            if (foundCount >= toRemove.length) toRemove = Arrays.copyOf(toRemove, toRemove.length * 2);
                            toRemove[foundCount++] = pack(lx, y, lz);
                        }
                    }
                }
            }
            // No issues.
            if (foundCount == 0) {
                Location anchor = new Location(world, (cx << 4) + 8, 64, (cz << 4) + 8);
                Bukkit.getRegionScheduler().run(plugin, anchor, (t) -> markChunkClean(world, cx, cz));
                return;
            }

            plugin.getLogger().info("[AntiIllegal] Detected " + foundCount + " illegal blocks in chunk [" + cx + ", " + cz + "]. Liquidating...");

            // Move to the Regional Thread to actually modify blocks
            final int[] finalQueue = toRemove;
            final int finalTotal = foundCount;

            // convert location and anchor into the middle of the chunk.
            // each chunk is 16 blocks, 8 blocks is in the center so cx * 16 + 8 (center offset)
            // << 4 is equalivent to * 16. 
            // We need to multiply by 16 to get from the chunk index to the start blocking of the chunk.
            Location anchor = new Location(world, (cx << 4) + 8, 64, (cz << 4) + 8);
            
            Bukkit.getRegionScheduler().run(plugin, anchor, (t) -> {
                processRemovalBatch(world, cx, cz, finalQueue, finalTotal, 0, anchor);
            });
        });
    }

    private void processRemovalBatch(World world, int cx, int cz, int[] queue, int total, int index, Location anchor) {
        if (!world.isChunkLoaded(cx, cz)) return;

        int processedInThisTick = 0;
        int baseX = cx << 4;
        int baseZ = cz << 4;

        // Process removals in governed batches to prevent our server from literally dieing.
        while (processedInThisTick < batchSize && index < total) {
            int packed = queue[index];
            int x = baseX + unpackLX(packed);
            int z = baseZ + unpackLZ(packed);
            int y = unpackY(packed);
            
            Block b = world.getBlockAt(x, y, z);
            if (illegalMaterials.contains(b.getType())) {
                b.setType(Material.AIR, false);
            }
            index++;
            processedInThisTick++;
        }

        if (index < total) {
            final int nextIndex = index;
            Bukkit.getRegionScheduler().runDelayed(plugin, anchor, 
                (t) -> processRemovalBatch(world, cx, cz, queue, total, nextIndex, anchor), delayTicks);
        } else {
            markChunkClean(world, cx, cz);
        }
    }

    private void markChunkClean(World world, int cx, int cz) {
        Chunk c = world.getChunkAt(cx, cz);
        c.getPersistentDataContainer().set(scanKey, PersistentDataType.INTEGER, configHash);
    }

    
    // Pack relative local coords and Y into a single 32-bit slot for cache efficiency.
    private int pack(int lx, int y, int lz) {
        return (y << 8) | (lx << 4) | lz;
    }
    private int unpackLX(int p) { return (p >> 4) & 0xF; }
    private int unpackLZ(int p) { return p & 0xF; }
    private int unpackY(int p) { return p >> 8; }


    private boolean isLegitimateBlock(World.Environment env, int x, int y, int z, Material type, int minY) {
        if (type == Material.BEDROCK || type == Material.END_GATEWAY) {
            if (type == Material.BEDROCK) {
                // Vanilla floor
                if (y < minY + 5) return true; 
                // Nether ceiling
                if (env == World.Environment.NETHER && y >= 123 && y <= 127) return true;
            }

            if (env == World.Environment.THE_END) {
                // Central Exit Portal
                if (Math.abs(x) <= IllegalConstants.EXIT_PORTAL_RADIUS && 
                    Math.abs(z) <= IllegalConstants.EXIT_PORTAL_RADIUS && 
                    y >= IllegalConstants.EXIT_PORTAL_Y_MIN && 
                    y <= IllegalConstants.EXIT_PORTAL_Y_MAX) return true;

                // Gateway Rings
                if (y >= IllegalConstants.GATEWAY_RING_Y_MIN && y <= IllegalConstants.GATEWAY_RING_Y_MAX) {
                    long distSq = (long) x * x + (long) z * z;
                    return distSq >= IllegalConstants.GATEWAY_RING_INNER_RADIUS_SQ && 
                           distSq <= IllegalConstants.GATEWAY_RING_OUTER_RADIUS_SQ;
                }

                // Outer Islands
                if (y >= 0 && y <= IllegalConstants.OUTER_ISLAND_Y_MAX) { 
                    long distSq = (long) x * x + (long) z * z;
                    // central void gap where no bedrock/gateways allowed
                    if (Math.abs(x) < 700 && Math.abs(z) < 700) return false;
                    if (distSq >= IllegalConstants.OUTER_ISLAND_VOID_GAP_SQ) return true;
                }
            }
            return false;
        }

        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME) {
            if (env == World.Environment.NORMAL) {
                // Strongholds
                if (Math.abs(x) <= 2 && Math.abs(z) <= 2 && y == minY + 5) return true;
                if (y < minY || y > 40) return false;
                if (Math.abs(x) < 1280 && Math.abs(z) < 1280) return false;
                double distanceSq = (long) x * x + (long) z * z;
                return distanceSq >= IllegalConstants.STRONGHOLD_SAFE_ZONE_SQ;
            }
            if (env == World.Environment.THE_END) {
                return Math.abs(x) <= 2 && Math.abs(z) <= 2 && (y == 60 || y == 61);
            }
        }
        return false;
    }

    private static EnumSet<Material> buildMaterialSet(List<String> patterns) {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (String pat : patterns) {
            try {
                Pattern p = Pattern.compile("^" + pat.replace("*", ".*") + "$", Pattern.CASE_INSENSITIVE);
                for (Material m : Material.values()) {
                    if (p.matcher(m.name()).matches()) set.add(m);
                }
            } catch (Exception ignored) {}
        }
        return set;
    }
}
