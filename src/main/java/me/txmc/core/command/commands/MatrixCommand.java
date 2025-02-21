package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.command.CommandSender;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class MatrixCommand extends BaseCommand {
    private final CommandSection main;

    public MatrixCommand(CommandSection main) {
        super(
                "matrix",
                "/matrix",
                "8b8tcore.command.matrix",
                "Shows a matrix link");
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sendMessage(sender, main.getConfig().getString("Matrix"));
    }
}
