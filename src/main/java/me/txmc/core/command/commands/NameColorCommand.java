package me.txmc.core.command.commands;

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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class NameColorCommand extends BaseTabCommand {
    private final List<String> decorationOptions;
    private final List<String> animationOptions;
    private final GeneralDatabase database;

    public NameColorCommand(Main plugin) {
        super("nc", "/nc <#hex:#hex...> [bold] [italic] [underlined] [strikethrough] [animation] [speed]", "8b8tcore.command.nc");
        decorationOptions = List.of("bold", "italic", "underlined", "strikethrough");
        animationOptions = List.of("wave", "pulse", "rainbow", "shift", "none");
        this.database = GeneralDatabase.getInstance();
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

        String colors = args[0];
        if (!colors.matches("^#[0-9A-Fa-f]{6}(:#[0-9A-Fa-f]{6})*$")) {
            sendPrefixedLocalizedMessage(player, "gradient_invalid", colors);
            return;
        }

        List<String> decorations = new ArrayList<>();
        String animation = "none";
        int speed = 5;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("underline")) arg = "underlined";
            if (arg.equals("st")) arg = "strikethrough";
            
            if (decorationOptions.contains(arg)) {
                decorations.add(arg);
            } else if (animationOptions.contains(arg)) {
                animation = arg;
            } else {
                try {
                    speed = Math.max(1, Math.min(10, Integer.parseInt(arg)));
                } catch (NumberFormatException ignored) {}
            }
        }

        CompletableFuture<Void> update1 = database.updateCustomGradient(player.getName(), colors);
        CompletableFuture<Void> update2 = database.updateGradientAnimation(player.getName(), animation);
        CompletableFuture<Void> update3 = database.updateGradientSpeed(player.getName(), speed);
        
        String decorationsStr = decorations.isEmpty() ? null : String.join(",", decorations);
        CompletableFuture<Void> update4 = database.upsertPlayer(player.getName(), "nameDecorations", decorationsStr);
        
        CompletableFuture.allOf(update1, update2, update3, update4).thenRun(() -> refreshPlayer(player));
        
        sendPrefixedLocalizedMessage(player, "gradient_success");
    }

    private void refreshPlayer(Player player) {
        Main plugin = (Main) Main.getInstance();
        player.getScheduler().run(plugin, (task) -> { 
           me.txmc.core.Section section = plugin.getSectionByName("TabList");
           if (section instanceof me.txmc.core.tablist.TabSection tabSection) {
               tabSection.setTab(player, true);
           }
        }, null);
    }

    @Override
    public List<String> onTab(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("#FFFFFF:#FF0000", "#00FF00:#0000FF");
        } else if (args.length > 1) {
            List<String> suggestions = new ArrayList<>(decorationOptions);
            suggestions.addAll(animationOptions);
            return suggestions.stream()
                    .filter(option -> option.startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player)  sendPrefixedLocalizedMessage(player, "nc_no_permission");
    }
}
