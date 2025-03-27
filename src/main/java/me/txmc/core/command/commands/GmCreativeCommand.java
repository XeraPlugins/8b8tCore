package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class GmCreativeCommand extends BaseCommand {

    public GmCreativeCommand() {
        super(
                "gmc",
                "/gmc",
                "8b8tcore.command.gmc",
                "Simple way to change your gamemode to Creative");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = getSenderAsPlayer(sender).orElse(null);
        if (player != null) {
            if (player.hasPermission("8b8tcore.command.gmc") || player.isOp() || player.hasPermission("*")) {
                player.setGameMode(GameMode.CREATIVE);
            }
        } else sendErrorMessage(sender, PLAYER_ONLY);
    }
}
