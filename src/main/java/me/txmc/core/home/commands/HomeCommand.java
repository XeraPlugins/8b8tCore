package me.txmc.core.home.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.home.Home;
import me.txmc.core.home.HomeData;
import me.txmc.core.home.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class HomeCommand implements TabExecutor {
    private final HomeManager main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            int radius = main.config().getInt("Radius");
            HomeData homeData = main.homes().get(player);
            if (homeData.hasHomes()) {
                sendPrefixedLocalizedMessage(player, "home_no_homes");
                return true;
            }
            String names = String.join(", ", homeData.stream().map(Home::getName).toArray(String[]::new));
            if (args.length < 1) {
                sendPrefixedLocalizedMessage(player, "home_specify_home", names);
                return true;
            }
            if (isSpawn(player, radius)) {
                sendPrefixedLocalizedMessage(player, "home_too_close", radius);
                return true;
            }
            boolean teleported = false;
            for (Home home : homeData.getHomes()) {
                if (home.getName().equalsIgnoreCase(args[0])) {
                    vanish(player);
                    player.teleportAsync(home.getLocation());
                    unVanish(player);
                    sendPrefixedLocalizedMessage(player, "home_success", home.getName());
                    teleported = true;
                    break;
                }
            }
            if (!teleported) sendPrefixedLocalizedMessage(player, "home_not_found", args[0]);
        } else sendMessage(sender, "&3You must be a player to use this command");
        return true;
    }

    private boolean isSpawn(Player player, int range) {
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