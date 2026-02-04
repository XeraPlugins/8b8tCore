package me.txmc.core.dupe.framedupe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * This file is apart of the 8b8tCore plugin.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/02/04
*/

@RequiredArgsConstructor
public class FrameDupe implements Listener {

    private final Main plugin;

    private final Cache<UUID, Long> cooldowns = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFrameInteract(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame itemFrame)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        if (!plugin.getConfig().getBoolean("FrameDupe.enabled", true)) return;
        
        boolean votersOnly = plugin.getConfig().getBoolean("FrameDupe.votersOnly", false);
        if (votersOnly && !player.hasPermission("8b8tcore.dupe.frame")) return;

        int probability = plugin.getConfig().getInt("FrameDupe.probabilityPercentage", 100);
        if (probability < 100) {
            if (ThreadLocalRandom.current().nextInt(100) >= probability) {
                return;
            }
        }

        long cooldownTime = plugin.getConfig().getLong("FrameDupe.dupeCooldown", 200L);
        UUID chunkUUID = getChunkUUID(itemFrame.getLocation().getChunk());

        Long lastDupe = cooldowns.getIfPresent(chunkUUID);
        if (lastDupe != null && System.currentTimeMillis() - lastDupe < cooldownTime) {
            sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
            return;
        }

        int maxItems = plugin.getConfig().getInt("FrameDupe.limitItemsPerChunk", 18);
        if (countItemsInChunk(itemFrame.getLocation().getChunk()) >= maxItems) {
            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
            return;
        }

        ItemStack itemStack = itemFrame.getItem();
        if (itemStack.getType() != Material.AIR) {
            itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack.clone());
            cooldowns.put(chunkUUID, System.currentTimeMillis());
        }
    }

    private int countItemsInChunk(Chunk chunk) {
        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item) {
                count++;
            }
        }
        return count;
    }

    private UUID getChunkUUID(Chunk chunk) {
        long x = chunk.getX();
        long z = chunk.getZ();
        return new UUID(chunk.getWorld().getUID().getMostSignificantBits(), (x << 32) | (z & 0xFFFFFFFFL));
    }
}