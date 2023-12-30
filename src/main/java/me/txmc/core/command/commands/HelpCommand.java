package me.txmc.core.command.commands;

import me.txmc.core.Localization;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.translateChars;

/**
 * @author 254n_m
 * @since 2023/12/20 6:18 PM
 * This file was created as a part of 8b8tCore
 */
public class HelpCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player player) {
            Localization loc = Localization.getLocalization(player.locale().getLanguage());
            TextComponent helpMsg = translateChars(String.join("\n", loc.getStringList("HelpMessage").toArray(String[]::new)));
            player.sendMessage(helpMsg);
        } else sendMessage(sender, "&cYou must be a player");
        return true;
    }
}
