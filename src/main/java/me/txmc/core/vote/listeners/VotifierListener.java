package me.txmc.core.vote.listeners;

import me.txmc.core.vote.VoteSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.vexsoftware.votifier.model.VotifierEvent;

/**
 * Votifier Listener for 8b8t handling incoming vote events from vote sites.
 * @author MindComplexity 
 * @since 2025/08/30
 * This file was created as a part of 8b8tCore
 */

public class VotifierListener implements Listener {
    private final VoteSection voteSection;

    public VotifierListener(VoteSection voteSection) {
        this.voteSection = voteSection;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        String username = event.getVote().getUsername().toLowerCase();
        Player player = Bukkit.getPlayerExact(username);        
        boolean voteAccepted = voteSection.registerVote(username);
        
        if (!voteAccepted) {
            if (player != null && player.isOnline()) {
                long remainingDays = voteSection.getRemainingVoterDays(username);
                player.sendMessage("§cYou already have the voter role! It expires in " + remainingDays + " days.");
            }
            return;
        }
        
        if (player != null && player.isOnline()) {
            voteSection.rewardPlayer(player);
        } else {
            voteSection.announceVote(username);
        }
    }
}
