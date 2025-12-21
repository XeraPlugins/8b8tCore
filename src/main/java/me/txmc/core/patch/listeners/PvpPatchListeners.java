package me.txmc.core.patch.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * PatchPVP - 8b8tcore 
 * @author MindComplexity (aka Libalpm)
 * @since 12/20/2025
 */
public class PvpPatchListeners implements Listener {
    private final EnumSet<Material> suffocationMaterials = EnumSet.of(
            Material.OBSIDIAN, Material.BEDROCK, Material.NETHERITE_BLOCK,
            Material.BARRIER, Material.END_PORTAL_FRAME, Material.ANVIL,
            Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.REINFORCED_DEEPSLATE
    );
    private final Map<UUID, Long> lastDamage = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSuffocation(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == DamageCause.SUFFOCATION) {
                if (suffocationMaterials.contains(player.getLocation().getBlock().getType())) {
                    event.setDamage(7.0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (suffocationMaterials.contains(event.getTo().getBlock().getType())) {
            long now = System.currentTimeMillis();
            Long last = lastDamage.get(player.getUniqueId());
            if (last == null || (now - last) >= 500) {
                lastDamage.put(player.getUniqueId(), now);
                player.damage(7.0);
                player.teleportAsync(event.getFrom().add(0, 0.1, 0));
            }
        }
    }
}