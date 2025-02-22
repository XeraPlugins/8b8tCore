package me.txmc.core.chat;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
                //MiniMessage miniMessage = MiniMessage.miniMessage();
                //msg = miniMessage.stripTags(msg);
                msg = sanitizeMessage(msg);
                sendLocalizedAmpersandMessage(target, "whisper_from", false, player.getName(), msg);
                sendLocalizedAmpersandMessage(player, "whisper_to", false, target.getName(), msg);
            } else sendLocalizedMessage(player, "whisper_ignoring", false, target.getName());
        } else sendLocalizedMessage(player, "whisper_you_are_ignoring", false);
    }

    private String sanitizeMessage(String msg) {
        msg = msg.replaceAll("(?i)<(?!black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|gradient|bold|italic|underlined|strikethrough|obfuscated|reset|st|u|i|b|r)[a-zA-Z]+>", "");
        return msg;
    }
}