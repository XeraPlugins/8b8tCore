package me.txmc.core.antiillegal.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import me.txmc.core.antiillegal.check.checks.PlayerEffectCheck;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

/**
 * Listener for entities to check and fix illegal data.
 * @author MindComplexity (aka Libalpm)
 * @since 2026-01-08
 */
public class EntityEffectListener implements Listener {
    
    private final Plugin plugin;
    private final PlayerEffectCheck effectCheck;
    private final int checkInterval;

    private static final int MAX_ENTITY_NAME_PLAIN_LENGTH = 128;
    private static final int MAX_ENTITY_NAME_JSON_LENGTH = 4096;
    private static final int MAX_ENTITY_NAME_DEPTH = 8;
    private static final int MAX_ENTITY_NAME_NESTING = 3;
    private static final float MAX_EXPLOSION_POWER = 4.0f;
    private static final float MAX_FIREBALL_DAMAGE = 6.0f; 

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
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    entity.getScheduler().run(plugin, (stask) -> {
                        performChecks(entity);
                    }, null);
                }
            }
        }
    }
    
    private void performChecks(Entity entity) {
        if (!entity.isValid()) return;

        checkAndFixEntityName(entity);

        if (entity instanceof LivingEntity livingEntity && !(entity instanceof org.bukkit.entity.Player)) {
            if (!livingEntity.getActivePotionEffects().isEmpty()) {
                checkAndFixEntityEffects(livingEntity);
            }
        }

        if (entity instanceof Explosive) {
            checkAndFixExplosives((Explosive) entity);
        }
    }

    private void checkAndFixExplosives(Explosive explosive) {
        if (explosive.getYield() > MAX_EXPLOSION_POWER) {
            explosive.setYield(MAX_EXPLOSION_POWER);
        }
        
        if (explosive instanceof Fireball fireball) {
            if (fireball instanceof org.bukkit.entity.LargeFireball largeFireball) {
                explosive.setYield(Math.min(explosive.getYield(), MAX_EXPLOSION_POWER));
            }
            
            if (Double.isNaN(fireball.getAcceleration().getX()) || 
                Double.isNaN(fireball.getAcceleration().getY()) || 
                Double.isNaN(fireball.getAcceleration().getZ())) {
                fireball.remove();
            }
        }
    }

    private void checkAndFixEntityEffects(LivingEntity entity) {
        if (!entity.isValid() || entity.isDead()) return;
        
        if (effectCheck.checkEntityEffects(entity)) {
            effectCheck.fixEntityEffects(entity);
        }
    }

    private void checkAndFixEntityName(Entity entity) {
        if (entity == null || entity.customName() == null) return;

        Component name = entity.customName();
        
        boolean illegal = false;

        if (GlobalUtils.getComponentDepth(name) > MAX_ENTITY_NAME_DEPTH) {
            illegal = true;
        } else {
            String json = GsonComponentSerializer.gson().serialize(name);
            if (json.length() > MAX_ENTITY_NAME_JSON_LENGTH) {
                illegal = true;
            } else {
                String plainText = GlobalUtils.getStringContent(name);
                if (plainText.length() > MAX_ENTITY_NAME_PLAIN_LENGTH) {
                    illegal = true;
                } else if (countNestingDepth(json) > MAX_ENTITY_NAME_NESTING) {
                    illegal = true;
                }
            }
        }

        if (illegal) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
        }
    }
    
    private int countNestingDepth(String json) {
        int maxDepth = 0;
        int currentDepth = 0;
        for (int i = 0; i < json.length() - 6; i++) {
            if (json.regionMatches(i, "\"extra\"", 0, 7)) {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            }
        }
        return maxDepth;
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
        for (Entity entity : event.getChunk().getEntities()) {
            entity.getScheduler().run(plugin, (task) -> {
               performChecks(entity);
            }, null);
        }
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        performChecks(entity);
    }
    
    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
    }
}