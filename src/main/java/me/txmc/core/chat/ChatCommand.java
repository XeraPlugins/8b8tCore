package me.txmc.core.chat;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendLocalizedMessage;

public abstract class ChatCommand implements CommandExecutor {
    public void sendWhisper(Player player, ChatInfo senderInfo, Player target, ChatInfo targetInfo, String msg) {
        if (!senderInfo.isIgnoring(target.getUniqueId())) {
            if (!targetInfo.isIgnoring(player.getUniqueId())) {
                targetInfo.setReplyTarget(player);
                senderInfo.setReplyTarget(target);

                msg = MiniMessage.miniMessage().stripTags(msg);

                sendLocalizedMessage(target, "whisper_from", false, "<click:suggest_command:'/msg "+ player.getName() +" '>" + player.getName() + "</click>", msg);
                sendLocalizedMessage(player, "whisper_to", false, "<click:suggest_command:'/msg "+ player.getName() +" '>" + player.getName() + "</click>", msg);
            } else {
                sendLocalizedMessage(player, "whisper_ignoring", false, target.getName());
            }
        } else {
            sendLocalizedMessage(player, "whisper_you_are_ignoring", false);
        }
    }
}
