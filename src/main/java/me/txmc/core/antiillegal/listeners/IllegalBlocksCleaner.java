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

    /**
     * @param plugin         main plugin instance
     * @param blockPatterns  wildcard patterns from config
     */
    public IllegalBlocksCleaner(Main plugin, List<String> blockPatterns) {
        this.plugin = plugin;
        this.illegalMaterials = buildMaterialSet(blockPatterns);
        this.batchSize = Math.max(1, plugin.getConfig().getInt("IllegalBlocksCleaner.Batch", 128));
        this.delayTicks = Math.max(1L, plugin.getConfig().getLong("IllegalBlocksCleaner.DelayTicks", 5L));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        int cx = chunk.getX(), cz = chunk.getZ();
        if (Math.abs(cx) >= 29999984 || Math.abs(cz) >= 29999984) return;

        World world = event.getWorld();
        World.Environment env = world.getEnvironment();
        int minY = world.getMinHeight();
        int maxY = (env == World.Environment.NETHER ? 127 : world.getMaxHeight());

        ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);

        List<int[]> toRemove = new ArrayList<>(512);
        int baseX = (cx << 4), baseZ = (cz << 4);

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = minY; y < maxY; y++) {
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
                    toRemove.add(new int[] { wx, y, wz });
                }
            }
        }

        if (toRemove.isEmpty()) return;

        int[] anchor = toRemove.get(0);
        org.bukkit.Location anchorLoc = new org.bukkit.Location(world, anchor[0], anchor[1], anchor[2]);

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
                }
            }
        };
        Bukkit.getRegionScheduler().run(plugin, anchorLoc, (t) -> worker.accept(null));
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
