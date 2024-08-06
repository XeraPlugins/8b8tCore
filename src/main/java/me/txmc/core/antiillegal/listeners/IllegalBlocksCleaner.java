package me.txmc.core.antiillegal.listeners;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.block.Block;

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

public class IllegalBlocksCleaner implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        Chunk chunk = event.getChunk();

        int yUpperLimit = event.getWorld().getMaxHeight();
        int yLowerLimit = event.getWorld().getMinHeight() + 5;

        if(event.getWorld().getEnvironment() == World.Environment.NETHER){
            yUpperLimit = 125;
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yLowerLimit; y < yUpperLimit; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (
                            block.getType() == Material.BEDROCK ||
                            block.getType() == Material.END_PORTAL_FRAME ||
                            block.getType() == Material.REINFORCED_DEEPSLATE ||
                            block.getType() == Material.BARRIER
                    ) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
}