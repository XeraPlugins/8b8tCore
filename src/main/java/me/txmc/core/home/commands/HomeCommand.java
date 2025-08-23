package me.txmc.core.home.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.home.Home;
import me.txmc.core.home.HomeData;
import me.txmc.core.home.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class HomeCommand implements TabExecutor {
    private final HomeManager main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            HomeData homes = main.homes().get(player);
            if (!homes.hasHomes()) {
                sendPrefixedLocalizedMessage(player, "home_no_homes");
                return true;
            }
            String names = String.join(", ", homes.stream().map(Home::getName).toArray(String[]::new));
            String homesCount = String.valueOf(homes.stream().count());
            if (args.length < 1) {
                sendPrefixedLocalizedMessage(player, "home_specify_home", homesCount, names);
                return true;
            }

            int maxDistanceFromSpawn = getMaxDistanceFromSpawn(player);
            if(player.getWorld().getEnvironment() == World.Environment.NETHER){
                maxDistanceFromSpawn = getMaxDistanceFromSpawn(player) / 8;
            }
            if (isWithinRestrictedArea(player, maxDistanceFromSpawn)) {
                sendPrefixedLocalizedMessage(player, "home_too_close", maxDistanceFromSpawn);
                return true;
            }

            boolean homeFound = false;
            for (Home home : homes.getHomes()) {
                if (!home.getName().equals(args[0])) continue;
                vanish(player);
                main.plugin.lastLocations.put(player, player.getLocation());
                
                Location targetLoc = home.getLocation();
                final String homeName = home.getName();
                targetLoc.getWorld().getChunkAtAsyncUrgently(targetLoc.getBlock()).thenAccept(chunk -> {
                    if (player.isOnline()) {
                        player.teleportAsync(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                        if(!main.plugin.getVanishedPlayers().contains(player.getUniqueId())) unVanish(player);
                        sendPrefixedLocalizedMessage(player, "home_success", homeName);
                    }
                });
                homeFound = true;
                break;
            }
            if (!homeFound) sendPrefixedLocalizedMessage(player, "home_not_found", args[0]);
        } else {
            sendMessage(sender, "&3You must be a player to use this command");
        }
        return true;
    }

// In HomeCommand.java

    private int getMaxDistanceFromSpawn(Player player) {
        FileConfiguration cfg = main.plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("TPAHOMERADIUS");
        // read the configured default (no hard-coded fallback)
        int maxDistance = sec.getInt("default");

        // scan for any home.spawn.<n> permission on the player
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith("home.spawn.")) {
                try {
                    int dist = Integer.parseInt(perm.substring("home.spawn.".length()));
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

    private void vanish(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.hidePlayer(main.plugin(), player);
            }
        }
    }

    public void unVanish(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.showPlayer(main.plugin(), player);
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return main.tabComplete(sender, args);
    }
}
