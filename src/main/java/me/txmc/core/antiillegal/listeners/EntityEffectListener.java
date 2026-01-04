package me.txmc.core.antiillegal.listeners;

import me.txmc.core.antiillegal.check.checks.PlayerEffectCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for entities to check and fix illegal potion effects.
 * Some players managed to work around to using mobs to have infinite HP, instant kill, etc.
 * This should fix the problem and revert the effects on entites on chunk load.
 * @author MindComplexity (aka Libalpm)
 * @since 2025-10-2
 */
public class EntityEffectListener implements Listener {
    
    private final Plugin plugin;
    private final PlayerEffectCheck effectCheck;
    private final int checkInterval;

    public EntityEffectListener(Plugin plugin) {
        this(plugin, 600);
    }

    public EntityEffectListener(Plugin plugin, int checkInterval) {
        this.plugin = plugin;
        this.effectCheck = new PlayerEffectCheck();
        this.checkInterval = checkInterval;
        startEntityEffectChecker();
    }

    private void startEntityEffectChecker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            checkAllEntities();
        }, 20L, checkInterval);
    }

    private void checkAllEntities() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                entity.getScheduler().run(plugin, (task) -> {
                    if (entity.isValid() && !entity.isDead() && !(entity instanceof org.bukkit.entity.Player)) {
                        if (!entity.getActivePotionEffects().isEmpty()) {
                            checkAndFixEntityEffects(entity);
                        }
                    }
                }, null);
            }
        }
    }
    
    private void checkAndFixEntityEffects(LivingEntity entity) {
        if (!entity.isValid() || entity.isDead()) return;
        
        if (effectCheck.checkEntityEffects(entity)) {
            effectCheck.fixEntityEffects(entity);
        }
    }
    
    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            return;
        }
        
        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED || 
            event.getAction() == EntityPotionEffectEvent.Action.CHANGED) {
            
            if (event.getNewEffect() != null && effectCheck.isIllegalEffect(event.getNewEffect())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;        
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.getScheduler().run(plugin, (task) -> {
                    if (livingEntity.isValid() && !livingEntity.isDead() && !(livingEntity instanceof org.bukkit.entity.Player)) {
                        if (!livingEntity.getActivePotionEffects().isEmpty()) {
                            checkAndFixEntityEffects(livingEntity);
                        }
                    }
                }, null);
            }
        }
    }
    
    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
    }
}
