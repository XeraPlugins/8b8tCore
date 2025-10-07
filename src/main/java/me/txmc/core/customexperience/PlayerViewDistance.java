package me.txmc.core.customexperience;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;
import static me.txmc.core.util.GlobalUtils.log;

/**
 * This class is part of the custom experience module for the 8b8tCore plugin.
 * It listens for player join events and sets the view distance based on permissions.
 *
 * <p>Designed to customize the view distance for players according to their permissions.
 * Each player can have a unique view distance set when they join the server.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Listening for player join events</li>
 *     <li>Checking player permissions to determine view distance</li>
 *     <li>Setting the view distance for each player</li>
 * </ul>
 *
 * <p>The view distance is determined by the highest valid permission found,
 * within a range of 2 to 32 chunks. If no specific permission is found, a default value is used.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/18 12:42 PM
 */

public class PlayerViewDistance implements Listener {
    private final JavaPlugin plugin;

    public PlayerViewDistance(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerJoin(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    setRenderDistance(player);
                }
            }
        }.runTaskLater(plugin, 10L); // 0.5 second delay
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
        int defaultDistance = plugin.getConfig().getInt("viewdistance.default", 2);
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
                    // If it's not here it's an invalid permission format. So we will skip.
                }
            }
        }

        return maxDistance;
    }
}