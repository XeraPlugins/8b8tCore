package me.txmc.core.tpa.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.TPASection;
import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 *
 *  @author 5aks
 *  @since  7/16/2025 1:00 PM
 *  This file was created as a part of 8b8tCore
 * 
 */

@RequiredArgsConstructor
public class TPABlockCommand implements CommandExecutor{
    private final TPASection main;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args){
        if(sender instanceof Player from){
            if(args.length == 1){
                Player blocked = Bukkit.getPlayer(args[0]);
                if(main.checkBlocked(from, blocked)){
                    sendPrefixedLocalizedMessage(from, "tpa_requests_unblocked", blocked.getName());
                    main.removeBlockedPlayer(from, blocked);
                } else {
                    sendPrefixedLocalizedMessage(from, "tpa_requests_blocked", blocked.getName());
                    main.addBlockedPlayer(from, blocked);
                } 
            }else{
                sendPrefixedLocalizedMessage(from, "tpa_block_syntax");
            }
        } else{
            sendMessage(sender, "You must be a player.");
        }
        return true;
    }
}