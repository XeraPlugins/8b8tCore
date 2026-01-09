package me.txmc.core.chat.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatCommand;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2026/01/02
 * This file was created as a part of 8b8tCore
*/
public class ChatCommands {

    @RequiredArgsConstructor
    public static class IgnoreCommand implements CommandExecutor {
        private final ChatSection manager;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (sender instanceof Player player) {
                if (args.length == 1) {
                    ChatInfo info = manager.getInfo(player);
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                    if (!info.isIgnoring(target.getUniqueId())) {
                        info.ignorePlayer(target.getUniqueId());
                        sendPrefixedLocalizedMessage(player, "ignore_successful", target.getName());
                    } else sendPrefixedLocalizedMessage(player, "already_ignoring");
                } else sendPrefixedLocalizedMessage(player, "ignore_command_syntax");
            } else sendMessage(sender, "&cYou must be a player");
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class UnIgnoreCommand implements CommandExecutor {
        private final ChatSection manager;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args) {
            if (sender instanceof Player player) {
                if (args.length == 1) {
                    ChatInfo info = manager.getInfo(player);
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                    if (info.isIgnoring(target.getUniqueId())) {
                        info.unignorePlayer(target.getUniqueId());
                        sendPrefixedLocalizedMessage(player, "unignore_successful", target.getName());
                    } else sendPrefixedLocalizedMessage(player, "unignore_not_ignoring", target.getName());
                } else sendPrefixedLocalizedMessage(player, "unignore_command_syntax");
            } else sendMessage(sender, "&cYou must be a player");
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class IgnoreListCommand implements CommandExecutor {
        private final ChatSection manager;

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (sender instanceof Player player) {
                ChatInfo info = manager.getInfo(player);
                if (info != null) {
                    if (!info.getIgnoring().isEmpty()) {
                        String ignoredList = info.getIgnoring().stream()
                                .map(uuid -> Bukkit.getServer().getOfflinePlayer(uuid).getName())
                                .filter(name -> name != null && !name.isEmpty())
                                .collect(Collectors.joining("&3, &c"));
                        sendPrefixedLocalizedMessage(player, "ignorelist_successful", ignoredList);
                    } else sendPrefixedLocalizedMessage(player, "ignorelist_not_ignoring");
                } else sendPrefixedLocalizedMessage(player, "ignorelist_failed");
            } else sendMessage(sender, "You must be a player to use this command.");
            return true;
        }
    }

    public static class MessageCommand extends ChatCommand {
        public MessageCommand(ChatSection manager) {
            super(manager);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (sender instanceof Player player) {
                if (args.length >= 2) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null && target.isOnline()) {
                        ChatInfo senderInfo = manager.getInfo(player);
                        ChatInfo targetInfo = manager.getInfo(target);
                        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        sendWhisper(player, senderInfo, target, targetInfo, msg);
                    } else sendPrefixedLocalizedMessage(player, "msg_could_not_find_player", args[0]);
                } else sendPrefixedLocalizedMessage(player, "msg_command_syntax");
            } else sendMessage(sender, "&cYou must be a player");
            return true;
        }
    }

    public static class ReplyCommand extends ChatCommand {
        public ReplyCommand(ChatSection manager) {
            super(manager);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (sender instanceof Player player) {
                if (args.length >= 1) {
                    ChatInfo senderInfo = manager.getInfo(player);
                    if (senderInfo.getReplyTarget() != null) {
                        Player target = senderInfo.getReplyTarget();
                        if (target.isOnline()) {
                            ChatInfo targetInfo = manager.getInfo(target);
                            String msg = String.join(" ", args);
                            sendWhisper(player, senderInfo, target, targetInfo, msg);
                        } else sendPrefixedLocalizedMessage(player, "reply_player_offline", target.getName());
                    } else sendPrefixedLocalizedMessage(player, "reply_no_target");
                } else sendPrefixedLocalizedMessage(player, "reply_command_syntax");
            } else sendMessage(sender, "&cYou must be a player");
            return true;
        }
    }

    @RequiredArgsConstructor
    public static class ToggleChatCommand implements CommandExecutor {
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
}
