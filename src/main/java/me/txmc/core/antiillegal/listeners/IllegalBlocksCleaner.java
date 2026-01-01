/*
 * IllegalBlocksCleaner.java
 */
package me.txmc.core.antiillegal.listeners;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Simplified, performant listener that removes illegal blocks on chunk load.
 */
public class IllegalBlocksCleaner implements Listener {
    private final Main plugin;
    private final EnumSet<Material> illegalMaterials;
    private final int batchSize;
    private final long delayTicks;
    private final NamespacedKey scanKey;
    private final int configHash;
    private final Set<Long> sessionCache = ConcurrentHashMap.newKeySet();
    private final PriorityBlockingQueue<ChunkScanTask> scanQueue = new PriorityBlockingQueue<>();
    private final int immediateScanRadius;
    private final boolean enableSessionCache;
    private final long queueProcessInterval;

    public IllegalBlocksCleaner(Main plugin, List<String> blockPatterns) {
        this.plugin = plugin;
        this.illegalMaterials = buildMaterialSet(blockPatterns);
        this.batchSize = Math.max(1, plugin.getConfig().getInt("IllegalBlocksCleaner.Batch", 128));
        this.delayTicks = Math.max(1L, plugin.getConfig().getLong("IllegalBlocksCleaner.DelayTicks", 5L));
        this.scanKey = new NamespacedKey(plugin, "clean_scan_hash");
        this.configHash = illegalMaterials.hashCode();

        this.immediateScanRadius = plugin.getConfig().getInt("IllegalBlocksCleaner.ImmediateScanRadius", 128);
        this.enableSessionCache = plugin.getConfig().getBoolean("IllegalBlocksCleaner.EnableSessionCache", true);
        this.queueProcessInterval = plugin.getConfig().getLong("IllegalBlocksCleaner.QueueProcessInterval", 4L);

        startQueueProcessor();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk())
            return;

        Chunk chunk = event.getChunk();
        int cx = chunk.getX(), cz = chunk.getZ();

        if (Math.abs(cx) >= 29999984 || Math.abs(cz) >= 29999984)
            return;

        long chunkKey = getChunkKey(cx, cz);

        if (enableSessionCache && sessionCache.contains(chunkKey)) {
            return;
        }

        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(scanKey, PersistentDataType.INTEGER)) {
            Integer storedHash = pdc.get(scanKey, PersistentDataType.INTEGER);
            if (storedHash != null && storedHash == configHash) {
                sessionCache.add(chunkKey);
                return;
            }
        }

        World world = event.getWorld();

        double nearestPlayerDist = getNearestPlayerDistance(world, cx, cz);

        if (nearestPlayerDist <= immediateScanRadius) {
            scanChunk(chunk, world, cx, cz, chunkKey);
        } else {
            int priority = (int) Math.min(nearestPlayerDist, Integer.MAX_VALUE);
            scanQueue.offer(new ChunkScanTask(world, cx, cz, chunkKey, priority));
        }
    }

    /**
     * Starts the background queue processor
     */
    private void startQueueProcessor() {
        plugin.getExecutorService().scheduleAtFixedRate(() -> {
            try {
                ChunkScanTask task = scanQueue.poll();
                if (task == null)
                    return;
                org.bukkit.Location regionLoc = new org.bukkit.Location(
                        task.world,
                        (task.cx << 4) + 8,
                        64,
                        (task.cz << 4) + 8);

                Bukkit.getRegionScheduler().run(plugin, regionLoc, (scheduledTask) -> {
                    if (!task.world.isChunkLoaded(task.cx, task.cz))
                        return;

                    Chunk chunk = task.world.getChunkAt(task.cx, task.cz);
                    scanChunk(chunk, task.world, task.cx, task.cz, task.chunkKey);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing chunk scan queue: " + e.getMessage());
            }
        }, queueProcessInterval, queueProcessInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Performs the actual chunk scan
     */
    private void scanChunk(Chunk chunk, World world, int cx, int cz, long chunkKey) {
        World.Environment env = world.getEnvironment();
        int minY = world.getMinHeight();
        int maxY = (env == World.Environment.NETHER ? 127 : world.getMaxHeight());

        ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);

        plugin.getExecutorService().execute(() -> {
            List<int[]> toRemove = new ArrayList<>();
            int baseX = (cx << 4), baseZ = (cz << 4);

            int minSection = minY >> 4;
            int maxSection = (maxY - 1) >> 4;

            for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                if (snap.isSectionEmpty(sectionY - minSection))
                    continue;

                int startY = Math.max(minY, sectionY << 4);
                int endY = Math.min(maxY - 1, (sectionY << 4) + 15);

                for (int y = startY; y <= endY; y++) {
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            Material type = snap.getBlockType(lx, y, lz);
                            if (!illegalMaterials.contains(type))
                                continue;

                            if (type == Material.BEDROCK) {
                                if (env == World.Environment.NETHER) {
                                    if (y < minY + 5 || y > maxY - 5)
                                        continue;
                                } else if (env == World.Environment.NORMAL) {
                                    if (y < minY + 5)
                                        continue;
                                }
                            }

                            int wx = baseX + lx;
                            int wz = baseZ + lz;
                            toRemove.add(new int[] { wx, y, wz });
                        }
                    }
                }
            }

            int[] anchorFallback = new int[] { cx << 4, minY, cz << 4 };
            int[] anchor = toRemove.isEmpty() ? anchorFallback : toRemove.get(0);
            org.bukkit.Location anchorLoc = new org.bukkit.Location(world, anchor[0], anchor[1], anchor[2]);

            Bukkit.getRegionScheduler().run(plugin, anchorLoc, (t) -> {
                if (toRemove.isEmpty()) {
                    chunk.getPersistentDataContainer().set(scanKey, PersistentDataType.INTEGER, configHash);
                    sessionCache.add(chunkKey);
                    return;
                }

                final int BATCH = batchSize;
                final List<int[]> queue = new ArrayList<>(toRemove);

                java.util.function.Consumer<Object> worker = new java.util.function.Consumer<>() {
                    @Override
                    public void accept(Object ignore) {
                        int processed = 0;
                        while (processed < BATCH && !queue.isEmpty()) {
                            int[] pos = queue.remove(queue.size() - 1);
                            Block b = world.getBlockAt(pos[0], pos[1], pos[2]);
                            if (illegalMaterials.contains(b.getType())) {
                                b.setType(Material.AIR, false);
                            }
                            processed++;
                        }
                        if (!queue.isEmpty()) {
                            Bukkit.getRegionScheduler().runDelayed(plugin, anchorLoc, (t2) -> accept(null), delayTicks);
                        } else {
                            chunk.getPersistentDataContainer().set(scanKey, PersistentDataType.INTEGER, configHash);
                            sessionCache.add(chunkKey);
                        }
                    }
                };
                worker.accept(null);
            });
        });
    }

    /**
     * Gets the distance to the nearest player from chunk center
     */
    private double getNearestPlayerDistance(World world, int cx, int cz) {
        double chunkCenterX = (cx << 4) + 8;
        double chunkCenterZ = (cz << 4) + 8;

        double minDist = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            double dx = player.getLocation().getX() - chunkCenterX;
            double dz = player.getLocation().getZ() - chunkCenterZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    /**
     * Generates a unique key for chunk coordinates
     */
    private long getChunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /**
     * Build an EnumSet of Materials matching wildcard patterns.
     */
    private static EnumSet<Material> buildMaterialSet(List<String> patterns) {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (String pat : patterns) {
            String regex = "^" + pat.replace("*", ".*") + "$";
            Pattern p = Pattern.compile(regex);
            for (Material m : Material.values()) {
                if (p.matcher(m.name()).matches()) {
                    set.add(m);
                }
            }
        }
        return set;
    }

    /**
     * Task representing a pending chunk scan
     */
    private static class ChunkScanTask implements Comparable<ChunkScanTask> {
        final World world;
        final int cx, cz;
        final long chunkKey;
        final int priority;

        ChunkScanTask(World world, int cx, int cz, long chunkKey, int priority) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.chunkKey = chunkKey;
            this.priority = priority;
        }

        @Override
        public int compareTo(ChunkScanTask other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
}
