package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.command.CommandSender;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class DiscordCommand extends BaseCommand {
    private final CommandSection main;

    public DiscordCommand(CommandSection main) {
        super(
                "discord",
                "/discord",
                "8b8tcore.command.discord",
                "Shows a discord link");
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sendMessage(sender, main.getConfig().getString("Discord"));
    }
}
