package me.txmc.core.util;

import lombok.Cleanup;
import lombok.Getter;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import java.util.Random;

/**
 * @author 254n_m
 * @since 2023/12/17 9:55 PM
 * This file was created as a part of 8b8tCore
 */
public class GlobalUtils {
    @Getter private static final String PREFIX = Main.prefix;

    public static void info(String format) {
        log(Level.INFO, format);
    }

    public static void log(Level level, String format, Object... args) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        String message = String.format(format, args);
        message = getStringContent(translateChars(message));
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

    public static void sendDeathMessage(String key, String victim, String killer, String weapon) {
        try {
            Localization locEnglish = Localization.getLocalization("en");
            List<TextComponent> deathListMessages = locEnglish.getStringList(key)
                    .stream().map(GlobalUtils::translateChars).toList();

            if (deathListMessages == null) {
                return;
            }
            int msgIndex = 0;
            if(deathListMessages.size() > 1){
                Random random = new Random();
                msgIndex = random.nextInt(deathListMessages.size());
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                Localization loc = Localization.getLocalization(p.locale().getLanguage());
                List<TextComponent> deathMessages = loc.getStringList(key)
                        .stream()
                        .map(s -> s.replace("%victim%", victim))
                        .map(s -> s.replace("%killer%", killer))
                        .map(s -> s.replace("%kill-weapon%", weapon))
                        .map(GlobalUtils::translateChars).toList();

                TextComponent msg = deathMessages.get(msgIndex);
                p.sendMessage(msg);
            }
        } catch (Throwable ignored){};

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
    public static void executeCommand(String command, Object... args) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(command, args));
    }
    public static void removeElytra(Player player) {
        ItemStack chestPlate = player.getInventory().getChestplate();
        if (chestPlate == null) return;
        if (chestPlate.getType() == Material.AIR) return;
        if (chestPlate.getType() == Material.ELYTRA) {
            PlayerInventory inventory = player.getInventory();
            if (inventory.firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), chestPlate);
            } else inventory.setItem(inventory.firstEmpty(), chestPlate);
            ItemStack[] buffer = inventory.getArmorContents();
            buffer[2] = null;
            inventory.setArmorContents(buffer);
        }
    }
    public static String getStringContent(Component component) {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        return serializer.serialize(component);
    }
    public static CompletableFuture<Double> getTpsNearEntity(Entity entity) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        entity.getScheduler().run(Main.getInstance(), (st) -> future.complete(getCurrentRegionTps()), () -> {});
        return future;
    }
    public static CompletableFuture<Double> getRegionTps(Location location) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        Bukkit.getRegionScheduler().run(Main.getInstance(),location, (st) -> future.complete(getCurrentRegionTps()));
        return future;
    }

    public static double getCurrentRegionTps() {
        try {
            Object region = Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler").getDeclaredMethod("getCurrentRegion").invoke(null);
            if (region != null) {
                Object tickData = region.getClass().getDeclaredMethod("getData").invoke(region);
                Object regionShceduleHandle = tickData.getClass().getDeclaredMethod("getRegionSchedulingHandle").invoke(tickData);
                Object tickReport = regionShceduleHandle.getClass().getMethod("getTickReport15s", long.class).invoke(regionShceduleHandle, System.nanoTime());
                Object segmentedAvg = tickReport.getClass().getDeclaredMethod("tpsData").invoke(tickReport);
                Object segAll = segmentedAvg.getClass().getDeclaredMethod("segmentAll").invoke(segmentedAvg);
                return (double) segAll.getClass().getDeclaredMethod("average").invoke(segAll);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return -1;
    }
    public static String formatLocation(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
}
