package me.txmc.core.deathmessages;

import me.txmc.core.util.deathmessages.ExplosionManager;
import me.txmc.core.util.deathmessages.PlayerManager;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeath implements Listener {

    /*
    @EventHandler
    synchronized void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player p && Bukkit.getOnlinePlayers().contains((Player) e.getEntity())) {
            PlayerManager pm = PlayerManager.getPlayer(p);
            if (pm == null)
                pm = new PlayerManager(p);
            if (e.getEntity().getLastDamageCause() == null) {
                pm.setLastDamageCause(EntityDamageEvent.DamageCause.CUSTOM);
            } else {
                pm.setLastDamageCause(e.getEntity().getLastDamageCause().getCause());
            }
            if (!(pm.getLastEntityDamager() instanceof LivingEntity) || pm.getLastEntityDamager() == e.getEntity()) {
                if (pm.getLastExplosiveEntity() instanceof EnderCrystal) {
                    TextComponent tx = Assets.getNaturalDeath(pm, "End-Crystal");
                    if (tx == null) return;
                } else if (pm.getLastExplosiveEntity() instanceof TNTPrimed) {
                    TextComponent tx = Assets.getNaturalDeath(pm, "TNT");
                    if (tx == null) return;
                } else if (pm.getLastExplosiveEntity() instanceof Firework) {
                    TextComponent tx = Assets.getNaturalDeath(pm, "Firework");
                    if (tx == null) return;
                } else if (pm.getLastClimbing() != null && pm.getLastDamage() == EntityDamageEvent.DamageCause.FALL) {
                    TextComponent tx = Assets.getNaturalDeath(pm, "Climbable");
                    if (tx == null) return;
                } else if (pm.getLastDamage() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    ExplosionManager explosionManager = ExplosionManager.getManagerIfEffected(p.getUniqueId());
                    if (explosionManager == null) return;
                    TextComponent tx = null;

                    if (explosionManager.getMaterial().name().contains("BED")) {
                        tx = Assets.getNaturalDeath(pm, "Bed");
                    }
                    if (explosionManager.getMaterial() == Material.RESPAWN_ANCHOR) {
                        tx = Assets.getNaturalDeath(pm, "Respawn-Anchor");
                    }

                    if (tx == null) return;

                } else if (pm.getLastDamage().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    TextComponent tx = Assets.getNaturalDeath(pm, Assets.getSimpleProjectile(pm.getLastProjectileEntity()));
                } else {
                    TextComponent tx = Assets.getNaturalDeath(pm, Assets.getSimpleCause(pm.getLastDamage()));
                    if (tx == null) return;
                }
            }
        }
    }
     */
}
