package me.txmc.core.chat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
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
    public void onChat(AsyncChatEvent event) {
        if (service == null) service = manager.getPlugin().getExecutorService();
        event.setCancelled(true);
        Player sender = event.getPlayer();
        int cooldown = manager.getConfig().getInt("Cooldown");
        ChatInfo ci = manager.getInfo(sender);
        if (ci.isChatLock() && !sender.isOp()) {
            sendPrefixedLocalizedMessage(sender, "chat_cooldown", cooldown);
            return;
        }
        ci.setChatLock(true);
        service.schedule(() -> ci.setChatLock(false), cooldown, TimeUnit.SECONDS);

        String ogMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component displayName = sender.displayName();
        Component message = formatMessage(ogMessage, displayName, sender, null);
        if(message == null) return;

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

            if (lowerMessage.contains(recipient.getName().toLowerCase())) {
                TextComponent highlightedMessage = formatMessage(ogMessage, displayName, sender, recipient.getName());
                if(highlightedMessage == null) return;

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

    public TextComponent formatMessage(String message, Component displayName, Player player, String mentionedPlayerName) {

        int exp = player.getLevel();
        String lang = player.locale().getLanguage();
        int distanceWalked = player.getStatistic(Statistic.WALK_ONE_CM) + player.getStatistic(Statistic.SPRINT_ONE_CM);
        int playerKills = player.getStatistic(Statistic.PLAYER_KILLS);
        int playerDeaths = player.getStatistic(Statistic.DEATHS);
        int timePlayedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);

        double distanceWalkedKm = distanceWalked / 100000.0;
        double timePlayedHours = timePlayedTicks / 20.0 / 3600.0;

        DecimalFormat df = new DecimalFormat("#.##");
        String distanceWalkedFormatted = df.format(distanceWalkedKm);
        String timePlayedFormatted = df.format(timePlayedHours);

        Component hoverText = Component.text()
                .append(player.displayName())
                .append(Component.text("\n\n", NamedTextColor.GRAY))
                .append(Component.text("Lang: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(lang + "\n", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("Experience: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(exp + "\n", NamedTextColor.GREEN))
                .append(Component.text("Distance Walked: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(distanceWalkedFormatted + " km\n", NamedTextColor.BLUE))
                .append(Component.text("Player Kills: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(playerKills + "\n", NamedTextColor.RED))
                .append(Component.text("Player Deaths: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(playerDeaths + "\n", NamedTextColor.RED))
                .append(Component.text("Time Played: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(timePlayedFormatted + " hours\n", NamedTextColor.YELLOW))
                .append(Component.text("\n(Click to send a direct message)").color(NamedTextColor.GRAY))
                .build();

        String prefix = prefixManager.getPrefix(player);
        Component prefixComponent = miniMessage.deserialize(prefix);

        Component nameComponent = prefixComponent
                .append(Component.text("<").color(TextColor.color(170, 170, 170)))
                .append(displayName
                        .hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " ")))
                .append(Component.text("> ").color(TextColor.color(170, 170, 170)));

        String resetColor = message.startsWith(">") ? ChatColor.GREEN.toString() : ChatColor.RESET.toString();
        message = message.replaceAll("(?i)\\b@?here\\b", ChatColor.YELLOW + "$0" + resetColor);
        message = message.replaceAll("(?i)\\b@?everyone\\b", ChatColor.YELLOW + "$0" + resetColor);

        NamedTextColor colorMessage = NamedTextColor.WHITE;

        if(message.startsWith(">")){
            message = message.substring(1).trim();
            colorMessage = NamedTextColor.GREEN;
        }

        if (message.trim().isEmpty()) return null;

        Component msgComponent = Component.text("");
        String[] words = message.split(" ");

        for (String word : words) {
            boolean isPlayerName = false;

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (word.equalsIgnoreCase(onlinePlayer.getName())) {
                    Component mentionedNameComponent = onlinePlayer.name();

                    mentionedNameComponent = mentionedNameComponent.color(colorMessage);
                    if(mentionedPlayerName != null){
                        if(onlinePlayer.getName().equals(mentionedPlayerName)){
                            mentionedNameComponent = mentionedNameComponent.color(NamedTextColor.YELLOW);
                        }
                    }

                    msgComponent = msgComponent.append(mentionedNameComponent).append(Component.text(" "));
                    isPlayerName = true;
                    break;
                }
            }

            if (!isPlayerName) {
                if(colorMessage != NamedTextColor.WHITE){
                    msgComponent = msgComponent.append(Component.text(word + " ").color(colorMessage));
                } else msgComponent = msgComponent.append(Component.text(word + " "));
            }
        }

        return (TextComponent) nameComponent.append(msgComponent);
    }
}
