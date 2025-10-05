package me.txmc.core.patch.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EndPortalBuilder implements Runnable {

    private final JavaPlugin plugin;

    public EndPortalBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World world = Bukkit.getWorlds().get(0);
        if (world == null) return;

        int x = 0, y = world.getMinHeight() + 5, z = 0;

        // Compute all affected chunk coordinates
        Set<ChunkCoord> chunksToLoad = new HashSet<>();

        // Add portal frame blocks
        int[][] offsets = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1}, {2, -1},
                {-2, 0}, {2, 0},
                {-2, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };
        for (int[] offset : offsets) {
            chunksToLoad.add(new ChunkCoord(x + offset[0], z + offset[1]));
        }

        // Add portal center blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunksToLoad.add(new ChunkCoord(x + dx, z + dz));
            }
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ChunkCoord coord : chunksToLoad) {
            CompletableFuture<Void> future = world.getChunkAtAsync(coord.chunkX, coord.chunkZ)
                    .thenAccept(chunk -> {});
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() ->
                Bukkit.getRegionScheduler().run(plugin, world, x, z, task -> {
                    buildEndPortal(world, x, y, z);
                })
        );
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
            frame.setType(Material.END_PORTAL_FRAME, false); // avoid block update issues
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

    // Utility class to de-duplicate chunk coordinates
    private static class ChunkCoord {
        final int chunkX, chunkZ;
        ChunkCoord(int blockX, int blockZ) {
            this.chunkX = blockX >> 4;
            this.chunkZ = blockZ >> 4;
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