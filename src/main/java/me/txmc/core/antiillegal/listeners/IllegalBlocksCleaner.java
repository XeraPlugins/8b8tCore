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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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

    /**
     * @param plugin         main plugin instance
     * @param blockPatterns  wildcard patterns from config
     */
    public IllegalBlocksCleaner(Main plugin, List<String> blockPatterns) {
        this.plugin = plugin;
        this.illegalMaterials = buildMaterialSet(blockPatterns);
        this.batchSize = Math.max(1, plugin.getConfig().getInt("IllegalBlocksCleaner.Batch", 128));
        this.delayTicks = Math.max(1L, plugin.getConfig().getLong("IllegalBlocksCleaner.DelayTicks", 5L));
        this.scanKey = new NamespacedKey(plugin, "clean_scan_hash");
        this.configHash = illegalMaterials.hashCode();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        
        // Aggressive Optimization: Skip if already scanned and clean
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (pdc.has(scanKey, PersistentDataType.INTEGER)) {
            Integer storedHash = pdc.get(scanKey, PersistentDataType.INTEGER);
            if (storedHash != null && storedHash == configHash) return;
        }

        int cx = chunk.getX(), cz = chunk.getZ();
        if (Math.abs(cx) >= 29999984 || Math.abs(cz) >= 29999984) return;

        World world = event.getWorld();
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
                // sectionY - minSection is the index for isSectionEmpty (0 handled as bottom)
                if (snap.isSectionEmpty(sectionY - minSection)) continue;

                int startY = Math.max(minY, sectionY << 4);
                int endY = Math.min(maxY - 1, (sectionY << 4) + 15);

                for (int y = startY; y <= endY; y++) {
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            Material type = snap.getBlockType(lx, y, lz);
                            if (!illegalMaterials.contains(type)) continue;

                            if (type == Material.BEDROCK) {
                                if (env == World.Environment.NETHER) {
                                    if (y < minY + 5 || y > maxY - 5) continue;
                                } else if (env == World.Environment.NORMAL) {
                                    if (y < minY + 5) continue;
                                }
                            }

                            int wx = baseX + lx;
                            int wz = baseZ + lz;
                            toRemove.add(new int[]{wx, y, wz});
                        }
                    }
                }
            }

            // Always run on region thread to mark as clean, even if toRemove is empty
            int[] anchorFallback = new int[]{cx << 4, minY, cz << 4};
            int[] anchor = toRemove.isEmpty() ? anchorFallback : toRemove.get(0);
            org.bukkit.Location anchorLoc = new org.bukkit.Location(world, anchor[0], anchor[1], anchor[2]);

            Bukkit.getRegionScheduler().run(plugin, anchorLoc, (t) -> {
                if (toRemove.isEmpty()) {
                    chunk.getPersistentDataContainer().set(scanKey, PersistentDataType.INTEGER, configHash);
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
                        }
                    }
                };
                worker.accept(null);
            });
        });
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
}
