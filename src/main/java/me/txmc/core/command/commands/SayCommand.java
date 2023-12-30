package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class SayCommand extends BaseCommand {

    public SayCommand() {
        super("say", "/say <message>", "8b8tcore.command.say", "Configurable say command");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            TextComponent msg = GlobalUtils.translateChars(String.join(" ", args));
            Bukkit.getOnlinePlayers().forEach(p -> GlobalUtils.sendPrefixedComponent(p, msg));
            GlobalUtils.sendPrefixedComponent(Bukkit.getConsoleSender(), msg);
        } else sendErrorMessage(sender, "Message cannot be blank");
    }
}