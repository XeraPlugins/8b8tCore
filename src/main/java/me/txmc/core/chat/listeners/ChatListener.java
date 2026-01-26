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
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<UUID, List<String>> playerMessages = new ConcurrentHashMap<>();
    private final Map<String, Component> prefixCache = new ConcurrentHashMap<>();
    private static final double SIMILARITY_THRESHOLD = 0.8;
    private static final int MESSAGE_HISTORY = 3;

    private static final Set<String> KEYWORDS = Set.of("@here", "@everyone", "here", "everyone");

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
        UUID senderUUID = sender.getUniqueId();
        String senderName = sender.getName();
        ChatInfo ci = manager.getInfo(sender);

        if (ci == null) return;

        int cooldown = manager.getConfig().getInt("Cooldown");
        if (ci.isChatLock() && !sender.isOp() && !sender.hasPermission("*")) {
            sendPrefixedLocalizedMessage(sender, "chat_cooldown", cooldown);
            return;
        }

        ci.setChatLock(true);
        service.schedule(() -> ci.setChatLock(false), cooldown, TimeUnit.SECONDS);

        String ogMessage = GlobalUtils.getStringContent(event.message());
        String filteredMessage = filterLongWords(ogMessage);

        List<String> messages = playerMessages.computeIfAbsent(senderUUID, k -> new ArrayList<>());
        synchronized (messages) {
            if (!sender.isOp() && !sender.hasPermission("*")) {
                for (String oldMessage : messages) {
                    if (ogMessage.length() < 20) continue;
                    if (isSimilar(filterLongWords(oldMessage), filteredMessage)) {
                        sendPrefixedLocalizedMessage(sender, "spam_alert");
                        return;
                    }
                }
            }
            if (messages.size() >= MESSAGE_HISTORY) messages.remove(0);
            messages.add(ogMessage);
        }

        if (blockedCheck(ogMessage) || ci.getMutedUntil() > Instant.now().getEpochSecond() || domainCheck(ogMessage)) {
            Component message = formatMessage(ogMessage, sender.displayName(), sender, null, ci);
            if (message != null) sender.sendMessage(message);
            if (!blockedCheck(ogMessage) && !domainCheck(ogMessage)) return;
            log(Level.INFO, "&3Prevented&r&a %s&r&3 from sending a message (banned words/link)", senderName);
            return;
        }

        Component displayName = sender.displayName();
        Component preparedMessage = formatMessage(ogMessage, displayName, sender, null, ci);
        if (preparedMessage == null) return;

        Bukkit.getLogger().info(GlobalUtils.getStringContent(preparedMessage));

        Set<String> onlinePlayerNames = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) onlinePlayerNames.add(p.getName().toLowerCase());

        String lowerMessage = ogMessage.toLowerCase();
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            ChatInfo info = manager.getInfo(recipient);
            if (info == null || info.isIgnoring(senderUUID) || info.isToggledChat()) continue;

            if (lowerMessage.contains(recipient.getName().toLowerCase())) {
                Component highlighted = formatMessageSimple(ogMessage, displayName, senderName, recipient.getName(), ci);
                if (highlighted != null) recipient.sendMessage(highlighted);
            } else {
                recipient.sendMessage(preparedMessage);
            }
        }
    }

    private boolean isSimilar(String m1, String m2) {
        int l1 = m1.length();
        int l2 = m2.length();
        if (Math.abs(l1 - l2) > Math.max(l1, l2) * (1.0 - SIMILARITY_THRESHOLD)) return false;
        return similarityPercentage(m1, m2) >= SIMILARITY_THRESHOLD;
    }

    private TextComponent formatMessageSimple(String message, Component displayName, String senderName, String mentionedPlayerName, ChatInfo ci) {
        Component prefixComponent = prefixCache.computeIfAbsent(prefixManager.getPrefix(ci), miniMessage::deserialize);
        Component nameComponent = prefixComponent
                .append(Component.text("<").color(TextColor.color(170, 170, 170)))
                .append(displayName.clickEvent(ClickEvent.suggestCommand("/msg " + senderName + " ")))
                .append(Component.text("> ").color(TextColor.color(170, 170, 170)));

        if (message.trim().isEmpty()) return null;
        TextColor color = message.startsWith(">") ? NamedTextColor.GREEN : NamedTextColor.WHITE;

        Component msgComponent = Component.text("");
        String[] words = message.split(" ");
        for (String word : words) {
            TextColor wordColor = color;
            if (word.equalsIgnoreCase(mentionedPlayerName) || KEYWORDS.contains(word.toLowerCase())) {
                wordColor = NamedTextColor.YELLOW;
            }
            msgComponent = msgComponent.append(Component.text(word + " ").color(wordColor));
        }

        return (TextComponent) nameComponent.append(msgComponent);
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
        int n = str1.length();
        int m = str2.length();
        if (n < m) return levenshteinDistance(str2, str1);
        if (m == 0) return n;
        if (m <= 64) return myersDistance(str1, str2);

        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) curr[j] = j;

        for (int i = 1; i <= n; i++) {
            int prev = curr[0];
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int temp = curr[j];
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    curr[j] = prev;
                } else {
                    curr[j] = Math.min(Math.min(curr[j - 1], curr[j]), prev) + 1;
                }
                prev = temp;
            }
        }
        return curr[m];
    }

    private int myersDistance(String str1, String str2) {
        int n = str1.length();
        int m = str2.length();
        long peq[] = new long[256];
        for (int i = 0; i < m; i++) peq[str2.charAt(i) & 0xFF] |= (1L << i);
        long pv = -1L, mv = 0;
        int dist = m;
        for (int i = 0; i < n; i++) {
            long eq = peq[str1.charAt(i) & 0xFF];
            long xv = eq | mv;
            long xh = (((eq & pv) + pv) ^ pv) | eq;
            long ph = mv | ~(xh | pv);
            long mh = pv & xh;
            if ((ph & (1L << (m - 1))) != 0) dist++;
            if ((mh & (1L << (m - 1))) != 0) dist--;
            ph = (ph << 1) | 1L;
            mh = (mh << 1);
            pv = mh | ~(ph | xv);
            mv = ph & xv;
        }
        return dist;
    }

    public TextComponent formatMessage(String message, Component displayName, Player player, String mentionedPlayerName, ChatInfo ci) {
        if (message.trim().isEmpty()) return null;

        Component hoverText = Component.text()
                .append(player.displayName())
                .append(Component.text("\n\n", NamedTextColor.GRAY))
                .append(Component.text("Lang: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(player.locale().getLanguage() + "\n", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("Experience: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(player.getLevel() + "\n", NamedTextColor.GREEN))
                .append(Component.text("Distance Walked: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(String.format("%.2f", (player.getStatistic(Statistic.WALK_ONE_CM) + player.getStatistic(Statistic.SPRINT_ONE_CM)) / 100000.0) + " km\n", NamedTextColor.BLUE))
                .append(Component.text("Player Kills: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(player.getStatistic(Statistic.PLAYER_KILLS) + "\n", NamedTextColor.RED))
                .append(Component.text("Player Deaths: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(player.getStatistic(Statistic.DEATHS) + "\n", NamedTextColor.RED))
                .append(Component.text("Time Played: ").color(TextColor.fromHexString("#FFD700")))
                .append(Component.text(String.format("%.2f", player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 3600.0) + " hours\n", NamedTextColor.YELLOW))
                .append(Component.text("\n(Click to send a direct message)").color(NamedTextColor.GRAY))
                .build();

        Component prefixComponent = prefixCache.computeIfAbsent(prefixManager.getPrefix(ci), miniMessage::deserialize);
        Component nameComponent = prefixComponent
                .append(Component.text("<").color(TextColor.color(170, 170, 170)))
                .append(displayName.hoverEvent(HoverEvent.showText(hoverText)).clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " ")))
                .append(Component.text("> ").color(TextColor.color(170, 170, 170)));

        TextColor color = message.startsWith(">") ? NamedTextColor.GREEN : NamedTextColor.WHITE;
        Component msgComponent = Component.text("");
        String[] words = message.split(" ");

        Set<String> onlinePlayerNames = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) onlinePlayerNames.add(p.getName().toLowerCase());

        for (String word : words) {
            String wordLower = word.toLowerCase();
            if (onlinePlayerNames.contains(wordLower)) {
                msgComponent = msgComponent.append(Component.text(word + " ").color(wordLower.equals(mentionedPlayerName != null ? mentionedPlayerName.toLowerCase() : null) ? NamedTextColor.YELLOW : color));
            } else if (KEYWORDS.contains(wordLower)) {
                msgComponent = msgComponent.append(Component.text(word + " ").color(NamedTextColor.YELLOW));
            } else {
                msgComponent = msgComponent.append(Component.text(word + " ").color(color));
            }
        }
        return (TextComponent) nameComponent.append(msgComponent);
    }
}
