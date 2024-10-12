package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Allows players to rename the item in their hand with color and style codes.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Servers.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/09/11 08:10 PM
 */
public class RenameCommand extends BaseCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public RenameCommand() {
        super("rename", "/rename <name>", "8b8tcore.command.rename");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "rename_no_name_provided");
            return;
        }

        String itemName = String.join(" ", args).replaceAll("(?i)<(hover:.*?|click:.*?|insert:.*?|selector:.*?|nbt:.*?|newline)[^>]*>", "").trim();

        String miniMessageFormatted = GlobalUtils.convertToMiniMessageFormat(itemName);

        Component displayName = miniMessage.deserialize(miniMessageFormatted);

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            sendPrefixedLocalizedMessage(player, "rename_no_item");
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            sendPrefixedLocalizedMessage(player, "rename_invalid_item");
            return;
        }

        if (extractPlainText(displayName).length() > 50) {
            sendPrefixedLocalizedMessage(player, "rename_too_large");
            return;
        }

        meta.displayName(displayName.decoration(TextDecoration.ITALIC,false));
        itemInHand.setItemMeta(meta);

        String displayNameLegacy = LegacyComponentSerializer.legacyAmpersand().serialize(displayName);
        sendPrefixedLocalizedMessage(player, "rename_success", displayNameLegacy);
    }

    private String extractPlainText(Component component) {
        StringBuilder plainText = new StringBuilder();
        extractPlainTextRecursive(component, plainText);
        return plainText.toString();
    }

    private void extractPlainTextRecursive(Component component, StringBuilder plainText) {
        if (component instanceof TextComponent textComponent) {
            plainText.append(textComponent.decoration(TextDecoration.ITALIC,false).content());
        }
        for (Component child : component.children()) {
            extractPlainTextRecursive(child, plainText);
        }
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player) {
            sendPrefixedLocalizedMessage(player, "rename_no_permission");
        }
    }
}
