package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SuicideCommand extends BaseCommand {
    public SuicideCommand() {
        super("suicide",
                "/suicide",
                "8b8tcore.command.suicide",
                "Command to commit suicide");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sendErrorMessage(sender, PLAYER_ONLY);
            return;
        }

        Player player = (Player) sender;
        player.setHealth(0.0);

    }
}
