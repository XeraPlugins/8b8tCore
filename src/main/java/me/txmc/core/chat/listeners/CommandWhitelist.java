package me.txmc.core.chat.listeners;

import lombok.RequiredArgsConstructor;
import me.txmc.core.chat.ChatSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

@RequiredArgsConstructor
public class CommandWhitelist implements Listener {
    private final ChatSection main;
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().isOp()) return;
        List<String> allowedCommands = main.getConfig().getStringList("CommandWhitelist");
        String command = event.getMessage().split(" ")[0];
        event.setCancelled(allowedCommands.stream().noneMatch(a -> a.equalsIgnoreCase(command)));
        if (event.isCancelled()) sendPrefixedLocalizedMessage(event.getPlayer(), "cmdwhitelist_cmd_not_allowed", command);
    }
}
