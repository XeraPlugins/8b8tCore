package me.txmc.core.patch.epc;

import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

/**
 * This file is apart of 8b8tcore.
 * @author 254n_m
 * @author MindComplexity (aka Libalpm)
 * @since 2026/01/02
 */
public class EntityCheckTask implements Runnable {
    private final PatchSection main;

    public EntityCheckTask(PatchSection main) {
        this.main = main;
    }

    @Override
    public void run() {
        try {
            for (World world : Bukkit.getWorlds()) {
                Chunk[] loadedChunks = world.getLoadedChunks();
                
                for (Chunk chunk : loadedChunks) {
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();
                    
                    Location chunkLoc = new Location(world, chunkX << 4, 64, chunkZ << 4);
                    
                    Bukkit.getRegionScheduler().run(main.plugin(), chunkLoc, (task) -> {
                        if (!world.isChunkLoaded(chunkX, chunkZ)) return;
                        Chunk currentChunk = world.getChunkAt(chunkX, chunkZ);
                        
                        Entity[] chunkEntities = currentChunk.getEntities();
                        if (chunkEntities.length == 0) return;

                        main.entityPerChunk().forEach((entityType, maxAllowed) -> {
                            Entity[] filteredEntities = Arrays.stream(chunkEntities)
                                    .filter(en -> en.getType() == entityType && en.isValid())
                                    .toArray(Entity[]::new);

                            int excessCount = filteredEntities.length - maxAllowed;
                            if (excessCount > 0) {
                                for (int i = 0; i < excessCount; i++) {
                                    Entity entityToRemove = filteredEntities[i];
                                    entityToRemove.getScheduler().run(main.plugin(), (entityTask) -> {
                                        if (entityToRemove.isValid()) {
                                            entityToRemove.remove();
                                        }
                                    }, null);
                                }
                            }
                        });
                    });
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "An error occurred while checking entities: %s", ex.getMessage());
            ex.printStackTrace();
        }
    }
}
