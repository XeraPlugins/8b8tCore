package me.txmc.core.antiillegal.listeners;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
*/

public class IllegalBlocksCleaner implements Listener {
    private final Main plugin;
    private final EnumSet<Material> illegalMaterials;
    private final int batchSize;
    private final long delayTicks;
    private final NamespacedKey scanKey;
    private final int configHash;
    private final boolean enableSessionCache;
    private final Set<Long> sessionCache = ConcurrentHashMap.newKeySet();

    public IllegalBlocksCleaner(Main plugin, ConfigurationSection config) {
        this.plugin = plugin;
        List<String> blockPatterns = config.getStringList("IllegalBlocks");
        this.illegalMaterials = buildMaterialSet(blockPatterns);
        this.batchSize = Math.max(1, config.getInt("IllegalBlocksCleaner.Batch", 128));
        this.delayTicks = Math.max(1L, config.getLong("IllegalBlocksCleaner.DelayTicks", 5L));
        this.scanKey = new NamespacedKey(plugin, "clean_scan_hash");
        this.configHash = illegalMaterials.hashCode();
        this.enableSessionCache = config.getBoolean("IllegalBlocksCleaner.EnableSessionCache", true);
        
        plugin.getLogger().info("[AntiIllegal] Initialized IllegalBlocksCleaner with " + illegalMaterials.size() + " materials.");
        
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    int cx = chunk.getX();
                    int cz = chunk.getZ();
                    org.bukkit.Location chunkCenter = new org.bukkit.Location(world, (cx << 4) + 8, 64, (cz << 4) + 8);
                    Bukkit.getRegionScheduler().run(plugin, chunkCenter, (st) -> {
                            scanChunk(world.getChunkAt(cx, cz), world, cx, cz, getChunkKey(cx, cz));
                    });
                }
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        scanChunk(chunk, event.getWorld(), chunk.getX(), chunk.getZ(), getChunkKey(chunk.getX(), chunk.getZ()));
    }

    private void scanChunk(Chunk chunk, World world, int cx, int cz, long chunkKey) {
        if (Math.abs(cx) >= 1875000 || Math.abs(cz) >= 1875000) return;
                
        /*
        // Cache Disabled here: This approach is not reliable enough due to players constantly unloading chunks.
        // Server Check up task completely ignores the constant unloading of chunks.s
        if (enableSessionCache && sessionCache.contains(chunkKey)) return;

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(scanKey, PersistentDataType.INTEGER)) {
            Integer storedHash = pdc.get(scanKey, PersistentDataType.INTEGER);
            if (storedHash != null && storedHash == configHash) {
                if (enableSessionCache) sessionCache.add(chunkKey);
                return;
            }
        }
        */

        World.Environment env = world.getEnvironment();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);

        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            try {
                List<int[]> toRemove = new ArrayList<>();
                int baseX = (cx << 4), baseZ = (cz << 4);

                int minSection = minY >> 4;
                int maxSection = (maxY - 1) >> 4;

                for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                    if (snap.isSectionEmpty(sectionY - minSection)) continue;

                    int startY = Math.max(minY, sectionY << 4);
                    int endY = Math.min(maxY - 1, (sectionY << 4) + 15);

                    for (int y = startY; y <= endY; y++) {
                        for (int lx = 0; lx < 16; lx++) {
                            for (int lz = 0; lz < 16; lz++) {
                                Material type = snap.getBlockType(lx, y, lz);
                                if (!illegalMaterials.contains(type)) continue;

                                int blockX = baseX + lx;
                                int blockZ = baseZ + lz;

                                if (isLegitimateBlock(env, blockX, y, blockZ, type, minY)) continue;

                                toRemove.add(new int[] { blockX, y, blockZ });
                            }
                        }
                    }
                }

                org.bukkit.Location anchorLoc = new org.bukkit.Location(world, baseX + 8, 64, baseZ + 8);

                Bukkit.getRegionScheduler().run(plugin, anchorLoc, (t) -> {
                    if (!world.isChunkLoaded(cx, cz)) return;

                    if (toRemove.isEmpty()) {
                        return;
                    }

                    processBlockRemoval(world, cx, cz, chunkKey, anchorLoc, toRemove);
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error scanning chunk at " + cx + ", " + cz + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private boolean isLegitimateBlock(World.Environment env, int x, int y, int z, Material type, int minY) {
        if (type == Material.BEDROCK) {
            if (y < minY + 5) return true;
            if (env == World.Environment.NETHER && y >= 123 && y <= 127) return true;
            if (env == World.Environment.THE_END) {
                if (x >= -3 && x <= 3 && z >= -3 && z <= 3 && y >= 59 && y <= 63) return true;

                if (y >= 74 && y <= 76) {
                    double distSq = (long) x * x + (long) z * z;
                    if (distSq >= 90 * 90 && distSq <= 100 * 100) return true;
                }

                if (y >= 2 && y <= 74) {
                    double distSq = (long) x * x + (long) z * z;
                    if (distSq >= 760 * 760) return true;
                }
            }
            return false;
        }

        if (type == Material.END_GATEWAY) {
            if (env == World.Environment.THE_END) {
                if (y == 75) {
                    double distSq = (long) x * x + (long) z * z;
                    if (distSq >= 90 * 90 && distSq <= 100 * 100) return true;
                }
                if (y >= 3 && y <= 73) {
                    double distSq = (long) x * x + (long) z * z;
                    if (distSq >= 760 * 760) return true;
                }
            }
            return false;
        }

        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME) {
            if (env == World.Environment.NORMAL) {
                if (x >= -2 && x <= 2 && z >= -2 && z <= 2 && y == minY + 5) return true;
                double distance = Math.sqrt((long) x * x + (long) z * z);
                return distance >= 1280 && y >= minY && y <= 40;
            }
            if (env == World.Environment.THE_END) {
                return x >= -2 && x <= 2 && z >= -2 && z <= 2 && (y == 60 || y == 61);
            }
        }
        return false;
    }

    private void processBlockRemoval(World world, int cx, int cz, long chunkKey,
                                      org.bukkit.Location anchorLoc, List<int[]> queue) {
        int processed = 0;
        while (processed < batchSize && !queue.isEmpty()) {
            int[] pos = queue.remove(queue.size() - 1);
            Block b = world.getBlockAt(pos[0], pos[1], pos[2]);
            if (illegalMaterials.contains(b.getType())) {
                b.setType(Material.AIR, false);
            }
            processed++;
        }

        if (!queue.isEmpty()) {
            Bukkit.getRegionScheduler().runDelayed(plugin, anchorLoc, (t) -> {
                if (!world.isChunkLoaded(cx, cz)) return;
                processBlockRemoval(world, cx, cz, chunkKey, anchorLoc, queue);
            }, delayTicks);
        }
    }

    private void markChunkAsClean(World world, int cx, int cz, long chunkKey) {
        Chunk c = world.getChunkAt(cx, cz);
        c.getPersistentDataContainer().set(scanKey, PersistentDataType.INTEGER, configHash);
        sessionCache.add(chunkKey);
    }

    private long getChunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static EnumSet<Material> buildMaterialSet(List<String> patterns) {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (String pat : patterns) {
            try {
                String regex = "^" + pat.replace("*", ".*") + "$";
                Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                for (Material m : Material.values()) {
                    if (p.matcher(m.name()).matches()) {
                        set.add(m);
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AntiIllegal] Invalid pattern in IllegalBlocks: " + pat);
            }
        }
        return set;
    }
}
