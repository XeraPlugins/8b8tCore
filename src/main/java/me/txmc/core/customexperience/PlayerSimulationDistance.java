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

public class PlayerSimulationDistance implements Listener {
    private final JavaPlugin plugin;
    private volatile int defaultDistance;

    public PlayerSimulationDistance(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.defaultDistance = plugin.getConfig().getInt("simulationdistance.default", 4);
    }

    public void handlePlayerJoin(Player player) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                setSimulationDistance(player);
            }
        }, null, 10L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }

    private void setSimulationDistance(Player player) {
        int simulationDistance = calculateSimulationDistance(player);
        
        try {
            player.setSimulationDistance(simulationDistance);
            log(Level.INFO, "Simulation distance set to " + simulationDistance + " chunks for player: " + player.getName());
        } catch (Exception e) {
            log(Level.WARNING, "Failed to set simulation distance for " + player.getName() + ": " + e.getMessage());
        }
    }

    private int calculateSimulationDistance(Player player) {
        int maxDistance = defaultDistance;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();
            if (permission.startsWith("8b8tcore.simulationdistance.")) {
                try {
                    String chunksStr = permission.substring("8b8tcore.simulationdistance.".length());
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