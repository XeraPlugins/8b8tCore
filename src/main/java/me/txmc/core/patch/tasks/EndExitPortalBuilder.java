package me.txmc.core.patch.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import me.txmc.core.antiillegal.IllegalConstants;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
 */
public class EndExitPortalBuilder implements Runnable {

    private final JavaPlugin plugin;

    public EndExitPortalBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World endWorld = Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                .findFirst().orElse(null);
        if (endWorld == null) return;

        int centerX = IllegalConstants.EXIT_PORTAL_X;
        int centerY = IllegalConstants.EXIT_PORTAL_Y_MIN + 1;
        int centerZ = IllegalConstants.EXIT_PORTAL_Z;

        Set<ChunkCoord> neededChunks = getNeededChunks(centerX, centerZ);

        List<CompletableFuture<Chunk>> loadFutures = new ArrayList<>();
        for (ChunkCoord coord : neededChunks) {
            loadFutures.add(endWorld.getChunkAtAsync(coord.chunkX, coord.chunkZ));
        }

        final World finalWorld = endWorld;
        final int finalX = centerX, finalY = centerY, finalZ = centerZ;

        CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            Bukkit.getRegionScheduler().run(plugin, finalWorld, finalX >> 4, finalZ >> 4, (task) ->
                    buildEndPortal(finalWorld, finalX, finalY, finalZ));
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
        int r = IllegalConstants.EXIT_PORTAL_RADIUS; 
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
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
