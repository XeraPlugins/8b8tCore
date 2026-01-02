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
        String baseName;
        if (nickname == null || nickname.isEmpty() || nickname.equals(player.getName())) {
            baseName = player.getName();
        } else {
            baseName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(me.txmc.core.util.GlobalUtils.convertToMiniMessageFormat(nickname))).trim();
        }

        if (nameGradient == null || nameGradient.isEmpty()) {
            return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(me.txmc.core.util.GlobalUtils.convertToMiniMessageFormat(baseName));
        }

        boolean isGradient = nameGradient.contains(":") &&
                nameGradient.indexOf('#') != nameGradient.lastIndexOf('#');

        String finalGradient;
        long tick = me.txmc.core.util.GradientAnimator.getAnimationTick();
        if (isGradient) {
            finalGradient = me.txmc.core.util.GradientAnimator.applyAnimation(nameGradient, nameAnimation != null ? nameAnimation : "none", nameSpeed, tick);
        } else {
            finalGradient = nameGradient;
            isGradient = false;
        }

        if (isGradient && finalGradient.indexOf('#') == finalGradient.lastIndexOf('#')) {
            isGradient = false;
        }

        StringBuilder result = new StringBuilder();

        if (nameDecorations != null && !nameDecorations.isEmpty()) {
            for (String decoration : nameDecorations.split(",")) {
                result.append("<").append(decoration.trim()).append(">");
            }
        }

        if (isGradient) {
            result.append("<gradient:").append(finalGradient).append(">")
                    .append(baseName)
                    .append("</gradient>");
        } else {
            result.append("<color:").append(finalGradient).append(">")
                    .append(baseName)
                    .append("</color>");
        }

        if (nameDecorations != null && !nameDecorations.isEmpty()) {
            String[] decorations = nameDecorations.split(",");
            for (int i = decorations.length - 1; i >= 0; i--) {
                result.append("</").append(decorations[i].trim()).append(">");
            }
        }

        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(result.toString());
    }
}
