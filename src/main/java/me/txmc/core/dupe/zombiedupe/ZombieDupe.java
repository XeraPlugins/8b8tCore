package me.txmc.core.dupe.zombiedupe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

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
public class ZombieDupe implements Listener {

    private final Main plugin;

    private final Cache<UUID, Long> cooldowns = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityHitByArrow(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        if (!plugin.getConfig().getBoolean("ZombieDupe.enabled", true)) return;
        
        boolean votersOnly = plugin.getConfig().getBoolean("ZombieDupe.votersOnly", false);
        if (votersOnly && !player.hasPermission("8b8tcore.dupe.zombie")) return;

        boolean isGliding = player.isGliding();
        boolean isJumpFalling = player.hasPotionEffect(PotionEffectType.JUMP_BOOST) 
                                && player.getLocation().getBlock().getType() == Material.AIR 
                                && player.getVelocity().getY() < 0;

        if (isGliding || isJumpFalling) {
            event.setDamage(0);
            arrow.remove();

            int probability = plugin.getConfig().getInt("ZombieDupe.probabilityPercentage", 100);
            if (probability < 100) {
                if (ThreadLocalRandom.current().nextInt(100) >= probability) return;
            }

            long cooldownTime = plugin.getConfig().getLong("ZombieDupe.dupeCooldown", 200L);
            UUID chunkUUID = getChunkUUID(zombie.getLocation().getChunk());

            Long lastDupe = cooldowns.getIfPresent(chunkUUID);
            if (lastDupe != null && System.currentTimeMillis() - lastDupe < cooldownTime) {
                sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
                return;
            }

            int maxItems = plugin.getConfig().getInt("ZombieDupe.limitItemsPerChunk", 18);
            if (countItemsInChunk(zombie.getLocation().getChunk()) >= maxItems) {
                sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
                return;
            }

            ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
            if (itemInHand.getType() != Material.AIR) {
                zombie.getWorld().dropItemNaturally(zombie.getLocation(), itemInHand.clone());

                int fleshChance = plugin.getConfig().getInt("ZombieDupe.rottenFleshDropPercentage", 50);
                if (ThreadLocalRandom.current().nextInt(100) < fleshChance) {
                    zombie.getWorld().dropItemNaturally(zombie.getLocation(), new ItemStack(Material.ROTTEN_FLESH));
                }

                cooldowns.put(chunkUUID, System.currentTimeMillis());
            }
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