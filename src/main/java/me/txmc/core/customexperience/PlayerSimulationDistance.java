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
 * This class is part of the custom experience module for the 8b8tCore plugin.
 * It listens for player join events and sets the simulation distance based on permissions.
 *
 * <p>Designed to customize the simulation distance for players according to their permissions.
 * Each player can have a unique simulation distance set when they join the server.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Listening for player join events</li>
 *     <li>Checking player permissions to determine simulation distance</li>
 *     <li>Setting the simulation distance for each player</li>
 * </ul>
 *
 * <p>The simulation distance is determined by the highest valid permission found,
 * within a range of 2 to 32 chunks. If no specific permission is found, a default value is used.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/18 12:42 PM
 */
public class PlayerSimulationDistance implements Listener {
    private final JavaPlugin plugin;

    public PlayerSimulationDistance(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setSimulationDistance(player);
    }

    private void setSimulationDistance(Player player) {
        int simulationDistance = plugin.getConfig().getInt("simulationdistance.default", 4);
        int maxChunks = 2;

        for (String permission : player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).toList()) {
            if (permission.startsWith("8b8tcore.simulationdistance.")) {
                String chunksStr = permission.replace("8b8tcore.simulationdistance.", "");
                try {
                    int chunks = Integer.parseInt(chunksStr);
                    chunks = Math.max(2, Math.min(32, chunks));

                    if (chunks > maxChunks) {
                        maxChunks = chunks;
                    }
                    simulationDistance = maxChunks;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        player.setSimulationDistance(simulationDistance);
        log(Level.INFO,"Simulation distance set to " + simulationDistance + " chunks for player: " + player.getName());
    }
}
