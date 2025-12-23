package me.txmc.core.command.commands;

import me.txmc.core.command.BaseTabCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
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

            ItemMeta meta = itemInHand.getItemMeta();
            if (meta == null) {
                sendPrefixedLocalizedMessage(player, "ic_invalid_item");
                return;
            }

            String itemName = getCleanItemName(itemInHand);
            List<String> colors = new ArrayList<>();
            List<String> decorations = new ArrayList<>();

            for (String arg : args) {
                if (arg.startsWith("#") || arg.contains(":")) {
                    colors.add(arg);
                } else if (styleOptions.contains(arg.toLowerCase())) {
                    decorations.add(arg.toLowerCase());
                } else if (arg.startsWith("<") && arg.endsWith(">")) {
                   String mmString = arg + itemName + arg.replace("<", "</");
                   meta.displayName(MiniMessage.miniMessage().deserialize(mmString).decoration(TextDecoration.ITALIC, false));
                   itemInHand.setItemMeta(meta);
                   sendPrefixedLocalizedMessage(player, "ic_success", MiniMessage.miniMessage().serialize(meta.displayName()));
                   return;
                }
            }

            StringBuilder mmBuilder = new StringBuilder();
            for (String dec : decorations) mmBuilder.append("<").append(dec).append(">");
            
            if (colors.size() > 1) {
                mmBuilder.append("<gradient:").append(String.join(":", colors)).append(">");
                mmBuilder.append(itemName).append("</gradient>");
            } else if (colors.size() == 1) {
                mmBuilder.append("<").append(colors.get(0)).append(">").append(itemName).append("</").append(colors.get(0)).append(">");
            } else {
                mmBuilder.append(itemName);
            }

            for (int i = 0; i < decorations.size(); i++) mmBuilder.append("</").append(decorations.get(i)).append(">");

            Component finalName = MiniMessage.miniMessage().deserialize(mmBuilder.toString()).decoration(TextDecoration.ITALIC, false);
            meta.displayName(finalName);
            itemInHand.setItemMeta(meta);

            sendPrefixedLocalizedMessage(player, "ic_success", MiniMessage.miniMessage().serialize(finalName));
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
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        } else {
            return item.getType().toString().replace("_", " ").toLowerCase();
        }
    }

    @Override
    public List<String> onTab(org.bukkit.command.CommandSender sender, String[] args) {
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
