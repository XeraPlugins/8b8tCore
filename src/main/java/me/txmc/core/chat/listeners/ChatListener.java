package me.txmc.core.chat.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final ChatSection manager;
    private final HashSet<String> tlds;
    private ScheduledExecutorService service;
    private final PrefixManager prefixManager = new PrefixManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (service == null) service = manager.getPlugin().getExecutorService();
        event.setCancelled(true);
        Player sender = event.getPlayer();
        int cooldown = manager.getConfig().getInt("Cooldown");
        ChatInfo ci = manager.getInfo(sender);
        if (ci.isChatLock()) {
            sendPrefixedLocalizedMessage(sender, "chat_cooldown", cooldown);
            return;
        }
        ci.setChatLock(true);
        service.schedule(() -> ci.setChatLock(false), cooldown, TimeUnit.SECONDS);

        String ogMessage = event.getMessage();
        Component displayName = sender.displayName();
        Component message = formatMessage(ogMessage, displayName, sender, false, null);

        String legacyPrefix = LegacyComponentSerializer.legacySection().serialize(miniMessage.deserialize(prefixManager.getPrefix(sender)));
        String legacyDisplayName = LegacyComponentSerializer.legacySection().serialize(displayName);
        sender.setPlayerListName(legacyPrefix + legacyDisplayName);

        if (blockedCheck(ogMessage)) {
            sender.sendMessage(message);
            log(Level.INFO, "&3Prevented&r&a %s&r&3 from sending a message that has banned words", sender.getName());
            return;
        }
        if (domainCheck(ogMessage)) {
            sender.sendMessage(message);
            log(Level.INFO, "&3Prevented player&r&a %s&r&3 from sending a link / server ip", sender.getName());
            return;
        }

        Bukkit.getLogger().info(GlobalUtils.getStringContent(message));

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            ChatInfo info = manager.getInfo(recipient);
            if (info == null || info.isIgnoring(sender.getUniqueId()) || info.isToggledChat()) continue;

            String lowerMessage = ogMessage.toLowerCase();

            if (lowerMessage.contains(recipient.getName().toLowerCase()) || lowerMessage.contains("here")) {
                TextComponent highlightedMessage = formatMessage(ogMessage, displayName, sender, true, recipient.getName());
                recipient.sendMessage(highlightedMessage);
            } else {
                recipient.sendMessage(message);
            }
        }
    }

    private boolean domainCheck(String message) {
        if (!manager.getConfig().getBoolean("PreventLinks")) return false;
        message = message.toLowerCase().replace("dot", ".").replace("d0t", ".");
        if (message.indexOf('.') == -1) return false;
        String[] split = message.trim().split("\\.");
        if (split.length == 2) {
            String possibleTLD = split[1];
            if (possibleTLD.contains("/")) possibleTLD = possibleTLD.substring(0, possibleTLD.indexOf("/"));
            return tlds.contains(possibleTLD);
        } else {
            for (String word : split) {
                if (word.contains("/")) word = word.substring(0, word.indexOf("/"));
                if (tlds.contains(word)) return true;
            }
        }
        return false;
    }

    private boolean blockedCheck(String message) {
        List<String> blocked = manager.getConfig().getStringList("Blocked");
        for (String blockedWord : blocked) {
            if (message.toLowerCase().contains(blockedWord.toLowerCase())) return true;
        }
        return false;
    }

    public TextComponent formatMessage(String message, Component displayName, Player player, boolean highlightMentioned, String mentionedPlayerName) {
        String prefix = prefixManager.getPrefix(player);
        Component prefixComponent = miniMessage.deserialize(prefix);

        Component nameComponent = prefixComponent
                .append(Component.text("<").color(TextColor.color(170, 170, 170)))
                .append(displayName)
                .append(Component.text("> ").color(TextColor.color(170, 170, 170)));

        if (highlightMentioned && mentionedPlayerName != null) {
            String resetColor = message.startsWith(">") ? ChatColor.GREEN.toString() : ChatColor.RESET.toString();

            message = message
                    .replaceAll("(?i)" + mentionedPlayerName, ChatColor.YELLOW + mentionedPlayerName + resetColor)
                    .replaceAll("(?i)\\b@?here\\b", ChatColor.YELLOW + "$0" + resetColor);
        }

        Component msgComponent = message.startsWith(">")
                ? Component.text(message.substring(1).trim()).color(TextColor.color(85, 255, 85))
                : Component.text(message);

        return (TextComponent) nameComponent.append(msgComponent);
    }
}
