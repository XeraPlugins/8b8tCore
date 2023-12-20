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
            Set<PermissionAttachmentInfo> perms = player.getEffectivePermissions();
            List<Integer> maxL = perms.stream().map(PermissionAttachmentInfo::getPermission).filter(p -> p.startsWith("8b8tcore.home.max.")).map(s -> Integer.parseInt(s.substring(s.lastIndexOf('.') + 1))).toList();
            int maxHomes = (!maxL.isEmpty()) ? Collections.max(maxL) : main.config().getInt("MaxHomes");
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
            Home home = new Home(args[0], player.getLocation());
            homes.addHome(home);
            sendPrefixedLocalizedMessage(player, "sethome_success", home.getName());
        } else sendMessage(sender, "&3You must be a player to use this command");
        return true;
    }
}

