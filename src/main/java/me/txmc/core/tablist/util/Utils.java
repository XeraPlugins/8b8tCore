package me.txmc.core.tablist.util;

import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
 */
public class Utils {

    public static CompletableFuture<Component> parsePlaceHolders(String input, Player player, long startTime) {
        CompletableFuture<Component> future = new CompletableFuture<>();
        GlobalUtils.getTpsNearEntity(player).thenAccept(tps -> {
            String strTps = (tps >= 20.0) ? String.format("%s20.00", "<green>") : String.format("%s%.2f", GlobalUtils.getTPSColor(tps), tps);
            
            double mspt = GlobalUtils.getCurrentRegionMspt();
            String strMspt = String.format("%s%.1f", GlobalUtils.getMSPTColor(mspt), mspt);
            
            String uptime = Utils.getFormattedInterval(System.currentTimeMillis() - startTime);
            String online = String.valueOf(Bukkit.getOnlinePlayers().size());
            String ping = String.valueOf(player.getPing());
            
            String parsed = input.replace("%tps%", strTps)
                                .replace("%mspt%", strMspt)
                                .replace("%players%", online)
                                .replace("%ping%", ping)
                                .replace("%uptime%", uptime);
                                
            future.complete(GlobalUtils.translateChars(parsed));
        });
        return future;
    }
    public static String getFormattedInterval(long ms) {
        long seconds = ms / 1000L % 60L;
        long minutes = ms / 60000L % 60L;
        long hours = ms / 3600000L % 24L;
        long days = ms / 86400000L;
        return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

}
