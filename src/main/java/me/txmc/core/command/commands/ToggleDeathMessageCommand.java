package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class ToggleDeathMessageCommand extends BaseCommand {
    private final GeneralDatabase database;

    public ToggleDeathMessageCommand(Main plugin) {
        super("deathmessage", "/deathmessage", "8b8tcore.command.deathmessage");
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        boolean current = database.getPlayerHideDeathMessages(player.getName());
        boolean newValue = !current;
        database.updateHideDeathMessages(player.getName(), newValue);

        if (newValue) {
            sendMessage(player, "&aYou will no longer see death messages.");
        } else {
            sendMessage(player, "&aYou will now see death messages.");
        }
    }
}