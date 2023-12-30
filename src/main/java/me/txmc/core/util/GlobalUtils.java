package me.txmc.core.util;

import lombok.Cleanup;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/17 9:55 PM
 * This file was created as a part of 8b8tCore
 */
public class GlobalUtils {
    private static final String PREFIX = "&7&r&b&38b8t&r&aCore&r&7&r";

    public static void log(Level level, String format, Object... args) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        String message = String.format(format, args);
        message = translateChars(message).content();
        Main.getInstance().getLogger().log(level, String.format("%s%c%s", message, Character.MIN_VALUE, element.getClassName()));
    }

    public static TextComponent translateChars(String input) {
        return LegacyComponentSerializer.legacy('&').deserialize(input);
    }

    public static void sendMessage(CommandSender obj, String message, Object... args) {
        sendOptionalPrefixMessage(obj, message, true, args);
    }

    public static void sendOptionalPrefixMessage(CommandSender obj, String msg, boolean prefix, Object... args) {
        if (prefix) msg = String.format("%s &7>>&r %s", PREFIX, msg);
        msg = String.format(msg, args);
        obj.sendMessage(translateChars(msg));
    }
    public static void sendPrefixedLocalizedMessage(Player player, String key, Object... args) {
        sendLocalizedMessage(player, key, true, args);
    }

    public static void sendLocalizedMessage(Player player, String key, boolean prefix, Object... args) {
        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        String msg = String.format(loc.get(key), args);
        if (prefix) msg = PREFIX.concat(" &r&7>>&r ").concat(msg);
        player.sendMessage(translateChars(msg));
    }
    public static void sendPrefixedComponent(CommandSender target, Component component) {
        target.sendMessage(translateChars(String.format("%s &7>>&r ", PREFIX)).append(component));
    }
    public static void unpackResource(String resourceName, File file) {
        if (file.exists()) return;
        try {
            @Cleanup InputStream is = Main.class.getClassLoader().getResourceAsStream(resourceName);
            if (is == null)
                throw new NullPointerException(String.format("Resource %s is not present in the jar", resourceName));
            Files.copy(is, file.toPath());
        } catch (Throwable t) {
            log(Level.SEVERE, "&cFailed to extract resource from jar due to &r&3 %s&r&c! Please see the stacktrace below for more info", t.getMessage());
            t.printStackTrace();
        }
    }
    public static void executeCommand(String command, String... args) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(command, args));
    }
}
