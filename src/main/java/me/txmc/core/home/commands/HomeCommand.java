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
import org.bukkit.entity.Player;
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
            if (args.length < 1) {
                sendPrefixedLocalizedMessage(player, "home_specify_home", names);
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
                player.teleportAsync(home.getLocation());
                unVanish(player);
                sendPrefixedLocalizedMessage(player, "home_success", home.getName());
                homeFound = true;
                break;
            }
            if (!homeFound) sendPrefixedLocalizedMessage(player, "home_not_found", args[0]);
        } else {
            sendMessage(sender, "&3You must be a player to use this command");
        }
        return true;
    }

    private int getMaxDistanceFromSpawn(Player player) {
        Map<String, Integer> distanceMap = Map.of(
                "home.spawn.donator5", 2000,
                "home.spawn.donator4", 4000,
                "home.spawn.donator3", 6000,
                "home.spawn.donator2", 8000,
                "home.spawn.donator1", 10000,
                "home.spawn.voter", 15000
        );

        int maxDistance = 20000;

        for (Map.Entry<String, Integer> entry : distanceMap.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                maxDistance = Math.min(maxDistance, entry.getValue());
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
