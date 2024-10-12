package me.txmc.core.command.commands;

import me.txmc.core.Localization;
import me.txmc.core.command.BaseCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.translateChars;

public class HelpCommand extends BaseCommand {
    public HelpCommand() {
        super("help", "/help", "8b8tcore.command.help", "Displays a custom help menu");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Localization loc = Localization.getLocalization(player.locale().getLanguage());
            Component helpMsg = translateChars(String.join("\n", loc.getStringList("HelpMessage").toArray(String[]::new)));
            player.sendMessage(helpMsg);
        } else sendMessage(sender, "&cYou must be a player");
    }
}
