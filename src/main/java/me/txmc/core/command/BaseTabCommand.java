package me.txmc.core.command;

import java.util.List;

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

    public abstract List<String> onTab(String[] args);
}