package me.txmc.core.tablist.util;

import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
 */
public class Utils {

    public static Component parsePlaceHolders(String input, Player player, long startTime) {
        double tps;
        try {
            double[] regionTpsArr = GlobalUtils.getTpsNearEntitySync(player);
            tps = (regionTpsArr != null && regionTpsArr.length > 0) ? regionTpsArr[0] : 20.0;
        } catch (Throwable t) {
            tps = 20.0;
        }

        String tpsColorCode = getTPSColorCode(tps);
        String tpsStr = tps >= 20.0 ? "20.00" : String.format("%.2f", tps);

        double mspt = GlobalUtils.getCurrentRegionMspt();
        if (mspt <= 0 && tps > 0) mspt = (1000.0 / Math.min(tps, 20.0));
        String msptColorCode = getMSPTColorCode(mspt);
        String msptStr = String.format("%.1f", mspt);

        String uptime = Utils.getFormattedInterval(System.currentTimeMillis() - startTime);
        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        String ping = String.valueOf(player.getPing());

        String result = input
            .replace("%tps%", tpsColorCode + tpsStr)
            .replace("%mspt%", msptColorCode + msptStr)
            .replace("%players%", online)
            .replace("%ping%", ping)
            .replace("%uptime%", uptime);

        return GlobalUtils.translateChars(result);
    }

    private static String getTPSColorCode(double tps) {
        if (tps >= 18.0) return "<green>";
        if (tps >= 13.0) return "<yellow>";
        return "<red>";
    }

    private static NamedTextColor getTPSColor(double tps) {
        if (tps >= 18.0) return NamedTextColor.GREEN;
        if (tps >= 13.0) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    private static String getMSPTColorCode(double mspt) {
        if (mspt < 60) return "<green>";
        if (mspt <= 100) return "<yellow>";
        return "<red>";
    }

    private static NamedTextColor getMSPTColor(double mspt) {
        if (mspt < 60) return NamedTextColor.GREEN;
        if (mspt <= 100) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    public static String getFormattedInterval(long ms) {
        long seconds = ms / 1000L % 60L;
        long minutes = ms / 60000L % 60L;
        long hours = ms / 3600000L % 24L;
        long days = ms / 86400000L;
        return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }
}
