package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;
import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/
public class CosmeticsCommand extends BaseTabCommand {
    private final PrefixManager prefixManager;
    private final GeneralDatabase database;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CosmeticsCommand(Main plugin) {
        super("cosmetics", "/cosmetics <type> <action> [args]", "8b8tcore.command.cosmetics");
        this.prefixManager = new PrefixManager();
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cOnly players can use this command.");
            return;
        }

        if (args.length < 2) {
            sendHelp(player);
            return;
        }

        String type = args[0].toLowerCase();
        String action = args[1].toLowerCase();

        switch (type) {
            case "title" -> handleTitle(player, action, args);
            case "nick" -> handleNick(player, action, args);
            case "item" -> handleItem(player, action, args);
            case "clear" -> {
                handleTitle(player, "clear", args);
                handleNick(player, "clear", args);
            }
            default -> sendHelp(player);
        }
    }

    private void handleTitle(Player player, String action, String[] args) {
        if (action.equals("clear")) {
            database.updateSelectedRank(player.getName(), null);
            database.updatePrefixGradient(player.getName(), null);
            database.updatePrefixAnimation(player.getName(), "none");
            database.updatePrefixSpeed(player.getName(), 5);
            database.updatePrefixDecorations(player.getName(), null);

            me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) Main.getInstance().getSectionByName("ChatControl");
            if (chatSection != null) {
                me.txmc.core.chat.ChatInfo info = chatSection.getInfo(player);
                if (info != null) {
                    info.setSelectedRank(null);
                    info.setCustomGradient(null);
                    info.setPrefixAnimation("none");
                    info.setPrefixSpeed(5);
                    info.setPrefixDecorations(null);
                    info.clearAnimatedNameCache();
                }
            }

            refreshPlayer(player);
            sendPrefixedLocalizedMessage(player, "title_cleared");
        } else if (action.equals("color")) {
            if (!player.hasPermission("8b8tcore.prefix.custom")) {
                sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
                return;
            }
            
            List<String> styles = new ArrayList<>();
            String colors = null;

            for (int i = 2; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("#")) {
                    colors = arg;
                } else if (List.of("bold", "italic", "underlined", "strikethrough").contains(arg)) {
                    styles.add(arg);
                }
            }

            if (colors == null) {
                sendMessage(player, "&cUsage: /cosmetics title color <#hex:#hex...> [styles]");
                return;
            }

            if (!colors.matches("^#[0-9A-Fa-f]{6}(:#[0-9A-Fa-f]{6})*$")) {
                sendPrefixedLocalizedMessage(player, "gradient_invalid", colors);
                return;
            }
            
            String decorations = String.join(",", styles);
            database.updatePrefixGradient(player.getName(), colors);
            database.updatePrefixDecorations(player.getName(), decorations).thenRun(() -> refreshPlayer(player));
            
            String preview = colors;
            if (!styles.isEmpty()) {
                preview += " with styles: " + String.join(", ", styles);
            }
            sendMessage(player, "&aTitle gradient set: &r" + preview);
        } else if (action.equals("animation")) {
            if (!player.hasPermission("8b8tcore.prefix.custom")) {
                sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
                return;
            }
            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics title animation <wave/pulse/smooth/saturate/bounce/billboard/sweep/shimmer/none>");
                return;
            }
            String anim = args[2].toLowerCase();
            if (!List.of("wave", "pulse", "smooth", "saturate", "bounce", "billboard", "sweep", "shimmer", "none").contains(anim)) {
                sendMessage(player, "&cInvalid animation! Use: wave, pulse, smooth, saturate, bounce, billboard, sweep, shimmer, or none");
                return;
            }
            database.updatePrefixAnimation(player.getName(), anim).thenRun(() -> refreshPlayer(player));
            sendMessage(player, "&aTitle animation set to: &e" + anim);
        } else if (action.equals("speed")) {
            if (!player.hasPermission("8b8tcore.prefix.custom")) {
                sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
                return;
            }
            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics title speed <1-5>");
                return;
            }
            try {
                int speed = Integer.parseInt(args[2]);
                if (speed < 1 || speed > 5) {
                    sendMessage(player, "&cSpeed must be between 1 and 5!");
                    return;
                }
                database.updatePrefixSpeed(player.getName(), speed).thenRun(() -> refreshPlayer(player));
                sendMessage(player, "&aTitle animation speed set to: &e" + speed);
            } catch (NumberFormatException e) {
                sendMessage(player, "&cInvalid number!");
            }
        } else if (action.equals("set")) {
            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics title set <rank>");
                return;
            }
            String rankInput = args[2];
            selectRank(player, rankInput);
        } else {
            sendHelp(player);
        }
    }

    private void selectRank(Player player, String input) {
        List<String> available = prefixManager.getAvailableRanks(player);
        String targetRank = null;
        String inputLower = input.toLowerCase();

        for (String rank : available) {
            String friendly = prefixManager.getRankDisplayName(rank).toLowerCase();
            String permission = rank.toLowerCase();
            String stripped = rank.replace("8b8tcore.prefix.", "").toLowerCase();

            if (inputLower.equals(friendly) || inputLower.equals(permission) || inputLower.equals(stripped)) {
                targetRank = rank;
                break;
            }
        }

        if (targetRank != null) {
            database.updateSelectedRank(player.getName(), targetRank).thenRun(() -> refreshPlayer(player));
            sendPrefixedLocalizedMessage(player, "title_success", prefixManager.getRankDisplayName(targetRank));
        } else {
            sendPrefixedLocalizedMessage(player, "title_no_permission_for_rank");
        }
    }

    private void handleNick(Player player, String action, String[] args) {
        if (action.equals("clear")) {
            database.insertNickname(player.getName(), null);
            database.updateCustomGradient(player.getName(), null);
            database.updateGradientAnimation(player.getName(), "none");
            database.updateGradientSpeed(player.getName(), 5);
            database.updateNameDecorations(player.getName(), null);

            me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) Main.getInstance().getSectionByName("ChatControl");
            if (chatSection != null) {
                me.txmc.core.chat.ChatInfo info = chatSection.getInfo(player);
                if (info != null) {
                    info.setNickname(null);
                    info.setNameGradient(null);
                    info.setNameAnimation("none");
                    info.setNameSpeed(5);
                    info.setNameDecorations(null);
                    info.clearAnimatedNameCache();
                }
            }

            player.getScheduler().run(Main.getInstance(), (task) -> {
                player.displayName(miniMessage.deserialize(player.getName()));
                refreshPlayer(player);
            }, null);
            sendPrefixedLocalizedMessage(player, "nick_reset");
        } else if (action.equals("color")) {
            if (!player.hasPermission("8b8tcore.command.nc")) {
                sendPrefixedLocalizedMessage(player, "nc_no_permission");
                return;
            }

            if (args.length > 2 && args[2].equalsIgnoreCase("tobias")) {
                handleTobiasNick(player, args);
                return;
            }
            
            List<String> styles = new ArrayList<>();
            String colors = null;

            for (int i = 2; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("#")) {
                    colors = arg;
                } else if (List.of("bold", "italic", "underlined", "strikethrough").contains(arg)) {
                    styles.add(arg);
                }
            }

            if (colors == null) {
                sendMessage(player, "&cUsage: /cosmetics nick color <#hex:#hex...> [styles]");
                return;
            }

            if (!colors.matches("^#[0-9A-Fa-f]{6}(:#[0-9A-Fa-f]{6})*$")) {
                sendPrefixedLocalizedMessage(player, "gradient_invalid", colors);
                return;
            }

            final String finalColors = colors;
            database.getNicknameAsync(player.getName()).thenAccept(currentNick -> {
                if (currentNick == null || currentNick.isEmpty() || currentNick.equals(player.getName())) {
                    database.insertNickname(player.getName(), player.getName());
                } else {
                    String plain = PlainTextComponentSerializer.plainText().serialize(
                            miniMessage.deserialize(
                                    GlobalUtils.convertToMiniMessageFormat(currentNick)))
                            .trim();
                    database.insertNickname(player.getName(), plain);
                }

                String decorations = String.join(",", styles);
                database.updateCustomGradient(player.getName(), finalColors);
                database.updateNameDecorations(player.getName(), decorations).thenRun(() -> {
                    GlobalUtils.updateDisplayNameAsync(player).thenRun(() -> refreshPlayer(player));
                });
            });
            
            String preview = colors;
            if (!styles.isEmpty()) {
                preview += " with styles: " + String.join(", ", styles);
            }
            sendMessage(player, "&aNickname gradient set: &r" + preview);
        } else if (action.equals("animation")) {
            if (!player.hasPermission("8b8tcore.command.nc")) {
                sendPrefixedLocalizedMessage(player, "nc_no_permission");
                return;
            }
            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics nick animation <wave/pulse/smooth/saturate/bounce/billboard/sweep/shimmer/none>");
                return;
            }
            String anim = args[2].toLowerCase();
            if (!List.of("wave", "pulse", "smooth", "saturate", "bounce", "billboard", "sweep", "shimmer", "none").contains(anim)) {
                sendMessage(player, "&cInvalid animation! Use: wave, pulse, smooth, saturate, bounce, billboard, sweep, shimmer, or none");
                return;
            }
            database.updateGradientAnimation(player.getName(), anim).thenRun(() -> {
                GlobalUtils.updateDisplayNameAsync(player).thenRun(() -> refreshPlayer(player));
            });
            sendMessage(player, "&aNickname animation set to: &e" + anim);
        } else if (action.equals("speed")) {
            if (!player.hasPermission("8b8tcore.command.nc")) {
                sendPrefixedLocalizedMessage(player, "nc_no_permission");
                return;
            }
            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics nick speed <1-5>");
                return;
            }
            try {
                int speed = Integer.parseInt(args[2]);
                if (speed < 1 || speed > 5) {
                    sendMessage(player, "&cSpeed must be between 1 and 5!");
                    return;
                }
                database.updateGradientSpeed(player.getName(), speed).thenRun(() -> {
                    GlobalUtils.updateDisplayNameAsync(player).thenRun(() -> refreshPlayer(player));
                });
                sendMessage(player, "&aNickname animation speed set to: &e" + speed);
            } catch (NumberFormatException e) {
                sendMessage(player, "&cInvalid number!");
            }
        } else {
            if (action.equals("set")) {
                if (args.length < 3) {
                    sendHelp(player);
                    return;
                }
                setNickname(player, args, 2);
            } else {
                setNickname(player, args, 1);
            }
        }
    }

    private void handleTobiasNick(Player player, String[] args) {
        if (!player.hasPermission("8b8tcore.command.nc")) {
            sendPrefixedLocalizedMessage(player, "nc_no_permission");
            return;
        }

        if (args.length < 4) {
            sendMessage(player, "&cUsage: /cosmetics nick color tobias <length:decorations:#color> ...");
            sendMessage(player, "&7Example: /cosmetics nick color tobias 6:bold/italic:#FF0000 8:none:#00FF00");
            return;
        }

        database.getNicknameAsync(player.getName()).thenAccept(currentNick -> {
            if (currentNick == null || currentNick.isEmpty()) currentNick = player.getName();
            
            String plainNick = PlainTextComponentSerializer.plainText()
                    .serialize(miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(currentNick))).trim();
            int nickLength = plainNick.length();

            int totalLength = 0;
            List<String> parts = new ArrayList<>();
            for (int i = 3; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (!arg.matches("^\\d+:[a-z/]+:#[0-9a-f]{6}$")) {
                    sendMessage(player, "&cInvalid part format: " + arg + ". Expected length:decorations:#color");
                    return;
                }
                String[] split = arg.split(":");
                try {
                    int len = Integer.parseInt(split[0]);
                    totalLength += len;
                    parts.add(arg);
                } catch (NumberFormatException e) {
                    sendMessage(player, "&cInvalid length in part: " + arg);
                    return;
                }
            }

            if (totalLength != nickLength) {
                sendMessage(player, "&cTotal length of parts (" + totalLength + ") does not match nickname length (" + nickLength + ")");
                return;
            }

            String tobiasFormat = "tobias:" + String.join(";", parts);
            database.updateCustomGradient(player.getName(), tobiasFormat).thenRun(() -> {
                GlobalUtils.updateDisplayNameAsync(player).thenRun(() -> refreshPlayer(player));
            });
            sendMessage(player, "&aNickname set to special Tobias format!");
        });
    }

    private void setNickname(Player player, String[] args, int startIndex) {
        if (!player.hasPermission("8b8tcore.command.nick")) {
            sendPrefixedLocalizedMessage(player, "nick_no_permission");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String raw = sb.toString().trim();
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(raw))).trim();

        if (plain.length() > 16) {
            sendPrefixedLocalizedMessage(player, "nick_too_large", "16");
            return;
        }

        database.insertNickname(player.getName(), plain).thenRun(() -> {
            GlobalUtils.updateDisplayNameAsync(player).thenRun(() -> refreshPlayer(player));
            sendPrefixedLocalizedMessage(player, "nick_success", plain);
        });
    }

    private void handleItem(Player player, String action, String[] args) {
        if (action.equals("color")) {
            if (!player.hasPermission("8b8tcore.command.ic")) {
                sendPrefixedLocalizedMessage(player, "ic_no_permission");
                return;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                sendPrefixedLocalizedMessage(player, "ic_no_item");
                return;
            }

            if (args.length < 3) {
                sendMessage(player, "&cUsage: /cosmetics item color <#hex:#hex...> [styles]");
                return;
            }

            List<String> styles = new ArrayList<>();
            String colors = null;

            for (int i = 2; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("#")) {
                    colors = args[i];
                } else if (List.of("bold", "italic", "underlined", "strikethrough").contains(arg)) {
                    styles.add(arg);
                }
            }

            if (colors == null) {
                sendMessage(player, "&cYou must provide a color (e.g. #FF0000).");
                return;
            }

            ItemMeta meta = item.getItemMeta();
            String name = getCleanItemName(item);
            String escapedName = name.replace("<", "\\<").replace(">", "\\>");
            
            StringBuilder mm = new StringBuilder();

            for (String style : styles)
                mm.append("<").append(style).append(">");

            boolean isGradient = colors.contains(":") && colors.indexOf('#') != colors.lastIndexOf('#');

            if (isGradient) {
                mm.append("<gradient:").append(colors).append(">").append(escapedName).append("</gradient>");
            } else {
                mm.append("<color:").append(colors.split(":")[0]).append(">").append(escapedName).append("</color>");
            }
            
            for (int i = styles.size() - 1; i >= 0; i--) {
                mm.append("</").append(styles.get(i)).append(">");
            }

            meta.displayName(miniMessage.deserialize(mm.toString()).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
            sendPrefixedLocalizedMessage(player, "ic_success", miniMessage.serialize(meta.displayName()));
        } else {
            sendHelp(player);
        }
    }

    private String getCleanItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return item.getType().name().replace("_", " ").toLowerCase();
    }

    private void sendHelp(Player player) {
        sendMessage(player, "&6--- Cosmetics Help ---");
        
        String colorHelp = "<gold>/cosmetics <yellow><title|nick|item> <gold>color <red><colors> <aqua>[styles]</red>";
        String colorHover = "<gray>Colors: <white>#RRGGBB <gray>or <white>#RRGGBB:#RRGGBB <gray>(Gradient)\n" +
                           "<gray>Styles: <white>bold, italic, underlined, strikethrough";
        GlobalUtils.sendPrefixedComponent(player, miniMessage.deserialize(colorHelp)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(miniMessage.deserialize(colorHover))));

        String tobiasHelp = "<gold>/cosmetics <yellow>nick <gold>color <red>tobias <aqua><parts...>";
        String tobiasHover = "<gray>Formatting.\n<gray>Format: <white>length:decorations:#color\n<gray>Example: <white>6:bold:#FF0000 8:none:#00FF00";
        GlobalUtils.sendPrefixedComponent(player, miniMessage.deserialize(tobiasHelp)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(miniMessage.deserialize(tobiasHover))));

        String animHelp = "<gold>/cosmetics <yellow><title|nick> <gold>animation <red><type>";
        String animHover = "<gray>Types: <white>wave, pulse, smooth, saturate, bounce, billboard, sweep, shimmer, none";
        GlobalUtils.sendPrefixedComponent(player, miniMessage.deserialize(animHelp)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(miniMessage.deserialize(animHover))));

        sendMessage(player, "&6/cosmetics <nick|title> speed &e<1-5>");
        sendMessage(player, "&6/cosmetics <nick|title> clear");
        sendMessage(player, "&6/cosmetics title set &e<rank>");
        sendMessage(player, "&6/cosmetics nick &e<name>");
        sendMessage(player, "&3----------------------");
    }

    private void refreshPlayer(Player player) {
        Main plugin = (Main) Main.getInstance();
        me.txmc.core.Section chatSection = plugin.getSectionByName("ChatControl");
        if (chatSection instanceof me.txmc.core.chat.ChatSection chatSec) {
            me.txmc.core.chat.ChatInfo info = chatSec.getInfo(player);
            if (info != null) {
                info.clearAnimatedNameCache();
                chatSec.loadAllDataAsync(info);
            }
        }
        
        player.getScheduler().runDelayed(plugin, (task) -> {
            if (!player.isOnline()) return;
            me.txmc.core.Section section = plugin.getSectionByName("TabList");
            if (section instanceof me.txmc.core.tablist.TabSection tabSection) {
                tabSection.setTab(player, true);
            }
        }, null, 5L);
    }

    @Override
    public List<String> onTab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player))
            return Collections.emptyList();

        if (args.length == 1) {
            String lastArg = args[0].toLowerCase();
            return List.of("title", "nick", "item").stream()
                    .filter(s -> s.startsWith(lastArg))
                    .toList();
        }

        String type = args[0].toLowerCase();
        if (args.length == 2) {
            String lastArg = args[1].toLowerCase();
            return switch (type) {
                case "title" -> List.of("clear", "color", "animation", "speed", "set").stream()
                        .filter(s -> s.startsWith(lastArg)).toList();
                case "nick" -> List.of("clear", "color", "animation", "speed", "set").stream()
                        .filter(s -> s.startsWith(lastArg)).toList();
                case "item" -> List.of("color").stream()
                        .filter(s -> s.startsWith(lastArg)).toList();
                default -> Collections.emptyList();
            };
        }

        if (type.equals("title")) {
            if (args[1].equals("set") && args.length == 3) {
                String lastArg = args[2].toLowerCase();
                return prefixManager.getAvailableRanks(player).stream()
                        .filter(rank -> rank.toLowerCase().contains(lastArg) || 
                                prefixManager.getRankDisplayName(rank).toLowerCase().startsWith(lastArg))
                        .toList();
            }
        }

        if (args.length >= 3) {
            String lastArg = args[args.length - 1].toLowerCase();
            
            if ((type.equals("title") && args[1].equals("color")) ||
                    (type.equals("nick") && args[1].equals("color")) ||
                    (type.equals("item") && args[1].equals("color"))) {
                
                boolean hasColor = false;
                for (int i = 2; i < args.length - 1; i++) {
                    if (args[i].startsWith("#")) {
                        hasColor = true;
                        break;
                    }
                }
                
                List<String> suggestions = new ArrayList<>();
                List<String> hexes = List.of("#FFFFFF", "#FF0000", "#00FF00", "#0000FF", 
                                              "#FF0000:#00FF00", "#00FFFF:#FF00FF");
                suggestions.addAll(hexes);
                if (type.equals("nick") && args[1].equals("color") && args.length == 3) {
                    suggestions.add("tobias");
                }
                
                if (hasColor) {
                    List<String> allStyles = List.of("bold", "italic", "underlined", "strikethrough");
                    List<String> usedStyles = new ArrayList<>();
                    for (int i = 2; i < args.length - 1; i++) {
                        String arg = args[i].toLowerCase();
                        if (allStyles.contains(arg)) {
                            usedStyles.add(arg);
                        }
                    }
                    allStyles.stream()
                        .filter(style -> !usedStyles.contains(style))
                        .forEach(suggestions::add);
                }
                
                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(lastArg))
                        .toList();
            }
            
            if ((type.equals("title") && args[1].equals("animation")) ||
                    (type.equals("nick") && args[1].equals("animation"))) {
                return List.of("wave", "pulse", "smooth", "saturate", "bounce", "billboard", "sweep", "shimmer", "none").stream()
                        .filter(a -> a.startsWith(lastArg))
                        .toList();
            }
            
            if ((type.equals("title") && args[1].equals("speed")) ||
                    (type.equals("nick") && args[1].equals("speed"))) {
                return List.of("1", "2", "3", "4", "5").stream()
                        .filter(s -> s.startsWith(lastArg))
                        .toList();
            }
        }

        return Collections.emptyList();
    }
}