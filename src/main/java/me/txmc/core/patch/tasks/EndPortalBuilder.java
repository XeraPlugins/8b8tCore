package me.txmc.core.patch.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This class is responsible for building an End Portal in the Overworld at specific coordinates.
 * It generates the structure of the portal using End Portal Frame and End Portal blocks.
 *
 * <p>The functionality includes:</p>
 * <ul>
 *     <li>Scheduling the task to run and build the End Portal in the Overworld</li>
 *     <li>Placing End Portal Frame blocks around a 3x3 area, setting the frame to contain an Eye of Ender</li>
 *     <li>Placing End Portal blocks inside the frame to form the actual portal</li>
 * </ul>
 *
 * <p>The portal is constructed at fixed coordinates (x=0, y=<world min height> + 5, z=0), and the task is scheduled to run on the Overworld.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2025/02/26 13:40 PM
 */


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

        Bukkit.getRegionScheduler().run(plugin, world, x, z, task -> buildEndPortal(world, x, y, z));
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
            frame.setType(Material.END_PORTAL_FRAME);
            EndPortalFrame data = (EndPortalFrame) frame.getBlockData();
            data.setEye(true);
            frame.setBlockData(data);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.END_PORTAL);
            }
        }
    }
}
