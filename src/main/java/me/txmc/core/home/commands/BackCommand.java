package me.txmc.core.home.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.home.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Handles the /back command, which allows players to teleport to their last known location.
 *
 * <p>This command is part of the home system in the 8b8tCore plugin, providing functionality for players to return
 * to the last location they teleported from.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Retrieving the last saved location for the player</li>
 *     <li>Handling cases where no last location is available for the player</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/09/09 9:27 PM
 */

@RequiredArgsConstructor
public class BackCommand implements CommandExecutor {

    private final HomeManager main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {

            int maxDistanceFromSpawn = getMaxDistanceFromSpawn(player);
            if(player.getWorld().getEnvironment() == World.Environment.NETHER){
                maxDistanceFromSpawn = getMaxDistanceFromSpawn(player) / 8;
            }
            if (isWithinRestrictedArea(player, maxDistanceFromSpawn)) {
                sendPrefixedLocalizedMessage(player, "home_too_close", maxDistanceFromSpawn);
                return true;
            }

            Location lastLocation = main.plugin().getLastLocation(player);

            if (lastLocation == null) {
                sendPrefixedLocalizedMessage(player, "back_no_last_location");
                return true;
            }

            lastLocation.getWorld().getChunkAtAsyncUrgently(lastLocation.getBlock()).thenAccept(chunk -> {
                if (player.isOnline()) {
                    player.teleportAsync(lastLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    sendPrefixedLocalizedMessage(player, "back_teleported");
                    main.plugin().lastLocations.remove(player.getUniqueId());
                }
            });
            main.plugin().lastLocations.remove(player.getUniqueId());
        } else {
            sender.sendMessage("Only players can use this command.");
        }
        return true;
    }

    private int getMaxDistanceFromSpawn(Player player) {
        FileConfiguration cfg = main.plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("TPAHOMERADIUS");
        // read the configured default (no hard-coded fallback)
        int maxDistance = sec.getInt("default");

        // scan for any home.spawn.<n> permission on the player
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith("tpa.spawn.")) {
                try {
                    int dist = Integer.parseInt(perm.substring("tpa.spawn.".length()));
                    maxDistance = Math.min(maxDistance, dist);
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxDistance;
    }

    private boolean isWithinRestrictedArea(Player player, int range) {
        if (player.isOp()) return false;
        Location loc = player.getLocation();
        return loc.getBlockX() < range && loc.getBlockX() > -range && loc.getBlockZ() < range && loc.getBlockZ() > -range;
    }
}
