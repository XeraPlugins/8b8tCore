package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import me.txmc.core.util.GradientAnimator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Allows players to change their nickname with color and style codes.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Servers.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/09/11 08:10 PM
 */
public class NickCommand extends BaseCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GeneralDatabase database;
    private final PrefixManager prefixManager = new PrefixManager();

    public NickCommand(Main plugin) {
        super("nick", "/nick <name>", "8b8tcore.command.nick");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "nick_usage", "/nick <name|reset|gradient <colors...>>");
            return;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            resetNickname(player);
            return;
        }

        if (args[0].equalsIgnoreCase("gradient")) {
            if (args.length < 2) {
                sendPrefixedLocalizedMessage(player, "gradient_usage", "/nick gradient <color1> [color2] [color3...]");
                return;
            }
            
            StringBuilder gradient = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                String color = args[i].startsWith("#") ? args[i] : "#" + args[i];
                if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                    sendPrefixedLocalizedMessage(player, "gradient_invalid_color", color);
                    return;
                }
                if (i > 1) gradient.append(":");
                gradient.append(color);
            }
            
            database.updateGradient(player.getName(), gradient.toString());
            sendPrefixedLocalizedMessage(player, "gradient_set", gradient.toString());
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

        database.insertNickname(player.getName(), GlobalUtils.convertToMiniMessageFormat(nickname));        
        GlobalUtils.updateDisplayName(player);
        String tag = prefixManager.getPrefix(player);
        Component finalName = player.displayName();
        if (!tag.isEmpty()) {
            finalName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(tag)).append(finalName);
        }
        player.playerListName(finalName);

        sendPrefixedLocalizedMessage(player, "nick_success", miniMessage.serialize(player.displayName()));
    }
    
    private void resetNickname(Player player) {
        database.insertNickname(player.getName(), null);
        GlobalUtils.updateDisplayName(player);
        
        String tag = prefixManager.getPrefix(player);
        Component finalName = player.displayName();
        if (!tag.isEmpty()) {
            finalName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(tag)).append(finalName);
        }
        player.playerListName(finalName);
        
        sendPrefixedLocalizedMessage(player, "nick_reset");
    }

    private String extractPlainText(Component component) {
        StringBuilder plainText = new StringBuilder();
        extractPlainTextRecursive(component, plainText);
        return plainText.toString();
    }

    private void extractPlainTextRecursive(Component component, StringBuilder plainText) {
        if (component instanceof TextComponent textComponent) {
            plainText.append(textComponent.content());
        }
        for (Component child : component.children()) {
            extractPlainTextRecursive(child, plainText);
        }
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player)  sendPrefixedLocalizedMessage(player, "nick_no_permission");
    }
}
