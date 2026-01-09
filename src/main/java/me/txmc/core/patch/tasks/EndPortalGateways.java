package me.txmc.core.patch.tasks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * This file is apart of 8b8tcore.
 * Combined implementation for End Exit and Return Gateways.
 * @author MindComplexity
 * @since 01/02/2026
 */
public class EndPortalGateways {

    public static class EndExitGatewayBuilder implements Runnable {
        private final JavaPlugin plugin;
        private static final int GATEWAY_Y = 75;
        private static final int[][] EXIT_GATEWAY_POSITIONS = {
                {96, 0}, {91, 30}, {77, 57}, {57, 77}, {30, 91},
                {0, 96}, {-30, 91}, {-57, 77}, {-77, 57}, {-91, 30},
                {-96, 0}, {-91, -30}, {-77, -57}, {-57, -77}, {-30, -91},
                {0, -96}, {30, -91}, {57, -77}, {77, -57}, {91, -30}
        };

        public EndExitGatewayBuilder(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            World endWorld = Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                    .findFirst().orElse(null);
            if (endWorld == null) return;

            for (int[] pos : EXIT_GATEWAY_POSITIONS) {
                buildGatewayAsync(endWorld, pos[0], GATEWAY_Y, pos[1]);
            }
        }

        private void buildGatewayAsync(World world, int x, int y, int z) {
            Set<ChunkCoord> neededChunks = getNeededChunks(x, z);
            List<CompletableFuture<Chunk>> loadFutures = new ArrayList<>();
            for (ChunkCoord coord : neededChunks) {
                loadFutures.add(world.getChunkAtAsync(coord.chunkX, coord.chunkZ));
            }

            CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> Bukkit.getRegionScheduler().run(plugin, world,
                            x >> 4, z >> 4,
                            task -> buildSingleGateway(world, x, y, z)));
        }

        private void buildSingleGateway(World world, int x, int y, int z) {
            int[][] bedrockOffsets = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
            for (int[] offset : bedrockOffsets) {
                Block block = world.getBlockAt(x + offset[0], y + offset[1], z + offset[2]);
                if (block.getType() != Material.BEDROCK) block.setType(Material.BEDROCK, false);
            }
            Block gatewayBlock = world.getBlockAt(x, y, z);
            gatewayBlock.setType(Material.END_GATEWAY, false);
            if (gatewayBlock.getState() instanceof org.bukkit.block.EndGateway gateway) {
                gateway.setExitLocation(new Location(world, 0, 64, 0));
                gateway.setExactTeleport(false);
                gateway.update(true, false);
            }
        }

        private Set<ChunkCoord> getNeededChunks(int centerX, int centerZ) {
            Set<ChunkCoord> chunks = new HashSet<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    chunks.add(new ChunkCoord((centerX + dx) >> 4, (centerZ + dz) >> 4));
                }
            }
            return chunks;
        }
    }

    public static class EndReturnGatewayBuilder implements Runnable {
        private final JavaPlugin plugin;
        private final int gatewayX, gatewayY, gatewayZ;
        private static final int MIN_GATEWAY_Y = 3, MAX_GATEWAY_Y = 73;
        private static final int MIN_OUTER_DISTANCE = 768, MAX_OUTER_DISTANCE = 1024;

        public EndReturnGatewayBuilder(JavaPlugin plugin, int x, int y, int z) {
            this.plugin = plugin;
            this.gatewayX = x;
            this.gatewayY = Math.max(MIN_GATEWAY_Y, Math.min(MAX_GATEWAY_Y, y));
            this.gatewayZ = z;
        }

        public EndReturnGatewayBuilder(JavaPlugin plugin, double angleDegrees, int distance, int y) {
            this.plugin = plugin;
            double angleRad = Math.toRadians(angleDegrees);
            int clampedDistance = Math.max(MIN_OUTER_DISTANCE, Math.min(MAX_OUTER_DISTANCE, distance));
            this.gatewayX = (int) Math.round(clampedDistance * Math.cos(angleRad));
            this.gatewayZ = (int) Math.round(clampedDistance * Math.sin(angleRad));
            this.gatewayY = Math.max(MIN_GATEWAY_Y, Math.min(MAX_GATEWAY_Y, y));
        }

        @Override
        public void run() {
            World endWorld = Bukkit.getWorlds().stream()
                    .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                    .findFirst().orElse(null);
            if (endWorld == null) return;

            Set<ChunkCoord> neededChunks = getNeededChunks(gatewayX, gatewayZ);
            List<CompletableFuture<Chunk>> loadFutures = new ArrayList<>();
            for (ChunkCoord coord : neededChunks) {
                loadFutures.add(endWorld.getChunkAtAsync(coord.chunkX, coord.chunkZ));
            }

            final World finalWorld = endWorld;
            CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> Bukkit.getRegionScheduler().run(plugin, finalWorld,
                            gatewayX >> 4, gatewayZ >> 4,
                            task -> buildReturnGateway(finalWorld, gatewayX, gatewayY, gatewayZ)));
        }

        private void buildReturnGateway(World world, int x, int y, int z) {
            int[][] bedrockOffsets = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
            for (int[] offset : bedrockOffsets) {
                Block block = world.getBlockAt(x + offset[0], y + offset[1], z + offset[2]);
                if (block.getType() != Material.BEDROCK) block.setType(Material.BEDROCK, false);
            }
            Block gatewayBlock = world.getBlockAt(x, y, z);
            gatewayBlock.setType(Material.END_GATEWAY, false);
            if (gatewayBlock.getState() instanceof org.bukkit.block.EndGateway gateway) {
                gateway.setExitLocation(new Location(world, 0, 50, 0));
                gateway.setExactTeleport(false);
                gateway.update(true, false);
            }
        }

        private Set<ChunkCoord> getNeededChunks(int centerX, int centerZ) {
            Set<ChunkCoord> chunks = new HashSet<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    chunks.add(new ChunkCoord((centerX + dx) >> 4, (centerZ + dz) >> 4));
                }
            }
            return chunks;
        }
    }

    private static class ChunkCoord {
        final int chunkX, chunkZ;
        ChunkCoord(int x, int z) { this.chunkX = x; this.chunkZ = z; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof ChunkCoord c)) return false;
            return chunkX == c.chunkX && chunkZ == c.chunkZ;
        }
        @Override public int hashCode() { return Objects.hash(chunkX, chunkZ); }
    }
}
