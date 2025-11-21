package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class ToggleBadgesCommand extends BaseCommand {
    private final GeneralDatabase database;

    public ToggleBadgesCommand(Main plugin) {
        super("badges", "/badges", "8b8tcore.command.badges");
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        boolean current = database.getPlayerHideBadges(player.getName());
        boolean newValue = !current;
        database.updateHideBadges(player.getName(), newValue);

        if (newValue) {
            sendMessage(player, "&aYou will no longer see other players' advancements.");
        } else {
            sendMessage(player, "&aYou will now see other players' advancements.");
        }
    }
}