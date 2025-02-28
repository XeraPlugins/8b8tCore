package me.txmc.core.command.commands;

import lombok.Getter;
import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
public class NameColorCommand extends BaseTabCommand {
    private final List<String> colorOptions;
    private final List<String> styleOptions;
    private final List<Gradient> gradientsOptions;
    private final MiniMessage miniMessage;
    private final GeneralDatabase database;

    public NameColorCommand(Main plugin) {
        super("nc", "/nc <color> <styles...>", "8b8tcore.command.nc");
        styleOptions = getStyleOptions();
        miniMessage = MiniMessage.miniMessage();
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
        gradientsOptions = new ArrayList<>(List.of(new Gradient("fire", "#F3904F", "#E7654A"),
            new Gradient("sky", "#1488CC", "#0B14C3"),
            new Gradient("pink", "#F4C4F3", "#EC00E9"),
            new Gradient("green", "#59ED33", "#165818"),
            new Gradient("silver", "#C9C9C9", "#2F2F2F"),
            new Gradient("red", "#BF0C1F", "#DE4E4E"),
            new Gradient("aqua", "#4FF3EA", "#00828D"),
            new Gradient("retro", "#F34F4F", "#65008D"),
            new Gradient("mango", "#2BBFA7","#FFC600"),
            new Gradient("orange", "#FF9800", "#BC4664"),
            new Gradient("red_dark", "#212121", "#f4143f"),
            new Gradient("rick", "#98f8dd", "#F6FF58"),
            new Gradient("purple", "#d481d2", "#703b94")
        ));
        colorOptions = getColorOptions();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can run this command");
            return;
        }

        if (args.length < 1) {
            sendPrefixedLocalizedMessage(player, "nc_usage");
            return;
        }

        String colorName = args[0].toLowerCase();
        if (!colorOptions.contains(colorName)) {
            sendPrefixedLocalizedMessage(player, "nc_invalid_color");
            return;
        }

        String colorCode = getColorCode(colorName);
        StringBuilder nameBuilder = new StringBuilder(colorCode);

        for (int i = 1; i < args.length; i++) {
            String styleCode = getStyleCode(args[i]);
            if (styleCode != null) {
                nameBuilder.append(styleCode);
            }
        }

        nameBuilder.append(player.getName());

        String displayNameString = nameBuilder.toString();
        Component displayName = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(displayNameString));

        player.displayName(displayName);
        String playerName = miniMessage.serialize(player.displayName());
        sendPrefixedLocalizedMessage(player, "nc_success", playerName);

        database.insertNickname(player.getName(), GlobalUtils.convertToMiniMessageFormat(displayNameString));

    }

    private String getColorCode(String colorName) {
        return switch (colorName) {
            case "black" -> "&0";
            case "dark_blue" -> "&1";
            case "dark_green" -> "&2";
            case "dark_aqua" -> "&3";
            case "dark_red" -> "&4";
            case "dark_purple" -> "&5";
            case "gold" -> "&6";
            case "gray" -> "&7";
            case "dark_gray" -> "&8";
            case "blue" -> "&9";
            case "green" -> "&a";
            case "aqua" -> "&b";
            case "red" -> "&c";
            case "light_purple" -> "&d";
            case "yellow" -> "&e";
            case "white" -> "&f";
            case "rainbow" -> "<rainbow>";
            default -> getGradient(colorName);
        };
    }

    private String getGradient(String colorName) {
        for (Gradient gradient : gradientsOptions) {
            if (gradient.getName().equals(colorName)) {
                return gradient.getTag();
            }
        }
        return "&f";
    }

    private String getStyleCode(String styleName) {
        return switch (styleName.toLowerCase()) {
            case "bold" -> "&l";
            case "italic" -> "&o";
            case "underline" -> "&n";
            case "strikethrough" -> "&m";
            default -> null;
        };
    }

    public List<String> getColorOptions() {
        List<String> colorOptions = new ArrayList<>(List.of("black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white", "reset"));
        colorOptions.addAll(gradientsOptions.stream().map(Gradient::getName).toList());
        return colorOptions;
    }

    public List<String> getStyleOptions() {
        return List.of("bold", "italic", "underline", "strikethrough");
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
        if (sender instanceof Player player)  sendPrefixedLocalizedMessage(player, "nc_no_permission");
    }

    @Getter
    private static class Gradient{
        public Gradient(String name, String... hex) {
            this.name = "gradient_" + name;
            this.tag =  "<gradient:" + String.join(":", hex) + ">";
        }

        private final String name, tag;
    }
}
