package me.txmc.core.deathmessages;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendDeathMessage;

/**
 * This class was created as a part of 8b8tDeathMessages.
 * It listens for player death events and handles the sending of customized death messages.
 *
 * <p>Designed to provide unique and detailed messages when a player dies in various scenarios,
 * including by other players, entities, and projectiles.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Handling player death events and setting custom death messages</li>
 *     <li>Identifying the killer and the weapon used</li>
 *     <li>Distinguishing between different types of death causes</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/15 8:12 PM
 */
public class DeathMessageListener implements Listener {

    private final Map<UUID, Long> lastDeathTimes = new HashMap<>();
    private static final long DEATH_COOLDOWN = 30000;

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent e) {
        e.deathMessage(null);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        long currentTime = System.currentTimeMillis();
        long lastDeathTime = lastDeathTimes.getOrDefault(victimId, 0L);

        if (currentTime - lastDeathTime < DEATH_COOLDOWN) {
            event.setDeathMessage(null);
            return;
        }

        lastDeathTimes.put(victimId, currentTime);

        Entity killer = victim.getKiller();
        ItemStack weapon = null;

        if (victim.getLastDamageCause() == null) {
            return;
        }

        if (hasTotems(victim)) {
            return;
        }

        DeathCause deathCause = DeathCause.from(victim.getLastDamageCause().getCause());

        if (deathCause == DeathCause.UNKNOWN || deathCause == DeathCause.ENTITY_ATTACK) {
            return;
        }

        if (deathCause == DeathCause.ENTITY_EXPLOSION) {
            if (Bukkit.getOnlinePlayers().contains(killer)) {
                Player killerPlayer = (Player) killer;
                weapon = getKillWeapon(killerPlayer);

                deathCause = DeathCause.from(DeathCause.PLAYER);

                String causePath = deathCause.getPath();
                String killerName = (killer == null) ? "Leee" : killer.getName();
                String weaponName = (weapon.getType() == Material.AIR) ? "their hand" : getWeaponName(weapon);

                if (killerName.equals(victim.getName())) {
                    causePath = DeathCause.from(DeathCause.END_CRYSTAL).getPath();
                }
                sendDeathMessage(causePath, victim.getName(), killerName, weaponName);

            }
        } else if (killer == null) {
            String causePath = deathCause.getPath();
            sendDeathMessage(causePath, victim.getName(), "", "");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
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

        Entity killer = event.getDamager();
        ItemStack weapon = null;

        if (killer instanceof Projectile) {
            Projectile proj = (Projectile) killer;

            if (proj.getShooter() != null && proj.getShooter() instanceof Entity) {
                killer = (Entity) proj.getShooter();
            }
        }

        DeathCause deathCause = DeathCause.from(killer.getType());

        if (deathCause == DeathCause.WITHER) {
            deathCause = DeathCause.WITHER_BOSS;
        }

        if ((deathCause == DeathCause.PLAYER || deathCause == DeathCause.PROJECTILE || deathCause == DeathCause.ENTITY_EXPLOSION) && killer != null) {
            if (Bukkit.getOnlinePlayers().contains(killer)) {
                Player killerPlayer = (Player) killer;
                weapon = getKillWeapon(killerPlayer);

                deathCause = DeathCause.from(DeathCause.PLAYER);
            } else {
                deathCause = DeathCause.from(DeathCause.PROJECTILE);
            }
        }

        String killerName = (killer == null) ? "Leee" : killer.getName();
        String weaponName;
        if (weapon == null || weapon.getType() == Material.AIR) {
            weaponName = "their hand";
        } else {
            weaponName = getWeaponName(weapon);
        }

        String causePath = deathCause.getPath();

        if (deathCause == DeathCause.END_CRYSTAL) {
            return;
        }
        if (deathCause == DeathCause.UNKNOWN) {
            return;
        }

        lastDeathTimes.put(victimId, currentTime);
        sendDeathMessage(causePath, victim.getName(), killerName, weaponName);
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
