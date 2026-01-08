package me.txmc.core.chat;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class ChatInfo {
    private final Player player;
    private final HashSet<UUID> ignoring;
    private final ChatSection manager;
    private Player replyTarget;
    private boolean toggledChat;
    private boolean joinMessages;
    private boolean chatLock;
    private long mutedUntil;
    private boolean hidePrefix;
    private String selectedRank;
    private String customGradient;
    private String prefixAnimation;
    private int prefixSpeed;
    private String prefixDecorations;
    private String nickname;
    private boolean useVanillaLeaderboard;
    private String nameGradient;
    private String nameAnimation;
    private int nameSpeed;
    private String nameDecorations;
    private boolean hideAnnouncements;

    public ChatInfo(Player player, ChatSection manager, HashSet<UUID> ignoring, boolean toggledChat, boolean joinMessages) {
        this.player = player;
        this.manager = manager;
        this.ignoring = ignoring;
        this.toggledChat = toggledChat;
        this.joinMessages = joinMessages;
    }

    public ChatInfo(Player player, ChatSection manager) {
        this.player = player;
        this.manager = manager;
        this.ignoring = new HashSet<>();
    }


    public boolean isIgnoring(UUID player) {
        return ignoring.contains(player);
    }

    public void ignorePlayer(UUID player) {
        ignoring.add(player);
    }

    public void unignorePlayer(UUID player) {
        ignoring.remove(player);
    }

    public Set<UUID> getIgnoring() {
        return ignoring;
    }

    public boolean shouldNotSave() {
        return ignoring.isEmpty() && !toggledChat && !joinMessages;
    }

    public void saveChatInfo() {
        manager.getChatInfoStore().save(this, player);
    }

    public net.kyori.adventure.text.Component getDisplayNameComponent() {
        return me.txmc.core.util.GlobalUtils.parseDisplayName(player.getName(), nickname, nameGradient, nameAnimation, nameSpeed, nameDecorations);
    }
}
