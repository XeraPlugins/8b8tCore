package me.txmc.core.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.command.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommandHandler implements TabExecutor {
    @Getter private final ArrayList<BaseCommand> commands = new ArrayList<>();
    private final CommandSection main;

    public void registerCommands() {
        addCommand(new BaseCmd(main));
        addCommand(new DiscordCommand(main));
        addCommand(new MatrixCommand(main));
        addCommand(new HelpCommand());
        addCommand(new OpenInv());
        addCommand(new SayCommand());
        addCommand(new SpawnCommand());
        addCommand(new SpeedCommand());
        addCommand(new UptimeCommand(main.getPlugin()));
        addCommand(new WorldSwitcher());
        addCommand(new TpsinfoCommand(main.getPlugin()));
        addCommand(new RepairCommand(main.getPlugin()));
        addCommand(new NickCommand(main.getPlugin()));
        addCommand(new ToggleJoinMessagesCommand(main.getPlugin()));
        addCommand(new ItemColorCommand());
        addCommand(new NameColorCommand(main.getPlugin()));
        addCommand(new RenameCommand());
        addCommand(new SignCommand());
        addCommand(new ShadowMuteCommand(main.getPlugin()));
        addCommand(new VanishCommand(main.getPlugin()));
    }

    private void addCommand(BaseCommand command) {
        commands.add(command);
        PluginCommand pluginCommand = main.getPlugin().getCommand(command.getName());
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        for (BaseCommand command : commands) {
            if (!command.getName().equalsIgnoreCase(cmd.getName())) continue;

            boolean hasPermission = false;
            for (String permission : command.getPermissions()) {
                if (sender.hasPermission(permission) || sender.isOp() || sender.hasPermission("*")) {
                    hasPermission = true;
                    break;
                }
            }

            if (hasPermission) {
                command.execute(sender, args);
            } else {
                command.sendNoPermission(sender);
            }

            break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        for (BaseCommand command : commands) {
            if (!command.getName().equalsIgnoreCase(cmd.getName())) continue;
            if (command instanceof BaseTabCommand tabCommand) return tabCommand.onTab(args);
            if (command.getSubCommands() != null && args.length == 1) {
                return Arrays.stream(command.getSubCommands()).map(s -> s.split("::")[0]).filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            } else if (args.length > 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
            } else return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.singletonList("");
    }
}