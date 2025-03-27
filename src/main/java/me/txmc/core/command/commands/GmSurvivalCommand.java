package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class GmSurvivalCommand extends BaseCommand {

    public GmSurvivalCommand() {
        super(
                "gms",
                "/gms",
                "8b8tcore.command.gms",
                "Simple way to change your gamemode to Survival");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = getSenderAsPlayer(sender).orElse(null);
        if (player != null) {
            if (player.hasPermission("8b8tcore.command.gms") || player.isOp() || player.hasPermission("*")) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        } else sendErrorMessage(sender, PLAYER_ONLY);
    }
}
