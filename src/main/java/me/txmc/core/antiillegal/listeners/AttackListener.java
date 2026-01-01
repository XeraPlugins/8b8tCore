package me.txmc.core.antiillegal.listeners;

import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author MindComplexity
 * @since 2026/01/01
 * Uses Custom PVP meta.
 */
public class AttackListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Long> lastGroundTime = new HashMap<>();
    private final Map<UUID, Long> lastSmashTime = new HashMap<>();
    private final Map<UUID, Location> tickStartLocations = new HashMap<>();

    public AttackListener(Main plugin) {
        this.plugin = plugin;
        for (Player p : Bukkit.getOnlinePlayers()) {
            startTracking(p);
        }
    }

    private void startTracking(Player player) {
        player.getScheduler().runAtFixedRate(plugin, (task) -> {
            tickStartLocations.put(player.getUniqueId(), player.getLocation());
        }, null, 1L, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        startTracking(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        tickStartLocations.remove(uuid);
        lastGroundTime.remove(uuid);
        lastSmashTime.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (((org.bukkit.entity.Entity) player).isOnGround()) {
            lastGroundTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        tickStartLocations.put(player.getUniqueId(), event.getTo());
        if (((org.bukkit.entity.Entity) player).isOnGround()) {
            lastGroundTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean hasMace = mainHand.getType().name().equals("MACE") || offHand.getType().name().equals("MACE");
        boolean hasSpear = mainHand.getType().name().endsWith("_SPEAR") || offHand.getType().name().endsWith("_SPEAR");
        boolean hasTrident = mainHand.getType() == Material.TRIDENT || offHand.getType() == Material.TRIDENT;

        if (!hasMace && !hasSpear && !hasTrident) return;

        double damage = event.getDamage();
        Location targetLoc = event.getEntity().getLocation();
        double currDistance = player.getLocation().distance(targetLoc);
        Location origin = tickStartLocations.getOrDefault(player.getUniqueId(), player.getLocation());
        double tickStartDistance = origin.distance(targetLoc);

        double reachLimit = 8.5;
        if (currDistance > reachLimit || tickStartDistance > reachLimit + 3.0) {
            cancelAttack(event);
            return;
        }

        if (hasMace) {
            double fallDist = player.getFallDistance();
            long now = System.currentTimeMillis();
            long timeInAir = now - lastGroundTime.getOrDefault(player.getUniqueId(), now);            
            double t = timeInAir / 1000.0;
            double maxPhysFall = (25.0 * (t * t)) + (t * 2.0) + 1.5; 

            if (fallDist > maxPhysFall && timeInAir < 60000) {
                 cancelAttack(event);
                 return;
            }

            // Ratelimit maces to prevent them from being op af.
            if (damage > 30.0) {
                long lastSmash = lastSmashTime.getOrDefault(player.getUniqueId(), 0L);
                if (now - lastSmash < 600) {
                    cancelAttack(event);
                    return;
                }
                lastSmashTime.put(player.getUniqueId(), now);
            }
            // Damage Ceiling to prevent Maces from being overpowered af.
            if (fallDist < 1.0 && damage > 35.0) {
                event.setDamage(25.0); 
            } else if (fallDist >= 1.0) {
                double ceiling = (7.5 * fallDist) + 30.0;
                if (damage > ceiling && damage > 50.0) {
                    event.setDamage(ceiling);
                }
            }
            
            if (damage > 1000.0) event.setDamage(1000.0);
            return;
        }

        if (hasSpear || hasTrident) {
            if (damage > 150.0) {
                cancelAttack(event);
            }
        }
    }

    private void cancelAttack(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
    }
}
