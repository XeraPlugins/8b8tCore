package me.txmc.core.patch.epc;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.patch.PatchSection;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EntitySpawnListener implements Listener {
    private final PatchSection main;
    
    // Map<ChunkKey, Map<EntityType, Count>>
    // chunk.getChunkKey() is more memory efficient than the Chunk object
    private final Map<Long, Map<EntityType, Integer>> cache = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        EntityType type = entity.getType();
        
        Integer max = main.entityPerChunk().get(type);
        if (max == null) return;

        long chunkKey = entity.getLocation().getChunk().getChunkKey();
        
        Map<EntityType, Integer> chunkMap = cache.computeIfAbsent(chunkKey, k -> new HashMap<>());
        int currentCount = chunkMap.getOrDefault(type, 0) + 1;
        chunkMap.put(type, currentCount);

        if (currentCount > max) {
            // Decrement immediately when removed to prevent issues.
            chunkMap.put(type, currentCount - 1);
            
            entity.getScheduler().run(main.plugin(), (t) -> {
                if (entity.isValid()) entity.remove();
            }, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        EntityType type = event.getEntity().getType();
        if (!main.entityPerChunk().containsKey(type)) return;

        long chunkKey = event.getEntity().getLocation().getChunk().getChunkKey();
        Map<EntityType, Integer> chunkMap = cache.get(chunkKey);
        
        if (chunkMap != null) {
            chunkMap.computeIfPresent(type, (t, count) -> (count > 1) ? count - 1 : null);
            
            if (chunkMap.isEmpty()) {
                cache.remove(chunkKey);
            }
        }
    }
}
