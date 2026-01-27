package me.txmc.core.command.commands;

import me.txmc.core.command.BaseCommand;
import me.txmc.core.command.CommandSection;
import org.bukkit.command.CommandSender;

import static me.txmc.core.util.GlobalUtils.sendMessage;

public class BaseCmd extends BaseCommand {
    private final CommandSection main;

    public BaseCmd(CommandSection main) {
        super("lef",
                "/lef reload | version | help",
                "8b8tcore.command.lef",
                "Base command of the plugin",
                new String[]{
                    "reload::Reloads all plugin sections and config", 
                    "version::Displays current build version", 
                    "help::Shows this diagnostic menu"
                });
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendErrorMessage(sender, getUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "version" -> sendMessage(sender, "&3Version &r&c%s", main.getPlugin().getPluginMeta().getVersion());
            case "help" -> handleHelp(sender);
            default -> sendMessage(sender, "&cUnknown sub-command:&r&3 %s", args[0]);
        }
    }

    private void handleReload(CommandSender sender) {
        main.getPlugin().reloadConfig();
        sendMessage(sender, "&aSuccessfully reloaded configuration and cleared internal caches.");
    }

    private void handleHelp(CommandSender sender) {
        sendMessage(sender, "&1---&r %s &6Help &r&1---", PREFIX);
        
        main.getCommandHandler().getCommands().forEach(command -> {
            if (command.getPermission() != null && !sender.hasPermission(command.getPermission())) return;

            sendMessage(sender, "&3/%s &7- %s", command.getName(), command.getDescription());
            
            if (command.getSubCommands() != null) {
                for (String sub : command.getSubCommands()) {
                    String[] split = sub.split("::", 2); 
                    if (split.length == 2) {
                        sendMessage(sender, "  &6» &e%s &7| %s", split[0], split[1]);
                    } else {
                        sendMessage(sender, "  &6» &e%s", sub);
                    }
                }
            }
        });
    }
}
