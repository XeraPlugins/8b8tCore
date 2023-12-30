package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class SpeedCommand extends BaseCommand {

    public SpeedCommand() {
        super(
                "speed",
                "/speed <number>",
                "8b8tcore.command.speed",
                "Turn up your fly speed");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        getSenderAsPlayer(sender).ifPresentOrElse(player -> {
            try {
                if (args.length > 0) {
                    float speed = Float.parseFloat(args[0]);
                    if (speed <= 1) {
                        player.setFlySpeed(speed);
                        sendMessage(player, "&3Fly speed set to&r&a %f", speed);
                    } else sendMessage(player, "Flying speed must not be above 1");
                } else {
                    sendMessage(sender, "&3Please note that the default flight speed is&r&a 0.1");
                    sendErrorMessage(sender, getUsage());
                }
            } catch (NumberFormatException e) {
                sendErrorMessage(player, getUsage());
            }
        }, () -> sendErrorMessage(sender, PLAYER_ONLY));
    }
}
