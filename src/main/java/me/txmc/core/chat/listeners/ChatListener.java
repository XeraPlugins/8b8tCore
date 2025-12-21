package me.txmc.core.chat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
import java.util.*;
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
    private final GeneralDatabase database;

    private final Map<UUID, LinkedList<String>> playerMessages = new HashMap<>();
    private static final double SIMILARITY_THRESHOLD = 0.8;
    private static final int MESSAGE_HISTORY = 3;

    public ChatListener(ChatSection manager, HashSet<String> tlds) {
        this.manager = manager;
        this.tlds = tlds;
        this.database = GeneralDatabase.getInstance();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        if (service == null) service = manager.getPlugin().getExecutorService();
        event.setCancelled(true);
        Player sender = event.getPlayer();
        int cooldown = manager.getConfig().getInt("Cooldown");
        ChatInfo ci = manager.getInfo(sender);
        if (ci == null) {
            log(Level.WARNING, "ChatInfo is null for player %s", sender.getName());
            return;
        }
        if (ci.isChatLock() && !sender.isOp() && !sender.hasPermission("*")) {
            sendPrefixedLocalizedMessage(sender, "chat_cooldown", cooldown);
            return;
        }
        ci.setChatLock(true);
        service.schedule(() -> ci.setChatLock(false), cooldown, TimeUnit.SECONDS);

        String ogMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        LinkedList<String> messages = playerMessages.computeIfAbsent(sender.getUniqueId(), k -> new LinkedList<>());

        if(!sender.isOp() && !sender.hasPermission("*")){
            for (String oldMessage : messages) {

                if(ogMessage.length() < 20) continue;

                if (similarityPercentage(filterLongWords(oldMessage), filterLongWords(ogMessage)) >= SIMILARITY_THRESHOLD) {
                    sendPrefixedLocalizedMessage(sender, "spam_alert");
                    return;
                }
            }
        }

        if (messages.size() >= MESSAGE_HISTORY) {
            messages.removeFirst();
        }
        messages.addLast(ogMessage);

        Component displayName = sender.displayName();
        Component message = formatMessage(ogMessage, displayName, sender, null);
        if(message == null) return;

        if (blockedCheck(ogMessage)) {
            sender.sendMessage(message);
            log(Level.INFO, "&3Prevented&r&a %s&r&3 from sending a message that has banned words", sender.getName());
            return;
        }

        if (database.isMuted(sender.getName())) {
            sender.sendMessage(message);
            return;
        }

        if (domainCheck(ogMessage)) {
            sender.sendMessage(message);
            log(Level.INFO, "&3Prevented player&r&a %s&r&3 from sending a link / server ip", sender.getName());
            return;
        }

        Bukkit.getLogger().info(GlobalUtils.getStringContent(message));

        Bukkit.getGlobalRegionScheduler().runDelayed(manager.getPlugin(), (task) -> {
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
        }, 1L);
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

    private double similarityPercentage(String str1, String str2) {
        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(str1, str2);
        return 1.0 - ((double) distance / maxLength);
    }

    public static String filterLongWords(String message) {
        String[] words = message.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() <= 12) {
                result.append(word).append(" ");
            }
        }

        return result.toString().trim();
    }

    private int levenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            for (int j = 0; j <= len2; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
        }
        return dp[len1][len2];
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

        boolean isGreentext = message.startsWith(">");
        NamedTextColor keywordColor = NamedTextColor.YELLOW;
        NamedTextColor normalColor = isGreentext ? NamedTextColor.GREEN : NamedTextColor.WHITE;

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
                Component wordComp = Component.text(word + " ");
                if (word.matches("(?i)@?here|@?everyone")) {
                    wordComp = wordComp.color(NamedTextColor.YELLOW);
                } else if (normalColor != NamedTextColor.WHITE) {
                    wordComp = wordComp.color(normalColor);
                }
                msgComponent = msgComponent.append(wordComp);
            }
        }

        return (TextComponent) nameComponent.append(msgComponent);
    }
}
