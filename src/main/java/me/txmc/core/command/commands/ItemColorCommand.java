package me.txmc.core.command.commands;

import me.txmc.core.command.BaseTabCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;
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
public class ItemColorCommand extends BaseTabCommand {
    private final List<String> colorOptions;
    private final List<String> styleOptions;

    public ItemColorCommand() {
        super("ic", "/ic <color> <styles...>", "8b8tcore.command.ic");
        colorOptions = getColorOptions();
        styleOptions = getStyleOptions();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        getSenderAsPlayer(sender).ifPresentOrElse(player -> {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType() == Material.AIR) {
                sendPrefixedLocalizedMessage(player, "ic_no_item");
                return;
            }

            if (args.length < 1) {
                sendPrefixedLocalizedMessage(player, "ic_usage");
                return;
            }

            NamedTextColor color = getColor(args[0]);
            if (color == null) {
                sendPrefixedLocalizedMessage(player, "ic_invalid_color");
                return;
            }

            ItemMeta meta = itemInHand.getItemMeta();
            if (meta == null) {
                sendPrefixedLocalizedMessage(player, "ic_invalid_item");
                return;
            }

            String itemName = getCleanItemName(itemInHand);
            Component itemNameComponent = Component.text(itemName).color(color).decoration(TextDecoration.ITALIC,false);

            for (int i = 1; i < args.length; i++) {
                TextDecoration decoration = getStyle(args[i]);
                if (decoration != null) {
                    itemNameComponent = itemNameComponent.decoration(decoration,true);
                }
            }

            meta.displayName(itemNameComponent);
            itemInHand.setItemMeta(meta);

            sendPrefixedLocalizedMessage(player, "ic_success", LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName()));
        }, () -> sendMessage(sender, "&c%s", PLAYER_ONLY));
    }

    private NamedTextColor getColor(String colorName) {
        try {
            return NamedTextColor.NAMES.value(colorName.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TextDecoration getStyle(String styleName) {
        return switch (styleName.toLowerCase()) {
            case "bold" -> TextDecoration.BOLD;
            case "italic" -> TextDecoration.ITALIC;
            case "underline" -> TextDecoration.UNDERLINED;
            case "strikethrough" -> TextDecoration.STRIKETHROUGH;
            default -> null;
        };
    }

    public List<String> getColorOptions() {
        return NamedTextColor.NAMES.keys().stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    public List<String> getStyleOptions() {
        List<String> styles = new ArrayList<>();
        styles.add("bold");
        styles.add("italic");
        styles.add("underline");
        styles.add("strikethrough");
        return styles;
    }

    private String getCleanItemName(ItemStack item) {
        if (item == null) {
            return "";
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            return ChatColor.stripColor(name);
        } else {
            return item.getType().toString().replace("_", " ").toLowerCase();
        }
    }

    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            return colorOptions.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            return styleOptions.stream()
                    .filter(option -> option.startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player)  sendPrefixedLocalizedMessage(player, "ic_no_permission");
    }

}
