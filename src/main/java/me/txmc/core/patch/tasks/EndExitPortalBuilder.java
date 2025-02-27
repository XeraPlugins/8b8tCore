package me.txmc.core.patch.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;

/**
 * This class is responsible for building an exit portal in the End world within the Minecraft server.
 * It generates the structure of the portal using Bedrock and End Portal blocks at specific coordinates.
 *
 * <p>The functionality includes:</p>
 * <ul>
 *     <li>Scheduling the task to run and build the portal in the End world</li>
 *     <li>Building two layers of Bedrock as the base of the portal</li>
 *     <li>Placing the End Portal blocks in a specific pattern to create the exit portal</li>
 *     <li>Creating an additional column of Bedrock to reinforce the portal structure</li>
 * </ul>
 *
 * <p>The portal is constructed at fixed coordinates (x=0, y=60, z=0), and the task is scheduled to run on the End world</p>
 *
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2025/02/26 13:40 PM
 */


public class EndExitPortalBuilder implements Runnable {

    private final JavaPlugin plugin;

    public EndExitPortalBuilder(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        World endWorld = Bukkit.getWorlds().get(2);
        if (endWorld == null) {
            return;
        }

        int x = 0, y = 60, z = 0;

        Bukkit.getRegionScheduler().execute(plugin, endWorld, x, z, () -> buildEndPortal(endWorld, x, y, z));
    }

    private void buildEndPortal(World world, int x, int y, int z) {
        int[][] bedrockLayer1 = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1},{-1, -1}, {0, -1}, {1, -1}, {2, -1},
                {-2, 0},{-1, 0}, {0, 0}, {1, 0}, {2, 0},
                {-2, 1},{-1, 1}, {0, 1}, {1, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : bedrockLayer1) {
            Block block = world.getBlockAt(x + offset[0], y, z + offset[1]);
            block.setType(Material.BEDROCK);
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
            Block block = world.getBlockAt(x + offset[0], y + 1, z + offset[1]);
            block.setType(Material.BEDROCK);
        }

        int[][] portalBlocks = {
                {-1, -2}, {0, -2}, {1, -2},
                {-2, -1},{-1, -1}, {0, -1}, {1, -1}, {2, -1},
                {-2, 0}, {-1, 0}, {1, 0}, {2, 0},
                {-2, 1},{-1, 1}, {0, 1}, {1, 1}, {2, 1},
                {-1, 2}, {0, 2}, {1, 2}
        };

        for (int[] offset : portalBlocks) {
            Block block = world.getBlockAt(x + offset[0], y + 1, z + offset[1]);
            block.setType(Material.END_PORTAL);
        }

        for (int dy = 1; dy <= 4; dy++) {
            world.getBlockAt(x, y + dy, z).setType(Material.BEDROCK);
        }
    }
}