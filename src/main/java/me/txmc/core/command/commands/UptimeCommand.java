package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import me.txmc.core.tablist.util.Utils;
import org.bukkit.command.CommandSender;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class UptimeCommand extends BaseCommand {
    private final Main plugin;
    public UptimeCommand(Main plugin) {
        super(
                "uptime",
                "/uptime",
                "8b8tcore.command.uptime",
                "Show the uptime of the server");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sendMessage(sender, "&3The server has had&r&a %s&r&3 uptime", Utils.getFormattedInterval(System.currentTimeMillis() - plugin.getStartTime()));
    }
}
