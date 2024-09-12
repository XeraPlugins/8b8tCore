package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public NickCommand(Main plugin) {
        super("nick", "/nick <name>", "8b8tcore.command.nick");
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "nick_no_name_provided");
            return;
        }

        String nickname = String.join(" ", args);

        Component displayName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(nickname));

        if (extractPlainText(displayName).length() > 16) {
            sendPrefixedLocalizedMessage(player, "nick_too_large");
            return;
        }

        player.displayName(displayName);
        String playerName = LegacyComponentSerializer.legacyAmpersand().serialize(player.displayName());

        sendPrefixedLocalizedMessage(player, "nick_success", playerName);
        database.insertNickname(player.getName(), GlobalUtils.convertToMiniMessageFormat(nickname));

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
