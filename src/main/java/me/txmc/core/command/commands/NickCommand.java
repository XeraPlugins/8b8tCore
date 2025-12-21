package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class NickCommand extends BaseCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GeneralDatabase database;
    private final PrefixManager prefixManager = new PrefixManager();

    public NickCommand(Main plugin) {
        super("nick", "/nick <name|reset>", "8b8tcore.command.nick");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "nick_usage", "/nick <name|reset>");
            return;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            resetNickname(player);
            return;
        }

        String nickname = String.join(" ", args)
            .replaceAll("(?i)<(hover:.*?|click:.*?|insert:.*?|selector:.*?|nbt:.*?|newline)[^>]*>", "")
            .trim();

        String plainText = nickname.replaceAll("<[^>]+>", "");
        if (plainText.length() > 16) {
            sendPrefixedLocalizedMessage(player, "nick_too_large", "16");
            return;
        }

        database.insertNickname(player.getName(), GlobalUtils.convertToMiniMessageFormat(nickname)).thenRun(() -> {
            GlobalUtils.updateDisplayName(player);
            String tag = prefixManager.getPrefix(player);
            Component finalName = player.displayName();
            if (!tag.isEmpty()) {
                finalName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(tag)).append(finalName);
            }
            player.playerListName(finalName);

            sendPrefixedLocalizedMessage(player, "nick_success", miniMessage.serialize(player.displayName()));
        });
    }
    
    private void resetNickname(Player player) {
        database.insertNickname(player.getName(), null).thenRun(() -> {
            GlobalUtils.updateDisplayName(player);
            
            String tag = prefixManager.getPrefix(player);
            Component finalName = player.displayName();
            if (!tag.isEmpty()) {
                finalName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(tag)).append(finalName);
            }
            player.playerListName(finalName);
            
            sendPrefixedLocalizedMessage(player, "nick_reset");
        });
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player)  sendPrefixedLocalizedMessage(player, "nick_no_permission");
    }
}
