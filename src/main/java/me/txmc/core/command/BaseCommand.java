package me.txmc.core.command;

import lombok.Getter;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

@Getter
public abstract class BaseCommand {
    protected final String CONSOLE_ONLY = "This command is console only";
    protected final String PLAYER_ONLY = "This command is player only";
    protected final String PREFIX = GlobalUtils.getPREFIX();
    private final String name;
    private final String usage;
    private final String[] permissions;
    private final String description;
    private final String[] subCommands;

    public BaseCommand(String name, String usage, String permission) {
        this(name, usage, permission, null, null);
    }

    public BaseCommand(String name, String usage, String permission, String description) {
        this(name, usage, permission, description, null);
    }
    public BaseCommand(String name, String usage, String[] permission, String description) {
        this(name, usage, permission, description, null);
    }

    public BaseCommand(String name, String usage, String permission, String description, String[] subCommands) {
        this.name = name;
        this.usage = usage;
        this.permissions = new String[]{permission};
        this.description = description;
        this.subCommands = subCommands;
    }

    public BaseCommand(String name, String usage, String[] permissions, String description, String[] subCommands) {
        this.name = name;
        this.usage = usage;
        this.permissions = permissions;
        this.description = description;
        this.subCommands = subCommands;
    }
    public void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "&cYou are lacking the permission&r&a %s", String.join(", ", getPermissions()));
    }

    public void sendErrorMessage(CommandSender sender, String message) {
        sendMessage(sender, String.format("&c%s", message));
    }

    public Optional<Player> getSenderAsPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return Optional.of((Player) sender);
        } else return Optional.empty();
    }

    public String getPermission() {
        return (permissions != null && permissions.length > 1) ? permissions[0] : (permissions != null && permissions.length == 1) ? permissions[0] : null;
    }

    public abstract void execute(CommandSender sender, String[] args);
}