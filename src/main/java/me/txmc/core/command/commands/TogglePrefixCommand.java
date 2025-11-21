package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class TogglePrefixCommand extends BaseCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GeneralDatabase database;
    private final PrefixManager prefixManager = new PrefixManager();

    public TogglePrefixCommand(Main plugin) {
        super("toggleprefix", "/toggleprefix", "8b8tcore.command.toggleprefix");
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        if (!prefixManager.hasRank(player)) {
            sendMessage(player, "&cYou must have a rank to hide your prefix.");
            return;
        }

        boolean current = database.getPlayerHidePrefix(player.getName());
        boolean newValue = !current;
        database.updateHidePrefix(player.getName(), newValue);

        String tag = newValue ? "" : prefixManager.getPrefix(player);
        if (tag.isEmpty()) {
            player.playerListName(player.displayName());
        } else {
            player.playerListName(miniMessage.deserialize(tag).append(player.displayName()));
        }

        if (newValue) {
            sendMessage(player, "&aYour prefix is now hidden.");
        } else {
            sendMessage(player, "&aYour prefix is now visible.");
        }
    }
}