package me.txmc.core.deathmessages;

import me.txmc.core.Main;
import me.txmc.core.util.deathmessages.EntityManager;
import me.txmc.core.util.deathmessages.PlayerManager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class EntityDamageByEntity implements Listener {
    private final HashMap<UUID, Entity> explosions = new HashMap<>();
    @EventHandler
    public void onPlayerDie(PlayerDeathEvent e) {
        e.setDeathMessage(null);
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageByEntityEvent e) {
        Player p = (Player) e.getEntity();
        PlayerManager pm = PlayerManager.getPlayer(p);

        if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
            if (e.getDamager() instanceof org.bukkit.entity.EnderCrystal && explosions.containsKey(e.getDamager().getUniqueId())) {
                pm.setLastEntityDamager(explosions.get(e.getDamager().getUniqueId()));
                pm.setLastExplosiveEntity(e.getDamager());
            } else if (e.getDamager() instanceof TNTPrimed tnt) {
                if (tnt.getSource() instanceof org.bukkit.entity.LivingEntity) {
                    pm.setLastEntityDamager(tnt.getSource());
                    pm.setLastExplosiveEntity(e.getDamager());
                }
            } else if (e.getDamager() instanceof Firework firework) {
                try {
                    if (firework.getShooter() instanceof org.bukkit.entity.LivingEntity) {
                        pm.setLastEntityDamager((Entity) firework.getShooter());
                        pm.setLastExplosiveEntity(e.getDamager());
                    }
                } catch (NoSuchMethodError noSuchMethodError) {
                    // ignore
                }
            } else {
                pm.setLastEntityDamager(e.getDamager());
                pm.setLastExplosiveEntity(e.getDamager());
            }
        } else if (e.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity) {
                pm.setLastEntityDamager((Entity) projectile.getShooter());
                pm.setLastProjectileEntity(projectile);
            }
        } else if (e.getDamager() instanceof FallingBlock) {
            pm.setLastEntityDamager(e.getDamager());
        } else if (e.getDamager().getType().isAlive()) {
            pm.setLastEntityDamager(e.getDamager());
        } else if (!(e.getEntity() instanceof Player) && e.getDamager() instanceof Player) {
            if (Main.getInstance().getConfig().getConfigurationSection("Entities") == null) return;
            Set<String> listenedMobs = Main.getInstance().getConfig().getConfigurationSection("Entities").getKeys(false);
            if (listenedMobs.isEmpty()) return;
            for (String listened : listenedMobs) {
                if (listened.contains(e.getEntity().getType().getEntityClass().getSimpleName().toLowerCase())) {
                    EntityManager em = EntityManager.getEntity(e.getEntity().getUniqueId());

                    if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
                        if (e.getDamager() instanceof EnderCrystal && explosions.containsKey(e.getDamager().getUniqueId())) {
                            if (explosions.get(e.getDamager().getUniqueId()) instanceof Player) {
                                em.setLastPlayerDamager(PlayerManager.getPlayer((Player) explosions.get(e.getDamager().getUniqueId())));
                                em.setLastExplosiveEntity(e.getDamager());
                            }
                            continue;
                        }
                        if (e.getDamager() instanceof TNTPrimed tnt) {
                            if (tnt.getSource() instanceof Player) {
                                em.setLastPlayerDamager(PlayerManager.getPlayer((Player) tnt.getSource()));
                                em.setLastExplosiveEntity(e.getDamager());
                            }
                            continue;
                        }
                        if (e.getDamager() instanceof Firework firework) {
                            try {
                                if (firework.getShooter() instanceof Player) {
                                    em.setLastPlayerDamager(PlayerManager.getPlayer((Player) firework.getShooter()));
                                    em.setLastExplosiveEntity(e.getDamager());
                                }
                            } catch (NoSuchMethodError noSuchMethodError) {
                                // ignore
                            }
                            continue;
                        }
                        em.setLastPlayerDamager(PlayerManager.getPlayer((Player) e.getDamager()));
                        em.setLastExplosiveEntity(e.getDamager());
                        continue;
                    }
                    if (e.getDamager() instanceof Projectile projectile) {
                        if (projectile.getShooter() instanceof Player) {
                            em.setLastPlayerDamager(PlayerManager.getPlayer((Player) projectile.getShooter()));
                            em.setLastProjectileEntity(projectile);
                        }
                        continue;
                    }
                    if (e.getDamager() instanceof Player) {
                        em.setLastPlayerDamager(PlayerManager.getPlayer((Player) e.getDamager()));
                    }
                }
            }
        }
        if (e.getEntity() instanceof EnderCrystal) {
            if (e.getDamager().getType().isAlive()) {
                explosions.put(e.getEntity().getUniqueId(), e.getDamager());
            } else if (e.getDamager() instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof LivingEntity)
                    explosions.put(e.getEntity().getUniqueId(), (Entity) projectile.getShooter());
            }
        }
    }
}

