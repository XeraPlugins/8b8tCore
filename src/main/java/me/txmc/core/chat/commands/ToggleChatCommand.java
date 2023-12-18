package me.txmc.core.chat.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor

public class ToggleChatCommand implements CommandExecutor {
    private final ChatSection manager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            ChatInfo info = manager.getInfo(player);
            if (info.isToggledChat()) {
                sendPrefixedLocalizedMessage(player, "togglechat_chat_enabled");
            } else sendPrefixedLocalizedMessage(player, "togglechat_chat_disabled");
            info.setToggledChat(!info.isToggledChat());
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }
}
