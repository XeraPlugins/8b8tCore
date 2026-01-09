package me.txmc.core.patch.epc;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

@RequiredArgsConstructor
public class EntitySpawnListener implements Listener {
    private final PatchSection main;

    @EventHandler
    public void onEntitySpawn(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        EntityType type = entity.getType();
        
        if (!main.entityPerChunk().containsKey(type)) return;
        
        int amt = enumerate(entity.getLocation().getChunk(), type);
        int max = main.entityPerChunk().get(type);
        
        if (amt >= max) {
            entity.getScheduler().run(main.plugin(), (t) -> {
                if (entity.isValid()) {
                    entity.remove();
                }
            }, null);
        }
    }

    private int enumerate(Chunk chunk, EntityType entityType) {
        return (int) Arrays.stream(chunk.getEntities()).filter(e -> e.getType() == entityType).count();
    }
}
