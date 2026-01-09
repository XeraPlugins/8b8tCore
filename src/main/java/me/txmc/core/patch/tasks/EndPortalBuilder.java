package me.txmc.core.patch.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
 */
public class EndPortalBuilder implements Runnable {

    private final JavaPlugin plugin;

    public EndPortalBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World world = Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst().orElse(null);
        if (world == null) return;

        int x = 0, y = world.getMinHeight() + 5, z = 0;

        Set<ChunkCoord> chunksToLoad = new HashSet<>();

        int[][] offsets = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1}, {2, -1},
                {-2, 0}, {2, 0},
                {-2, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : offsets) {
            int blockX = x + offset[0];
            int blockZ = z + offset[1];
            chunksToLoad.add(new ChunkCoord(blockX >> 4, blockZ >> 4));
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunksToLoad.add(new ChunkCoord((x + dx) >> 4, (z + dz) >> 4));
            }
        }

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (ChunkCoord coord : chunksToLoad) {
            futures.add(world.getChunkAtAsync(coord.chunkX, coord.chunkZ));
        }

        final World finalWorld = world;
        final int finalX = x, finalY = y, finalZ = z;

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                Bukkit.getRegionScheduler().run(plugin, finalWorld, finalX >> 4, finalZ >> 4, 
                    task -> buildEndPortal(finalWorld, finalX, finalY, finalZ));
            });
    }

    private void buildEndPortal(World world, int x, int y, int z) {
        int[][] offsets = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1}, {2, -1},
                {-2, 0}, {2, 0},
                {-2, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : offsets) {
            Block frame = world.getBlockAt(x + offset[0], y, z + offset[1]);
            frame.setType(Material.END_PORTAL_FRAME, false);
            EndPortalFrame data = (EndPortalFrame) frame.getBlockData();
            data.setEye(true);
            frame.setBlockData(data, false);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block portalBlock = world.getBlockAt(x + dx, y, z + dz);
                portalBlock.setType(Material.END_PORTAL, false);
            }
        }
    }

    private static class ChunkCoord {
        final int chunkX, chunkZ;
        ChunkCoord(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof ChunkCoord other)) return false;
            return chunkX == other.chunkX && chunkZ == other.chunkZ;
        }

        @Override public int hashCode() {
            return Objects.hash(chunkX, chunkZ);
        }
    }
}