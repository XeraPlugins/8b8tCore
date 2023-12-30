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
                new String[]{"reload::Reload the config file", "version::Show the version of the plugin", "help::Shows the help for the plugin"});
        this.main = main;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "reload":
                    main.getPlugin().reloadConfig();
                    sendMessage(sender, "&aSuccessfully reloaded configuration file");
                    break;
                case "version":
                    sendMessage(sender, "&3Version &r&c" + main.getPlugin().getPluginMeta().getVersion());
                    break;
                case "help":
                    sendMessage(sender, "&1---&r " + PREFIX + " &6Help &r&1---");
                    main.getCommandHandler().getCommands().forEach(command -> {
                        String helpMsg = "&1---&r&3&l /" + command.getName() + "&r&6 Help &r&1---";
                        sendMessage(sender, helpMsg);
                        sendMessage(sender, "&3Description: " + command.getDescription());
                        if (command.getSubCommands() != null) {
                            if (command.getSubCommands().length > 0) {
                                for (String subCommand : command.getSubCommands()) {
                                    String[] split = subCommand.split("::");
                                    if (split.length > 0) {
                                        sendMessage(sender, "&6/" + command.getName() + " " + split[0] + " |&r&e " + split[1]);
                                    } else sendMessage(sender, "&6/" + command.getName() + " " + subCommand);
                                }
                                sendMessage(sender, "&1--------------------");
                            }
                        }
                        sendMessage(sender, "");
                    });
                    break;
                default:
                    sendMessage(sender, "&cUnknown command&r&3 %s&r", args[0]);
                    break;
            }
        } else sendErrorMessage(sender, getUsage());
    }
}
