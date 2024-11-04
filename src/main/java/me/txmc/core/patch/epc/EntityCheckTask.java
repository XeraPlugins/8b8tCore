package me.txmc.core.patch.epc;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

@RequiredArgsConstructor
public class EntityCheckTask implements Runnable {
    private final PatchSection main;

    @Override
    public void run() {
        try {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    Bukkit.getRegionScheduler().execute(main.plugin(), chunk.getBlock(chunk.getX(), 0, chunk.getZ()).getLocation(), () -> {
                        Entity[] chunkEntities = chunk.getEntities();
                        if (chunkEntities.length == 0) return;

                        main.entityPerChunk().forEach((entityType, maxAllowed) -> {
                            Entity[] filteredEntities = Arrays.stream(chunkEntities)
                                    .filter(en -> en.getType() == entityType)
                                    .toArray(Entity[]::new);

                            int excessCount = filteredEntities.length - maxAllowed;
                            if (excessCount > 0) {
                                log(Level.INFO, "Removing %d entities from chunk %d,%d in world %s",
                                        excessCount, chunk.getX(), chunk.getZ(), world.getName());

                                for (int i = 0; i < excessCount; i++) {
                                    Entity entityToRemove = filteredEntities[i];
                                    if (entityToRemove.isValid()) {
                                        entityToRemove.remove();
                                    }
                                }
                            }
                        });
                    });
                }
            }
        } catch (Exception ignore) {
            //log(Level.SEVERE, "An error occurred while checking entities: %s", ex.getMessage());
            //ex.printStackTrace();
        }
    }
}
