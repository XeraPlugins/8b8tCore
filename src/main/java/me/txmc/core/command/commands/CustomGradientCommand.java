package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

public class CustomGradientCommand extends BaseCommand {
    private final GeneralDatabase database;

    public CustomGradientCommand(Main plugin) {
        super("customgradient", "/customgradient <#hex:#hex...>", "8b8tcore.prefix.custom");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }

        if (args.length == 0) {
            sendPrefixedLocalizedMessage(player, "gradient_usage", "/customgradient <#hex:#hex...> [animation] [speed]");
            return;
        }

        String colors = args[0];
        String animation = "none";
        int speed = 5;

        if (args.length >= 2) {
            animation = args[1].toLowerCase();
            List<String> validAnimations = List.of("wave", "pulse", "rainbow", "shift", "none");
            if (!validAnimations.contains(animation)) {
                sendPrefixedLocalizedMessage(player, "gradient_animation_invalid", animation);
                return;
            }
        }
        if (args.length >= 3) {
            try {
                speed = Math.max(1, Math.min(10, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {}
        }

        if (!colors.matches("^#[0-9A-Fa-f]{6}(:#[0-9A-Fa-f]{6})*$")) {
            sendPrefixedLocalizedMessage(player, "gradient_invalid", colors);
            return;
        }

        database.updateCustomGradient(player.getName(), colors);
        database.updateGradientAnimation(player.getName(), animation);
        database.updateGradientSpeed(player.getName(), speed);
        
        refreshPlayer(player);
        
        sendPrefixedLocalizedMessage(player, "gradient_success");
    }

    private void refreshPlayer(Player player) {
        Main plugin = (Main) Main.getInstance();
        player.getScheduler().run(plugin, (task) -> {
           me.txmc.core.Section section = plugin.getSectionByName("TabList");
           if (section instanceof me.txmc.core.tablist.TabSection tabSection) {
               tabSection.setTab(player);
           }
        }, null);
    }
}
