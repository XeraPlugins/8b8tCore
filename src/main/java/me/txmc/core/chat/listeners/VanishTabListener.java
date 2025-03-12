package me.txmc.core.chat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.Iterator;
import java.util.List;
import me.txmc.core.Main;
import org.bukkit.entity.Player;

public class VanishTabListener implements Listener {
    private final Main main;

    public VanishTabListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onTabComplete(org.bukkit.event.server.TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;

        Player sender = (Player) event.getSender();
        List<String> completions = event.getCompletions();
        Iterator<String> iterator = completions.iterator();

        while (iterator.hasNext()) {
            String name = iterator.next();
            Player target = main.getServer().getPlayerExact(name);

            if (target != null && main.getVanishedPlayers().contains(target.getUniqueId())) {
                if (!sender.hasPermission("*") && !sender.isOp()) {
                    iterator.remove();
                }
            }
        }
    }
}
