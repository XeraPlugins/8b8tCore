package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            sendPrefixedLocalizedMessage(player, "gradient_usage");
            return;
        }

        String gradient = String.join(":", args).replace(" ", "");
        // Basic validation: check if contains # and at least one hex
        if (!gradient.contains("#")) {
            sendPrefixedLocalizedMessage(player, "gradient_invalid");
            return;
        }

        database.updateCustomGradient(player.getName(), gradient);
        sendPrefixedLocalizedMessage(player, "gradient_success", gradient);
    }
}
