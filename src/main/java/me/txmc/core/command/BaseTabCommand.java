package me.txmc.core.command;

import java.util.List;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/
public abstract class BaseTabCommand extends BaseCommand {

    public BaseTabCommand(String name, String usage, String permission) {
        super(name, usage, permission);
    }

    public BaseTabCommand(String name, String usage, String permission, String description) {
        super(name, usage, permission, description);
    }
    public BaseTabCommand(String name, String usage, String permission, String description, String[] subCommands) {
        super(name, usage, permission, description, subCommands);
    }
    public BaseTabCommand(String name, String usage, String[] permissions, String description, String[] subCommands) {
        super(name, usage, permissions, description, subCommands);
    }

    public abstract List<String> onTab(org.bukkit.command.CommandSender sender, String[] args);
}
