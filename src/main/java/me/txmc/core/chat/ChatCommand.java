package me.txmc.core.chat;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendLocalizedAmpersandMessage;
import static me.txmc.core.util.GlobalUtils.sendLocalizedMessage;
public abstract class ChatCommand implements CommandExecutor {
    public void sendWhisper(Player player, ChatInfo senderInfo, Player target, ChatInfo targetInfo, String msg) {
        if (!senderInfo.isIgnoring(target.getUniqueId())) {
            if (!targetInfo.isIgnoring(player.getUniqueId())) {
                targetInfo.setReplyTarget(player);
                senderInfo.setReplyTarget(target);
                msg = ChatColor.stripColor(msg);
                sendLocalizedAmpersandMessage(target, "whisper_from", false, player.getName(), msg);
                sendLocalizedAmpersandMessage(player, "whisper_to", false, target.getName(), msg);
            } else sendLocalizedMessage(player, "whisper_ignoring", false, target.getName());
        } else sendLocalizedMessage(player, "whisper_you_are_ignoring", false);
    }
}