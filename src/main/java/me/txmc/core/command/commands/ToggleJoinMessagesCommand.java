package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Command to toggle the visibility of join messages for a player.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Servers.</p>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2025/03/01 10:00 AM
 */
public class ToggleJoinMessagesCommand extends BaseCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GeneralDatabase database;

    public ToggleJoinMessagesCommand(Main plugin) {
        super("togglejoinmessages", "/togglejoinmessages", "8b8tcore.command.togglejoinmessages");
        this.database = GeneralDatabase.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        boolean currentValue = database.getPlayerShowJoinMsg(player.getName());

        boolean newValue = !currentValue;

        database.updateShowJoinMsg(player.getName(), newValue);

        String messageKey = newValue ? "show_join_leave_messages_true" : "show_join_leave_messages_false";
        sendPrefixedLocalizedMessage(player, messageKey);
    }

    @Override
    public void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player player) {
            sendPrefixedLocalizedMessage(player, "toggleJoinmessages_no_permission");
        }
    }
}
