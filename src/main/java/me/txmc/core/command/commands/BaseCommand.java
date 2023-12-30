package me.txmc.core.command.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * @author 254n_m
 * @since 2023/12/29 11:16 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class BaseCommand implements CommandExecutor {
    private final Main plugin;
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!sender.hasPermission("8b8tcore.admin")) return true;
        if (args.length > 0) {
            switch (args[0]) {
                case "reload":
                    plugin.reloadConfig();
                    GlobalUtils.sendOptionalPrefixMessage(sender, "&3Successfully reloaded the configuration.", true);
            }
        } else GlobalUtils.sendMessage(sender, "base_command_help");
        return true;
    }
}
