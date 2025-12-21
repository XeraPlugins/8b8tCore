package me.txmc.core.util;

import lombok.Cleanup;
import lombok.Getter;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.lang.reflect.Method;
import java.util.logging.Level;

import java.util.Random;

/**
 * @author 254n_m
 * @since 2023/12/17 9:55 PM
 * This file was created as a part of 8b8tCore
 */
public class GlobalUtils {
    @Getter private static final String PREFIX = Main.prefix;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static GeneralDatabase database;
    
    // Reflection Cache for Folia TPS/MSPT
    private static java.lang.reflect.Method getCurrentRegionMethod;
    private static java.lang.reflect.Method getDataMethod;
    private static java.lang.reflect.Method getRegionSchedulingHandleMethod;
    private static java.lang.reflect.Method getTickReport15sMethod;
    private static java.lang.reflect.Method tpsDataMethod;
    private static java.lang.reflect.Method msptDataMethod;
    private static java.lang.reflect.Method segmentAllMethod;
    private static java.lang.reflect.Method averageMethod;

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
        return (TextComponent) miniMessage.deserialize(convertToMiniMessageFormat(input));
    }

    public static String convertToMiniMessageFormat(String input) {
        input = input.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");

        input = input.replace("&l", "<bold>");
        input = input.replace("&o", "<italic>");
        input = input.replace("&n", "<underlined>");
        input = input.replace("&m", "<strikethrough>");
        input = input.replace("&k", "<obfuscated>");
        input = input.replace("&r", "<reset>");
        input = input.replace("&0", "<black>");
        input = input.replace("&1", "<dark_blue>");
        input = input.replace("&2", "<dark_green>");
        input = input.replace("&3", "<dark_aqua>");
        input = input.replace("&4", "<dark_red>");
        input = input.replace("&5", "<dark_purple>");
        input = input.replace("&6", "<gold>");
        input = input.replace("&7", "<gray>");
        input = input.replace("&8", "<dark_gray>");
        input = input.replace("&9", "<blue>");
        input = input.replace("&a", "<green>");
        input = input.replace("&b", "<aqua>");
        input = input.replace("&c", "<red>");
        input = input.replace("&d", "<light_purple>");
        input = input.replace("&e", "<yellow>");
        input = input.replace("&f", "<white>");

        return input;
    }
    public static String getTPSColor(double tps) {
        if (tps >= 18.0D) {
            return "<green>";
        } else {
            return tps >= 13.0D ? "<yellow>" : "<red>";
        }
    }

    public static String getMSPTColor(double mspt) {
        if (mspt < 60.0D) {
            return "<green>";
        } else if (mspt <= 100.0D) {
            return "<yellow>";
        } else {
            return "<red>";
        }
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

    public static void sendLocalizedAmpersandMessage(Player player, String key, boolean prefix, Object... args) {
        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        String msg = String.format(loc.get(key), args);
        if (prefix) msg = PREFIX.concat(" &r&7>>&r ").concat(msg);
        player.sendMessage(translateChars(msg));
    }
    @SuppressWarnings("ConstantConditions")
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
            final int finalMsgIndex = msgIndex;

            Bukkit.getGlobalRegionScheduler().runDelayed(Main.getInstance(), (task) -> {
                try {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (database == null && Main.getInstance() != null) {
                            database = GeneralDatabase.getInstance();
                        }
                        if (database != null && database.getPlayerHideDeathMessages(p.getName())) {
                            continue;
                        }
                        Localization loc = Localization.getLocalization(p.locale().getLanguage());
                        List<TextComponent> deathMessages = loc.getStringList(key)
                                .stream()
                                .map(s -> s.replace("%victim%", victim))
                                .map(s -> s.replace("%killer%", killer))
                                .map(s -> s.replace("%kill-weapon%", weapon))
                                .map(GlobalUtils::translateChars).toList();

                        Component msg = deathMessages.get(finalMsgIndex);
                        p.sendMessage(msg);
                    }
                } catch (Throwable t) {
                    Main.getInstance().getLogger().warning("Failed to send death message: " + t.getMessage());
                }
            }, 1L);
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
        Bukkit.getRegionScheduler().run(Main.getInstance(), player.getLocation(), (task) -> {
            try {
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
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to remove elytra from " + player.getName() + ": " + e.getMessage());
            }
        });
    }
    public static String getStringContent(Component component) {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
        return serializer.serialize(component);
    }
    public static CompletableFuture<Double> getTpsNearEntity(Entity entity) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        double[] regionTpsArr = Bukkit.getRegionTPS(entity.getLocation());
        if (regionTpsArr != null && regionTpsArr.length > 0) {
            future.complete(regionTpsArr[0]);
            return future;
        }

        entity.getScheduler().run(Main.getInstance(), (st) -> {
            double regionTps = getCurrentRegionTps();
            future.complete(regionTps);
        }, () -> future.complete(-1.0));
        return future;
    }
    public static CompletableFuture<Double> getRegionTps(Location location) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        double[] regionTpsArr = Bukkit.getRegionTPS(location);
        if (regionTpsArr != null && regionTpsArr.length > 0) {
            future.complete(regionTpsArr[0]);
            return future;
        }
        
        Bukkit.getRegionScheduler().run(Main.getInstance(), location, (st) -> {
            double regionTps = getCurrentRegionTps();
            future.complete(regionTps);
        });
        return future;
    }

    public static double getCurrentRegionTps() {
        try {
            if (getCurrentRegionMethod == null) {
                Class<?> trs = Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler");
                getCurrentRegionMethod = trs.getMethod("getCurrentRegion");
            }
            Object region = getCurrentRegionMethod.invoke(null);
            if (region != null) {
                return getTpsFromRegionObject(region);
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public static double getCurrentRegionMspt() {
        try {
            if (getCurrentRegionMethod == null) {
                Class<?> trs = Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler");
                getCurrentRegionMethod = trs.getMethod("getCurrentRegion");
            }
            Object region = getCurrentRegionMethod.invoke(null);
            if (region != null) {
                return getMsptFromRegionObject(region);
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static double getTpsFromRegionObject(Object region) {
        try {
            Method mGetData = region.getClass().getMethod("getData");
            Object tickData = mGetData.invoke(region);
            
            Method mGetHandle = tickData.getClass().getMethod("getRegionSchedulingHandle");
            Object handle = mGetHandle.invoke(tickData);
            
            Object report = null;
            String[] reportMethods = {"getTickReport1s", "getTickReport5s", "getTickReport15s"};
            for (String mName : reportMethods) {
                try {
                    Method m = handle.getClass().getMethod(mName, long.class);
                    report = m.invoke(handle, System.nanoTime());
                    if (report != null) break;
                } catch (Exception ignored) {}
            }
            
            if (report != null) {
                Method mTpsData = report.getClass().getMethod("tpsData");
                Object segmentedAvg = mTpsData.invoke(report);
                return getAverageFromSegmented(segmentedAvg);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static double getMsptFromRegionObject(Object region) {
        try {
            Method mGetData = region.getClass().getMethod("getData");
            Object tickData = mGetData.invoke(region);
            
            Method mGetHandle = tickData.getClass().getMethod("getRegionSchedulingHandle");
            Object handle = mGetHandle.invoke(tickData);
            
            Object report = null;
            String[] reportMethods = {"getTickReport1s", "getTickReport5s", "getTickReport15s"};
            for (String mName : reportMethods) {
                try {
                    Method m = handle.getClass().getMethod(mName, long.class);
                    report = m.invoke(handle, System.nanoTime());
                    if (report != null) break;
                } catch (Exception ignored) {}
            }
            
            if (report != null) {
                Object segmentedAvg = null;
                String[] msptMethodNames = {"msptData", "tickTimeData", "mspt", "tickTimes"};
                for (String mName : msptMethodNames) {
                    try {
                        Method m = report.getClass().getMethod(mName);
                        segmentedAvg = m.invoke(report);
                        if (segmentedAvg != null) break;
                    } catch (Exception ignored) {}
                }

                if (segmentedAvg != null) {
                    return getAverageFromSegmented(segmentedAvg);
                }
            }
        } catch (Exception ignored) {}
        
        double tps = getCurrentRegionTps();
        if (tps > 0) return 1000.0 / Math.min(tps, 20.0);
        return -1;
    }

    private static double getAverageFromSegmented(Object segmentedAvg) throws Exception {
        try {
            return (double) segmentedAvg.getClass().getMethod("average").invoke(segmentedAvg);
        } catch (Exception e) {
            Object segAll = segmentedAvg.getClass().getMethod("segmentAll").invoke(segmentedAvg);
            return (double) segAll.getClass().getMethod("average").invoke(segAll);
        }
    }

    public static double getTotalTps() {
        Set<Integer> seenRegions = new HashSet<>();
        double total = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            double[] regionTPS = Bukkit.getRegionTPS(player.getLocation());
            if (regionTPS != null) {
                int regionId = System.identityHashCode(regionTPS);
                if (seenRegions.add(regionId)) {
                    total += regionTPS[0];
                }
            }
        }
        return total;
    }
    public static String formatLocation(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public static UUID getChunkId(Block block) {
        int x = block.getChunk().getX();
        int z = block.getChunk().getZ();

        return UUID.nameUUIDFromBytes((x + ":" + z).getBytes());
    }

    public static int getItemCountInChunk(Block block) {
        return (int) Arrays.stream(block.getChunk().getEntities())
                .filter(entity -> entity instanceof Item)
                .map(entity -> (Item) entity)
                .count();
    }

    public static void updateDisplayName(Player player) {
        if (database == null) database = GeneralDatabase.getInstance();
        String nick = database.getNickname(player.getName());
        if (nick == null || nick.isEmpty()) nick = player.getName();

        nick = nick.replaceAll("(?i)<gradient:[^>]+>", "").replaceAll("(?i)</gradient>", "");

        String gradient = database.getCustomGradient(player.getName());
        if (gradient != null && !gradient.isEmpty()) {
            String animationType = database.getGradientAnimation(player.getName());
            int speed = database.getGradientSpeed(player.getName());
            long tick = GradientAnimator.getAnimationTick();
            String animatedGradient = GradientAnimator.applyAnimation(gradient, animationType, speed, tick);
            nick = String.format("<gradient:%s>%s</gradient>", animatedGradient, nick);
        }

        Component displayName = miniMessage.deserialize(convertToMiniMessageFormat(nick));
        player.displayName(displayName);
    }
}
