package me.txmc.core.chat.commands;

import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * CommandExecutor implementation for the /ignorelist command.
 * <p>This command allows players to view a list of other players they are currently ignoring.</p>
 *
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/07/13 6:18 PM
 */
public class IgnoreListCommand implements CommandExecutor {
    private final ChatSection manager;

    public IgnoreListCommand(ChatSection manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            ChatInfo info = manager.getInfo(player);
            if (info != null) {
                if (!info.getIgnoring().isEmpty()) {
                    String ignoredList = info.getIgnoring().stream()
                            .map(uuid -> Bukkit.getServer().getOfflinePlayer(uuid).getName())
                            .filter(name -> name != null && !name.isEmpty())
                            .collect(Collectors.joining("&3, &c"));
                    sendPrefixedLocalizedMessage(player, "ignorelist_successful", ignoredList);
                } else sendPrefixedLocalizedMessage(player, "ignorelist_not_ignoring");
            } else sendPrefixedLocalizedMessage(player, "ignorelist_failed");
        } else sendMessage(sender, "You must be a player to use this command.");
        return true;
    }
}
