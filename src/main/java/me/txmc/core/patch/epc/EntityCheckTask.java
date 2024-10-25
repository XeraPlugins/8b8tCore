package me.txmc.core.patch.epc;

import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

@RequiredArgsConstructor
public class EntityCheckTask implements Runnable {
    private final PatchSection main;

    @Override
    public void run() {
        try {
            for (Chunk[] chunks : Bukkit.getWorlds().stream().map(World::getLoadedChunks).toList()) {
                for (Chunk chunk : chunks) {
                    if (chunk.getEntities().length == 0) continue;
                    main.entityPerChunk().forEach((e, i) -> {
                        Entity[] entities = Arrays.stream(chunk.getEntities())
                                .filter(en -> en.getType() == e)
                                .toList()
                                .toArray(Entity[]::new);
                        int amt = entities.length;
                        if (amt >= i) {
                            log(Level.INFO, "Removed %d entities from chunk %d,%d in world %s", amt - i, chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
                            for (int j = 0; j < amt - i; j++) {
                                Entity entity = entities[j];
                                entity.getScheduler().run(main.plugin(), (t) -> entity.remove(), () -> {});
                            }
                        }
                    });
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "An error occurred while checking entities: %s", ex.getMessage());
            ex.printStackTrace();
        }
    }
}
