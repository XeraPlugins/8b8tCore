package me.txmc.core.home.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.home.Home;
import me.txmc.core.home.HomeData;
import me.txmc.core.home.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class DelHomeCommand implements TabExecutor {
    private final HomeManager main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            HomeData homes = main.homes().get(player);
            if (!homes.hasHomes()) {
                sendPrefixedLocalizedMessage(player, "delhome_no_homes");
                return true;
            }
            if (args.length < 1) {
                String names = String.join(", ", homes.stream().map(Home::getName).toArray(String[]::new));
                sendPrefixedLocalizedMessage(player, "delhome_specify_home", names);
                return true;
            }
            Home home = homes.stream().filter(h -> h.getName().equals(args[0])).findFirst().orElse(null);
            if (home == null) {
                sendPrefixedLocalizedMessage(player, "delhome_home_not_found", args[0]);
                return true;
            }
            homes.deleteHome(home);
            sendPrefixedLocalizedMessage(player, "delhome_success", home.getName());
        } else sendMessage(sender, "&3You must be a player to use this command");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return main.tabComplete(sender, args);
    }
}
