package me.txmc.core.util;

import me.txmc.core.Main;
import org.bukkit.ChatColor;

import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/17 9:55 PM
 * This file was created as a part of 8b8tCore
 */
public class GlobalUtils {
    public static void log(Level level, String format, Object... args) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        String message = String.format(format, args);
        message = translateChars(message);
        Main.getInstance().getLogger().log(level, String.format("%s%c%s", message, Character.MIN_VALUE, element.getClassName()));
    }
    public static String translateChars(String input) {
        //LegacyComponentSerializer
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
