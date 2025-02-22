package me.txmc.core.antiillegal.listeners;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.block.Block;

import static java.lang.Math.abs;

/**
 * Listener for cleaning illegal blocks in chunks when they are loaded.
 *
 * <p>This class is part of the 8b8tCore plugin and is responsible for removing blocks that are considered
 * illegal (such as Bedrock, End Portal Frames, Reinforced Deepslate, and Barriers) from chunks as they
 * are loaded into the world.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Handling the ChunkLoadEvent to process chunks when they are loaded</li>
 *     <li>Checking each block within the chunk to determine if it is illegal</li>
 *     <li>Replacing illegal blocks with air</li>
 * </ul>
 *
 * <p>Note: The range of Y coordinates is dynamically adjusted based on the world type, with special handling
 * for the Nether environment.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/06 23:30
 */

@Slf4j
public class IllegalBlocksCleaner implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        if(event.isNewChunk()) return;

        Chunk chunk = event.getChunk();
        if(abs(chunk.getX()) >= 1000000 || abs(chunk.getZ()) >= 1000000) return;

        int yUpperLimit = event.getWorld().getMaxHeight();

        int yLowerLimit = event.getWorld().getMinHeight();

        World.Environment worldEnv = event.getWorld().getEnvironment();

        if (worldEnv == World.Environment.NETHER) {
            yUpperLimit = 127;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yLowerLimit; y < yUpperLimit; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material type = block.getType();

                    // NETHER
                    if (worldEnv == World.Environment.NETHER && (
                            type == Material.END_PORTAL_FRAME ||
                            type == Material.REINFORCED_DEEPSLATE ||
                            type == Material.BARRIER ||
                            type == Material.LIGHT ||
                            type == Material.END_PORTAL ||
                            (type == Material.BEDROCK && y >= yLowerLimit + 5 && y <= yUpperLimit - 5))) {
                        block.setType(Material.AIR);
                    }

                    // OVERWORLD
                    if (worldEnv == World.Environment.NORMAL && (
                            type == Material.END_PORTAL_FRAME ||
                            type == Material.REINFORCED_DEEPSLATE ||
                            type == Material.BARRIER ||
                            type == Material.LIGHT ||
                            type == Material.END_PORTAL ||
                            (type == Material.BEDROCK && y >= yLowerLimit + 5))) {
                        block.setType(Material.AIR);
                    }

                    // END
                    if (worldEnv == World.Environment.THE_END && (
                            type == Material.END_PORTAL_FRAME ||
                            type == Material.REINFORCED_DEEPSLATE ||
                            type == Material.BARRIER ||
                            type == Material.LIGHT ||
                            type == Material.END_PORTAL ||
                            (type == Material.BEDROCK))) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
}