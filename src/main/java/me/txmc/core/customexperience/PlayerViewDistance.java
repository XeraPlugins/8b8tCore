package me.txmc.core.customexperience;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import static me.txmc.core.util.GlobalUtils.log;

/**
 * This class is part of the 8b8tCore plugin.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/01/02 12:42 PM
 */

public class PlayerViewDistance implements Listener {
    private final JavaPlugin plugin;
    private volatile int defaultDistance;

    public PlayerViewDistance(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.defaultDistance = plugin.getConfig().getInt("viewdistance.default", 2);
    }

    public void handlePlayerJoin(Player player) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                setRenderDistance(player);
            }
        }, null, 10L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }

    private void setRenderDistance(Player player) {
        int renderDistance = calculateRenderDistance(player);
        
        try {
            player.setSendViewDistance(renderDistance);
            player.setViewDistance(renderDistance);
            log(Level.INFO, "View distance set to " + renderDistance + " chunks for player: " + player.getName());
        } catch (Exception e) {
            log(Level.WARNING, "Failed to set view distance for " + player.getName() + ": " + e.getMessage());
        }
    }

    private int calculateRenderDistance(Player player) {
        int maxDistance = defaultDistance;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();
            if (permission.startsWith("8b8tcore.viewdistance.")) {
                try {
                    String chunksStr = permission.substring("8b8tcore.viewdistance.".length());
                    int chunks = Integer.parseInt(chunksStr);
                    chunks = Math.max(2, Math.min(32, chunks));
                    maxDistance = Math.max(maxDistance, chunks);
                } catch (NumberFormatException ignored) {
                    // Invalid permission format, skip
                }
            }
        }

        return maxDistance;
    }
}