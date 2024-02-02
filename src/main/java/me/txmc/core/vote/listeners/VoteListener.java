package me.txmc.core.vote.listeners;

import com.vexsoftware.votifier.model.VotifierEvent;
import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;
import me.txmc.core.vote.VoteSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author 254n_m
 * @since 2023/12/20 6:58 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class VoteListener implements Listener {
    private final VoteSection main;

    @EventHandler
    public void onVote(VotifierEvent event) {
        String username = event.getVote().getUsername();
        Bukkit.getOnlinePlayers().stream().filter(p -> !p.getName().equals(username)).forEach(p -> GlobalUtils.sendPrefixedLocalizedMessage(p, "vote_announcement", username));
        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            main.registerOfflineVote(username);
            return;
        }
        main.rewardPlayer(player);
    }
}
