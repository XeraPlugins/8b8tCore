package me.txmc.core.vote.listeners;

import me.txmc.core.vote.VoteSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {
    private final VoteSection voteSection;

    public VoteListener(VoteSection voteSection) {
        this.voteSection = voteSection;
    }

    @EventHandler
    public void onVote(VotifierEvent event) {
        String username = event.getVote().getUsername();
        Player player = Bukkit.getPlayerExact(username);
        if (player != null && player.isOnline()) {
            voteSection.rewardPlayer(player);
        }
    }
}
