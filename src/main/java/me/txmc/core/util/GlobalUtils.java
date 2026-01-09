package me.txmc.core.util;

import lombok.Cleanup;
import lombok.Getter;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
 */
public class GlobalUtils {
    @Getter
    private static final String PREFIX = Main.prefix;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final Map<String, TextComponent> componentCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static GeneralDatabase database;

    private static java.lang.reflect.Method getCurrentRegionMethod;
    private static java.lang.reflect.Method getDataMethod;
    private static java.lang.reflect.Method getRegionSchedulingHandleMethod;
    private static java.lang.reflect.Method getTickReport15sMethod;
    private static java.lang.reflect.Method tpsDataMethod;
    private static java.lang.reflect.Method msptDataMethod;
    private static java.lang.reflect.Method segmentAllMethod;
    private static java.lang.reflect.Method averageMethod;
    
    @SuppressWarnings("deprecation")
    public static int calculateItemSize(org.bukkit.inventory.ItemStack item) {
        if (item == null) return 0;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             org.bukkit.util.io.BukkitObjectOutputStream boos = new org.bukkit.util.io.BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            boos.flush();
            return baos.size();
        } catch (Throwable e) {
            return 0;
        }
    }

    public static void info(String format) {
        log(Level.INFO, format);
    }

    public static void log(Level level, String format, Object... args) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[2];
        String message = String.format(format, args);
        message = getStringContent(translateChars(message));
        Main.getInstance().getLogger().log(level,
                String.format("%s%c%s", message, Character.MIN_VALUE, element.getClassName()));
    }

    public static TextComponent translateChars(String input) {
        if (input == null) return Component.empty();
        return componentCache.computeIfAbsent(input, i -> (TextComponent) miniMessage.deserialize(convertToMiniMessageFormat(i)));
    }

    public static boolean isTeleportRestricted(Player player) {
        if (player.isOp() || player.hasPermission("8b8tcore.teleport.bypass")) return false;

        if (player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) >= 7200000) return false;

        boolean ranked = false;
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getPermission().startsWith("8b8tcore.prefix.") && info.getValue()) {
                ranked = true;
                break;
            }
        }
        if (ranked) return false;

        int maxDistanceFromSpawn = Main.getInstance().getConfig().getInt("TPAHOMERADIUS.default", 50000);

        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith("tpa.spawn.") || perm.startsWith("home.spawn.")) {
                try {
                    String sub = perm.startsWith("tpa.spawn.") ? perm.substring(10) : perm.substring(11);
                    int dist = Integer.parseInt(sub);
                    maxDistanceFromSpawn = Math.min(maxDistanceFromSpawn, dist);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            maxDistanceFromSpawn /= 8;
        }

        org.bukkit.Location loc = player.getLocation();
        return loc.getBlockX() < maxDistanceFromSpawn && loc.getBlockX() > -maxDistanceFromSpawn &&
               loc.getBlockZ() < maxDistanceFromSpawn && loc.getBlockZ() > -maxDistanceFromSpawn;
    }

    public static int getTeleportRestrictionRange(Player player) {
        int range = Main.getInstance().getConfig().getInt("TPAHOMERADIUS.default", 50000);
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) continue;
            String perm = info.getPermission();
            if (perm.startsWith("tpa.spawn.") || perm.startsWith("home.spawn.")) {
                try {
                    String sub = perm.startsWith("tpa.spawn.") ? perm.substring(10) : perm.substring(11);
                    int dist = Integer.parseInt(sub);
                    range = Math.min(range, dist);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) range /= 8;
        return range;
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
        if (prefix)
            msg = String.format("%s &7>>&r %s", PREFIX, msg);
        msg = String.format(msg, args);
        obj.sendMessage(translateChars(msg));
    }

    public static void sendPrefixedLocalizedMessage(Player player, String key, Object... args) {
        sendLocalizedMessage(player, key, true, args);
    }

    public static void sendLocalizedMessage(Player player, String key, boolean prefix, Object... args) {
        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        String msg = String.format(loc.get(key), args);
        if (prefix)
            msg = PREFIX.concat(" &r&7>>&r ").concat(msg);
        player.sendMessage(translateChars(msg));
    }

    public static void sendLocalizedAmpersandMessage(Player player, String key, boolean prefix, Object... args) {
        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        String msg = String.format(loc.get(key), args);
        if (prefix)
            msg = PREFIX.concat(" &r&7>>&r ").concat(msg);
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
            if (deathListMessages.size() > 1) {
                Random random = new Random();
                msgIndex = random.nextInt(deathListMessages.size());
            }
            final int finalMsgIndex = msgIndex;

            Bukkit.getGlobalRegionScheduler().runDelayed(Main.getInstance(), (task) -> {
                try {
                    if (database == null && Main.getInstance() != null) {
                        database = GeneralDatabase.getInstance();
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        database.getPlayerHideDeathMessagesAsync(p.getName()).thenAccept(hideDeathMessages -> {
                            if (hideDeathMessages || !p.isOnline()) {
                                return;
                            }
                            p.getScheduler().run(Main.getInstance(), (playerTask) -> {
                                if (!p.isOnline()) return;
                                Localization loc = Localization.getLocalization(p.locale().getLanguage());
                                List<String> rawMessages = loc.getStringList(key);
                                if (rawMessages.isEmpty()) return;
                                
                                String rawMsg = rawMessages.get(finalMsgIndex);
                                String formatted = rawMsg.replace("%victim%", victim)
                                        .replace("%killer%", killer)
                                        .replace("%kill-weapon%", weapon);
                                
                                p.sendMessage(translateChars(formatted));
                            }, null);
                        });
                    }
                } catch (Throwable t) {
                    Main.getInstance().getLogger().warning("Failed to send death message: " + t.getMessage());
                }
            }, 1L);
        } catch (Throwable ignored) {
        }

    }

    public static void sendPrefixedComponent(CommandSender target, Component component) {
        target.sendMessage(translateChars(String.format("%s &7>>&r ", PREFIX)).append(component));
    }

    public static void unpackResource(String resourceName, File file) {
        if (file.exists())
            return;
        try {
            @Cleanup
            InputStream is = Main.class.getClassLoader().getResourceAsStream(resourceName);
            if (is == null)
                throw new NullPointerException(String.format("Resource %s is not present in the jar", resourceName));
            Files.copy(is, file.toPath());
        } catch (Throwable t) {
            log(Level.SEVERE,
                    "&cFailed to extract resource from jar due to &r&3 %s&r&c! Please see the stacktrace below for more info",
                    t.getMessage());
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
                if (chestPlate == null)
                    return;
                if (chestPlate.getType() == Material.AIR)
                    return;
                if (chestPlate.getType() == Material.ELYTRA) {
                    PlayerInventory inventory = player.getInventory();
                    if (inventory.firstEmpty() == -1) {
                        player.getWorld().dropItemNaturally(player.getLocation(), chestPlate);
                    } else
                        inventory.setItem(inventory.firstEmpty(), chestPlate);
                    ItemStack[] buffer = inventory.getArmorContents();
                    buffer[2] = null;
                    inventory.setArmorContents(buffer);
                }
            } catch (Exception e) {
                Main.getInstance().getLogger()
                        .warning("Failed to remove elytra from " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    public static String getStringContent(Component component) {
        if (component == null) return "";
        try {
            if (getComponentDepth(component) > 50) return "Too many extra components";
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            return serializer.serialize(component);
        } catch (Throwable t) {
            return "Error serializing component";
        }
    }

    public static int getComponentDepth(Component component) {
        if (component == null) return 0;
        return getComponentDepth(component, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static int getComponentDepth(Component component, int currentDepth, Set<Component> visited) {
        if (component == null) return currentDepth;
        if (currentDepth > 50) return currentDepth;
        if (!visited.add(component)) return currentDepth + 100;

        int maxDepth = currentDepth + 1;

        for (Component child : component.children()) {
            maxDepth = Math.max(maxDepth, getComponentDepth(child, currentDepth + 1, visited));
            if (maxDepth > 50) return maxDepth;
        }

        if (component instanceof net.kyori.adventure.text.TranslatableComponent translatable) {
            for (Component arg : translatable.args()) {
                maxDepth = Math.max(maxDepth, getComponentDepth(arg, currentDepth + 1, visited));
                if (maxDepth > 50) return maxDepth;
            }
        }
        
        if (component.hoverEvent() != null) {
            net.kyori.adventure.text.event.HoverEvent<?> hover = component.hoverEvent();
            if (hover.action() == net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT) {
                Component hoverContent = (Component) hover.value();
                maxDepth = Math.max(maxDepth, getComponentDepth(hoverContent, currentDepth + 1, visited));
            } else if (hover.action() == net.kyori.adventure.text.event.HoverEvent.Action.SHOW_ENTITY) {
                net.kyori.adventure.text.event.HoverEvent.ShowEntity info = (net.kyori.adventure.text.event.HoverEvent.ShowEntity) hover.value();
                if (info.name() != null) {
                   maxDepth = Math.max(maxDepth, getComponentDepth(info.name(), currentDepth + 1, visited));
                }
            }
        }
        
        visited.remove(component);
        return maxDepth;
    }

    public static CompletableFuture<Double> getTpsNearEntity(Entity entity) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            double[] regionTpsArr = Bukkit.getRegionTPS(entity.getLocation());
            if (regionTpsArr != null && regionTpsArr.length > 0) {
                future.complete(regionTpsArr[0]);
                return future;
            }
        } catch (Throwable ignored) {
        }

        entity.getScheduler().run(Main.getInstance(), (st) -> {
            double regionTps = getCurrentRegionTps();
            future.complete(regionTps);
        }, () -> future.complete(-1.0));
        return future;
    }

    public static CompletableFuture<Double> getRegionTps(Location location) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {
            double[] regionTpsArr = Bukkit.getRegionTPS(location);
            if (regionTpsArr != null && regionTpsArr.length > 0) {
                future.complete(regionTpsArr[0]);
                return future;
            }
        } catch (Throwable ignored) {
        }

        Bukkit.getRegionScheduler().run(Main.getInstance(), location, (st) -> {
            double regionTps = getCurrentRegionTps();
            future.complete(regionTps);
        });
        return future;
    }

    private static boolean loggedMsptError = false;

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
        } catch (Throwable t) {
            if (!loggedMsptError) {
                Main.getInstance().getLogger().log(Level.WARNING, "Failed to get current region TPS", t);
                loggedMsptError = true;
            }
        }
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
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static double getTpsFromRegionObject(Object region) {
        try {
            Method mGetData = region.getClass().getMethod("getData");
            Object tickData = mGetData.invoke(region);

            Method mGetHandle = tickData.getClass().getMethod("getRegionSchedulingHandle");
            Object handle = mGetHandle.invoke(tickData);

            Object report = null;
            String[] reportMethods = { "getTickReport1s", "getTickReport5s", "getTickReport15s" };
            for (String mName : reportMethods) {
                try {
                    Method m = handle.getClass().getMethod(mName, long.class);
                    report = m.invoke(handle, System.nanoTime());
                    if (report != null)
                        break;
                } catch (Exception ignored) {
                }
            }

            if (report != null) {
                Method mTpsData = report.getClass().getMethod("tpsData");
                Object segmentedAvg = mTpsData.invoke(report);
                return getAverageFromSegmented(segmentedAvg);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static double getMsptFromRegionObject(Object region) {
        try {
            Method mGetData = region.getClass().getMethod("getData");
            Object tickData = mGetData.invoke(region);

            Method mGetHandle = tickData.getClass().getMethod("getRegionSchedulingHandle");
            Object handle = mGetHandle.invoke(tickData);

            Object report = null;
            String[] reportMethods = { "getTickReport1s", "getTickReport5s", "getTickReport15s" };
            for (String mName : reportMethods) {
                try {
                    Method m = handle.getClass().getMethod(mName, long.class);
                    report = m.invoke(handle, System.nanoTime());
                    if (report != null)
                        break;
                } catch (Exception ignored) {
                }
            }

            if (report != null) {
                Object segmentedAvg = null;
                String[] msptMethodNames = { "msptData", "tickTimeData", "mspt", "tickTimes" };
                for (String mName : msptMethodNames) {
                    try {
                        Method m = report.getClass().getMethod(mName);
                        segmentedAvg = m.invoke(report);
                        if (segmentedAvg != null)
                            break;
                    } catch (Exception ignored) {
                    }
                }

                if (segmentedAvg != null) {
                    return getAverageFromSegmented(segmentedAvg);
                }
            }
        } catch (Exception ignored) {
        }

        double tps = getCurrentRegionTps();
        if (tps > 0)
            return 1000.0 / Math.min(tps, 20.0);
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
        return location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", "
                + location.getBlockZ();
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
        updateDisplayNameAsync(player).join();
    }

    public static CompletableFuture<Void> updateDisplayNameAsync(Player player) {
        if (database == null)
            database = GeneralDatabase.getInstance();
        
        String username = player.getName();
        
        CompletableFuture<String> nickFuture = database.getNicknameAsync(username);
        CompletableFuture<String> gradientFuture = database.getCustomGradientAsync(username);
        CompletableFuture<String> animFuture = database.getGradientAnimationAsync(username);
        CompletableFuture<Integer> speedFuture = database.getGradientSpeedAsync(username);
        CompletableFuture<String> decorationsFuture = database.getPlayerDataAsync(username, "nameDecorations");
        
        return CompletableFuture.allOf(nickFuture, gradientFuture, animFuture, speedFuture, decorationsFuture)
            .thenAcceptAsync(v -> {
                if (!player.isOnline()) return;
                
                String nick = nickFuture.join();
                String customGradient = gradientFuture.join();
                String anim = animFuture.join();
                int speed = speedFuture.join();
                String decorationsStr = decorationsFuture.join();
                
                player.getScheduler().run(Main.getInstance(), (task) -> {
                    if (!player.isOnline()) return;
                    player.displayName(parseDisplayName(player.getName(), nick, customGradient, anim, speed, decorationsStr));
                }, null);
            });
    }

    public static Component parseDisplayName(String playerName, String nick, String customGradient, String anim, int speed, String decorationsStr) {
        String baseName;
        if (nick == null || nick.isEmpty() || nick.equals(playerName)) {
            baseName = playerName;
        } else {
            baseName = PlainTextComponentSerializer.plainText()
                    .serialize(miniMessage.deserialize(convertToMiniMessageFormat(nick))).trim();
        }

        if (customGradient == null || customGradient.trim().isEmpty()) {
            return renderSimpleName(baseName, decorationsStr);
        }

        String workingGradient = customGradient.trim();
        if (workingGradient.toLowerCase().startsWith("<gradient:") && workingGradient.endsWith(">")) {
            workingGradient = workingGradient.substring(10, workingGradient.length() - 1);
        } else if (workingGradient.toLowerCase().startsWith("<color:") && workingGradient.endsWith(">")) {
            workingGradient = workingGradient.substring(7, workingGradient.length() - 1);
        }

        if (workingGradient.toLowerCase().contains("tobias:")) {
            try {
                String lower = workingGradient.toLowerCase();
                int tIndex = lower.indexOf("tobias:");
                String format = workingGradient.substring(tIndex + 7).trim();
                if (format.endsWith(">")) format = format.substring(0, format.length() - 1);
                
                String[] parts = format.split(";");
                Component finalComp = Component.empty();
                int currentIndex = 0;
                for (String part : parts) {
                    if (!part.contains(":")) continue;
                    String[] split = part.split(":");
                    int len = Integer.parseInt(split[0]);
                    String decorations = split.length > 1 ? split[1] : "";
                    String colorHex = split.length > 2 ? split[2] : "";

                    if (currentIndex + len > baseName.length()) len = baseName.length() - currentIndex;
                    if (len <= 0) break;
                    String sub = baseName.substring(currentIndex, currentIndex + len);
                    currentIndex += len;

                    Component segment = Component.text(sub);
                    if (!colorHex.isEmpty() && colorHex.startsWith("#")) {
                        TextColor textColor = TextColor.fromHexString(colorHex);
                        if (textColor != null) segment = segment.color(textColor);
                    }
                    if (!decorations.isEmpty() && !decorations.equalsIgnoreCase("none")) {
                        for (String dec : decorations.split("/")) {
                            TextDecoration textDecoration = TextDecoration.NAMES.value(dec.toLowerCase().trim());
                            if (textDecoration != null) segment = segment.decoration(textDecoration, true);
                        }
                    }
                    finalComp = finalComp.append(segment);
                }
                return finalComp;
            } catch (Exception e) {
                return renderSimpleName(baseName, decorationsStr);
            }
        }

        boolean isGradient = workingGradient.contains(":") &&
                workingGradient.indexOf('#') != workingGradient.lastIndexOf('#');

        String finalGradient;
        if (isGradient) {
            finalGradient = GradientAnimator.applyAnimation(workingGradient, anim, speed,
                    GradientAnimator.getAnimationTick());
        } else {
            finalGradient = workingGradient;
        }

        StringBuilder result = new StringBuilder();
        if (decorationsStr != null && !decorationsStr.isEmpty()) {
            for (String decoration : decorationsStr.split(",")) {
                result.append("<").append(decoration.trim()).append(">");
            }
        }

        if (isGradient) {
            result.append("<gradient:").append(finalGradient).append(">")
                    .append(baseName)
                    .append("</gradient>");
        } else {
            String color = finalGradient.contains(":") ? finalGradient.split(":")[0] : finalGradient;
            result.append("<color:").append(color).append(">")
                    .append(baseName)
                    .append("</color>");
        }

        if (decorationsStr != null && !decorationsStr.isEmpty()) {
            String[] decorations = decorationsStr.split(",");
            for (int i = decorations.length - 1; i >= 0; i--) {
                result.append("</").append(decorations[i].trim()).append(">");
            }
        }

        return miniMessage.deserialize(result.toString());
    }

    private static Component renderSimpleName(String name, String decorationsStr) {
        StringBuilder sb = new StringBuilder();
        if (decorationsStr != null && !decorationsStr.isEmpty()) {
            for (String dec : decorationsStr.split(",")) sb.append("<").append(dec.trim()).append(">");
        }
        sb.append(name);
        if (decorationsStr != null && !decorationsStr.isEmpty()) {
            String[] decs = decorationsStr.split(",");
            for (int i = decs.length - 1; i >= 0; i--) sb.append("</").append(decs[i].trim()).append(">");
        }
        return miniMessage.deserialize(convertToMiniMessageFormat(sb.toString()));
    }
}
