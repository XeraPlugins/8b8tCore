package me.txmc.core.patch.listeners;

import me.txmc.core.Main;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
 */
public class AntiLagChest implements Listener {
    private final ConcurrentHashMap<UUID, Long> lastOpen = new ConcurrentHashMap<>();
    private final Main plugin;
    private final long cooldown;

    public AntiLagChest(Main plugin) {
        this.plugin = plugin;
        this.cooldown = plugin.getConfig().getLong("AntiLagChest.openCooldown", 2000L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isChest(block.getType())) return;

        double tps = GlobalUtils.getCurrentRegionTps();
        if (tps > 15.0 || tps == -1) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastOpen.getOrDefault(uuid, 0L);

        if (now - last < cooldown) {
            event.setCancelled(true);
            GlobalUtils.sendPrefixedLocalizedMessage(player, "chest_cooldown");
        } else {
            lastOpen.put(uuid, now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastOpen.remove(event.getPlayer().getUniqueId());
    }

    private boolean isChest(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL ||
               type == Material.SHULKER_BOX || type.name().contains("SHULKER_BOX") ||
               type == Material.ENDER_CHEST || type == Material.DISPENSER || type == Material.DROPPER ||
               type == Material.HOPPER;
    }
}