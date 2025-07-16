package me.txmc.core.tpa.commands;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.tpa.ToggledPlayer;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.txmc.core.util.GlobalUtils.*;
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
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cYou must be a player");
            return true;
        }
        Player targetPlayer = (Player) sender;
        ToggledPlayer player = new ToggledPlayer(targetPlayer, main);
        if (args.length != 0) {
            sendPrefixedLocalizedMessage(targetPlayer, "tpatoggle_syntax");
            return true;
        }else{
            player.toggle();
            if(player.isToggledOff()){
                sendPrefixedLocalizedMessage(targetPlayer, "tpa_requests_disabled");
            }else sendPrefixedLocalizedMessage(targetPlayer, "tpa_requests_enabled");
            
        }
        return true;
    }

}