package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.commands.*;
import me.txmc.core.tpa.ToggledPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 5aks
 * @since 7/13/2025 2:15am
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPAListener implements Listener {
    private final TPASection main;

    @EventHandler
    private void onTPARequst(PlayerCommandPreprocessEvent event){
        String[] args = event.getMessage().split(" ");
        String command = args[0].toLowerCase();
        if(args.length < 3) return;
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        handleTPARequest(event.getPlayer(), command, targetName, event);
    }


    private void handleTPARequest(@NotNull CommandSender sender, String unverifiedCommand, String recipient, PlayerCommandPreprocessEvent event){
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if(unverifiedCommand != "tpa" || unverifiedCommand != "tpahere"){
            return;
        }else{
            Player from = (Player) sender;
            Player targetPlayer = Bukkit.getPlayerExact(recipient);
            ToggledPlayer target = (ToggledPlayer) targetPlayer;
            
            if(target.isToggledOff()){
                //sendMessage("This player has TPA requests toggled off.");
                if(main.hasRequested(from, targetPlayer)) main.removeRequest(from, targetPlayer);
                if(main.hasHereRequested(from, targetPlayer)) main.removeHereRequest(from, targetPlayer);
                event.setCancelled(true);
                return;
            }else{
                if(unverifiedCommand == "tpahere"){

                    Bukkit.getServer().dispatchCommand(sender, ("tpahere " + targetPlayer + " 0") );
                    return;
                }else{
                    Bukkit.getServer().dispatchCommand(sender, ("tpa " + targetPlayer + " 0") );
                    return;
                }
            }
        }
    }
}