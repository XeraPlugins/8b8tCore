/*
 * IllegalBlocksCleaner.java
 */
package me.txmc.core.antiillegal.listeners;

import me.txmc.core.Main;
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

    /**
     * @param plugin         main plugin instance
     * @param blockPatterns  wildcard patterns from config
     */
    public IllegalBlocksCleaner(Main plugin, List<String> blockPatterns) {
        this.plugin = plugin;
        this.illegalMaterials = buildMaterialSet(blockPatterns);
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

        // Use ChunkSnapshot for fast reads
        ChunkSnapshot snap = chunk.getChunkSnapshot(false, false, false);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Material type = snap.getBlockType(x, y, z);
                    if (!illegalMaterials.contains(type)) continue;

                    // Bedrock rules per dimension
                    if (type == Material.BEDROCK) {
                        if (env == World.Environment.NETHER) {
                            // skip bottom 5 and top 5 layers
                            if (y < minY + 5 || y > maxY - 5) continue;
                        } else if (env == World.Environment.NORMAL) {
                            // skip bottom 5 layers
                            if (y < minY + 5) continue;
                        }
                        // THE_END: remove all bedrock
                    }

                    // Remove the block
                    Block b = chunk.getBlock(x, y, z);
                    b.setType(Material.AIR);
                }
            }
        }
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
