package me.txmc.core.tablist.util;

import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2026/03/22
 * This file was created as a part of 8b8tCore
*/

public class Utils {

    public static Component parsePlaceHolders(String input, Player player, long startTime) {
        if (input == null || input.isEmpty()) return Component.empty();

        double tps;
        try {
            double[] regionTpsArr = GlobalUtils.getTpsNearEntitySync(player);
            tps = (regionTpsArr != null && regionTpsArr.length > 0) ? regionTpsArr[0] : 20.0;
        } catch (Throwable t) {
            tps = 20.0;
        }

        String tpsColor = tps >= 18.0 ? "<green>" : tps >= 13.0 ? "<yellow>" : "<red>";
        String tpsStr = tps >= 20.0 ? "20.00" : String.format("%.2f", tps);

        double mspt = GlobalUtils.getCurrentRegionMspt();
        if (mspt <= 0 && tps > 0) mspt = (1000.0 / Math.min(tps, 20.0));
        String msptColor = mspt < 60 ? "<green>" : mspt <= 100 ? "<yellow>" : "<red>";
        String msptStr = String.format("%.1f", mspt);

        String uptime = getFormattedInterval(System.currentTimeMillis() - startTime);

        String result = input
            .replace("%tps%", tpsColor + tpsStr)
            .replace("%mspt%", msptColor + msptStr)
            .replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("%ping%", String.valueOf(player.getPing()))
            .replace("%uptime%", uptime);

        return GlobalUtils.translateChars(result);
    }

    public static String getFormattedInterval(long ms) {
        long seconds = ms / 1000L % 60L;
        long minutes = ms / 60000L % 60L;
        long hours = ms / 3600000L % 24L;
        long days = ms / 86400000L;
        return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }
}
