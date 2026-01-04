package me.txmc.core.deathmessages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.txmc.core.util.GlobalUtils.sendDeathMessage;

/**
 * This class was created as a part of 8b8tDeathMessages.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/01/02 8:12 PM
 */
public class DeathMessageListener implements Listener {

    private final Map<UUID, Long> lastDeathTimes = new ConcurrentHashMap<>();
    private static final long DEATH_COOLDOWN = 30000;

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(null); // Suppress default message
        
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        long currentTime = System.currentTimeMillis();
        long lastDeathTime = lastDeathTimes.getOrDefault(victimId, 0L);

        if (currentTime - lastDeathTime < DEATH_COOLDOWN) {
            return;
        }

        if (victim.getLastDamageCause() == null) {
            return;
        }

        if (hasTotems(victim)) {
            return;
        }

        lastDeathTimes.put(victimId, currentTime);

        Player killer = victim.getKiller();
        DeathCause deathCause = DeathCause.from(victim.getLastDamageCause().getCause());

        if (deathCause == DeathCause.UNKNOWN) {
            return;
        }

        if (killer != null) {
            ItemStack weapon = getKillWeapon(killer);
            String weaponName = (weapon.getType() == Material.AIR) ? "their hand" : getWeaponName(weapon);
            String causePath = DeathCause.PLAYER.getPath();
            
            if (killer.getUniqueId().equals(victimId)) {
                causePath = DeathCause.END_CRYSTAL.getPath();
            }
            
            sendDeathMessage(causePath, victim.getName(), killer.getName(), weaponName);
        } else if (deathCause != DeathCause.ENTITY_ATTACK) {
            sendDeathMessage(deathCause.getPath(), victim.getName(), "", "");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID victimId = victim.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastDeathTime = lastDeathTimes.getOrDefault(victimId, 0L);

        if (currentTime - lastDeathTime < DEATH_COOLDOWN) {
            return;
        }

        if (hasTotems(victim)) {
            return;
        }

        if (victim.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        Entity damager = event.getDamager();
        Entity killer = damager;

        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            killer = shooter;
        }

        DeathCause deathCause = DeathCause.from(killer.getType());

        if (deathCause == DeathCause.WITHER) {
            deathCause = DeathCause.WITHER_BOSS;
        }

        if (killer instanceof Player killerPlayer) {
            ItemStack weapon = getKillWeapon(killerPlayer);
            String weaponName = (weapon.getType() == Material.AIR) ? "their hand" : getWeaponName(weapon);
            
            deathCause = DeathCause.PLAYER;
            
            if (deathCause == DeathCause.END_CRYSTAL || deathCause == DeathCause.UNKNOWN) {
                return;
            }
            
            lastDeathTimes.put(victimId, currentTime);
            sendDeathMessage(deathCause.getPath(), victim.getName(), killerPlayer.getName(), weaponName);
        } else {
            if (deathCause == DeathCause.END_CRYSTAL || deathCause == DeathCause.UNKNOWN) {
                return;
            }
            
            String killerName = killer.getName();
            lastDeathTimes.put(victimId, currentTime);
            sendDeathMessage(deathCause.getPath(), victim.getName(), killerName, "");
        }
    }

    private static ItemStack getKillWeapon(Player killer) {
        return killer.getInventory().getItemInMainHand();
    }

    private boolean hasTotems(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    private String getWeaponName(ItemStack weapon) {
        if (weapon == null) {
            return "";
        }

        ItemMeta meta = weapon.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(meta.displayName());
        } else {
            return weapon.getType().toString().replace("_", " ").toLowerCase();
        }
    }
}
