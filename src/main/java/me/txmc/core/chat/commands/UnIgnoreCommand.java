package me.txmc.core.chat.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class UnIgnoreCommand implements CommandExecutor {
    private final ChatSection manager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String s, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                ChatInfo info = manager.getInfo(player);
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                if (info.isIgnoring(target.getUniqueId())) {
                    info.unignorePlayer(target.getUniqueId());
                    sendPrefixedLocalizedMessage(player, "unignore_successful", target.getName());
                } else sendPrefixedLocalizedMessage(player, "unignore_not_ignoring", target.getName());
            } else sendPrefixedLocalizedMessage(player, "unignore_command_syntax");
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }
}
