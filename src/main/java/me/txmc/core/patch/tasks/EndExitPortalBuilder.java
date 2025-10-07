package me.txmc.core.patch.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EndExitPortalBuilder implements Runnable {

    private final JavaPlugin plugin;

    public EndExitPortalBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World endWorld = Bukkit.getWorlds().get(2);
        if (endWorld == null) return;

        int centerX = 0, centerY = 59, centerZ = 0;

        Set<ChunkCoord> neededChunks = getNeededChunks(centerX, centerZ);

        // Load all required chunks asynchronously
        List<CompletableFuture<Chunk>> loadFutures = new ArrayList<>();
        for (ChunkCoord coord : neededChunks) {
            loadFutures.add(endWorld.getChunkAtAsync(coord.chunkX, coord.chunkZ));
        }

        CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            Bukkit.getRegionScheduler().run(plugin, endWorld, centerX, centerZ, (task) ->
                    buildEndPortal(endWorld, centerX, centerY, centerZ));
        });
    }

    private void buildEndPortal(World world, int x, int y, int z) {
        int[][] bedrockLayer1 = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
                {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0},
                {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : bedrockLayer1) {
            world.getBlockAt(x + offset[0], y, z + offset[1]).setType(Material.BEDROCK);
        }

        int[][] bedrockLayer2 = {
                {-1, -3}, {0, -3}, {1, -3},
                {-2, -2}, {2, -2},
                {-3, -1}, {3, -1},
                {-3, 0}, {3, 0},
                {-3, 1}, {3, 1},
                {-2, 2}, {2, 2},
                {-1, 3}, {0, 3}, {1, 3}
        };

        for (int[] offset : bedrockLayer2) {
            world.getBlockAt(x + offset[0], y + 1, z + offset[1]).setType(Material.BEDROCK);
        }

        int[][] portalBlocks = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
                {-2, 0}, {-1, 0}, {1, 0}, {2, 0},
                {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : portalBlocks) {
            world.getBlockAt(x + offset[0], y + 1, z + offset[1]).setType(Material.END_PORTAL);
        }

        for (int dy = 1; dy <= 4; dy++) {
            world.getBlockAt(x, y + dy, z).setType(Material.BEDROCK);
        }
    }

    private Set<ChunkCoord> getNeededChunks(int centerX, int centerZ) {
        Set<ChunkCoord> chunks = new HashSet<>();

        // Covers the entire structure bounds, -3 to +3 range
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int blockX = centerX + dx;
                int blockZ = centerZ + dz;
                chunks.add(new ChunkCoord(blockX >> 4, blockZ >> 4));
            }
        }

        return chunks;
    }

    private static class ChunkCoord {
        final int chunkX, chunkZ;

        ChunkCoord(int x, int z) {
            this.chunkX = x;
            this.chunkZ = z;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChunkCoord c)) return false;
            return chunkX == c.chunkX && chunkZ == c.chunkZ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkX, chunkZ);
        }
    }
}
