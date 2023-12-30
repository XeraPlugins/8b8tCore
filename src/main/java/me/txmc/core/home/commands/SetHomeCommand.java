package me.txmc.core.home.commands;

import lombok.AllArgsConstructor;
import me.txmc.core.home.Home;
import me.txmc.core.home.HomeData;
import me.txmc.core.home.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@AllArgsConstructor
public class SetHomeCommand implements CommandExecutor {
    private final HomeManager main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 1) {
                sendPrefixedLocalizedMessage(player, "sethome_include_name");
                return true;
            }
            int maxHomes = main.getMaxHomes(player);
            HomeData homes = main.homes().get(player);
            if (homes.stream().anyMatch(h -> h.getName().equals(args[0]))) {
                Home home = homes.stream().filter(h -> h.getName().equals(args[0])).findAny().get();
                sendPrefixedLocalizedMessage(player, "sethome_home_already_exists", home.getName());
                return true;
            }
            if (homes.getHomes().size() >= maxHomes && !player.isOp()) {
                sendPrefixedLocalizedMessage(player, "sethome_max_reached");
                return true;
            }
            homes.addHome(new Home(args[0], player.getLocation()));
            sendPrefixedLocalizedMessage(player, "sethome_success", args[0]);
        } else sendMessage(sender, "&3You must be a player to use this command");
        return true;
    }
}

