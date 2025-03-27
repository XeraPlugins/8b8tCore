package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class GmSpectatorCommand extends BaseCommand {

    public GmSpectatorCommand() {
        super(
                "gmsp",
                "/gmsp",
                "8b8tcore.command.gmsp",
                "Simple way to change your gamemode to Spectator");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = getSenderAsPlayer(sender).orElse(null);
        if (player != null) {
            if (player.hasPermission("8b8tcore.command.gmsp") || player.isOp() || player.hasPermission("*")) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else sendErrorMessage(sender, PLAYER_ONLY);
    }
}
