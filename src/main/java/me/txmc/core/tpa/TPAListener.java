package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
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

    private void handleTPARequest(@NotNull CommandSender sender, String unverifiedCommand){
        if(unverifiedCommand != "tpa" || unverifiedCommand != "tpahere"){
            return;
        }else{
            private Set<Player> recipients = event.getRecipients();
            private recipient[] = recipients.toArray();
            private ToggledPlayer recipient = recipient[0];
            if(recipient.isToggledOff){
                sendMessage("This player has TPA requests toggled off.")
                return;
            }else{if(unverifiedCommand = "tpahere"){
                Bukkit.getServer().dispatchCommand(sender, tpahere recipient True );
            }else
                Bukkit.getServer().dispatchCommand(sender, tpa recipient True );
            }
        }
    }
}