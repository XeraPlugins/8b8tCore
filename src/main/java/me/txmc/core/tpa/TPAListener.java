package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tpa.commands.*;
import org.bukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;


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
        handleTPARequst(event.getPlayer(), event.getMessage());
    }


    private void handleTPARequest(@NotNull CommandSender sender, Command unverifiedCommand){
        if(unverifiedCommand.getName() != "tpa" || unverifiedCommand.getName() != "tpahere"){
            return;
        }else{
            if(unverifiedCommand.args.size() > 1){
                return;
            }else{
                Set<Player> recipients = event.getRecipients();
                Array ecip[] = recipients.toArray();
                private ToggledPlayer recipient = recip[0];
                if(recipient.isToggledOff){
                    sendMessage("This player has TPA requests toggled off.");
                    TPADenyCommand.denyTPA(recipient, sender);
                    return;
                }else{if(unverifiedCommand.getName() = "tpahere"){
                    Bukkit.getServer().dispatchCommand(sender, tpahere, recipient, False );
                    return;
                }else
                    Bukkit.getServer().dispatchCommand(sender, tpa, recipient, False );
                    return;
                }
            }
        }
        return;
    }
}