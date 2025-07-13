package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.tpa.ToggledPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import me.txmc.core.util.GlobalUtils.sendMessage;
//import me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 5aks
 * @since 7/13/2025 1:30AM
 * This file was created as a part of 8b8tCore
 */

@RequiredArgsConstructor
public class TPAToggleCommand implements CommandExecutor {
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args){
        if (!(sender instanceof Player from)) {
            //sendMessage(sender, "&cYou must be a player");
            return true;
        }

        if (args.length != 0) {
            //sendMessage(sender, "Do not provide any arguments.");
        }else{
            Player targetPlayer = (Player) sender;
            ToggledPlayer target = new ToggledPlayer(targetPlayer);
            target.toggle();
            //sendMessage(sender, "TPA successfully toggled.");
        }
        return true;
    }

}