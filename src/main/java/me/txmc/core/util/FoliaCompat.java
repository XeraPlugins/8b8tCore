package me.txmc.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is part of the 8b8tCore plugin.
 * @author MindComplexity (aka Libalpm)
 * @since 2026/03/18
 */

public class FoliaCompat {

    private static final boolean LUMINOL;
    private static final Logger LOGGER;
    private static final Method LUMINOL_SCHEDULE;
    private static final Method LUMINOL_SCHEDULE_FIXED;
    private static final Method FOLIA_RUN_DELAYED;
    private static final Method FOLIA_RUN;
    private static final Method FOLIA_RUN_FIXED;
    private static final Method GET_SCHEDULER;
    private static final Method GET_HANDLE;
    private static final Method GET_BUKKIT_ENTITY;
    private static final Field TASK_SCHEDULER_FIELD;

    static {
        boolean luminol = false;
        Logger logger = null;
        Method scheduleMethod = null;
        Method scheduleFixedMethod = null;
        Method runDelayedMethod = null;
        Method runMethod = null;
        Method runFixedMethod = null;
        Method getScheduler = null;
        Method getHandle = null;
        Method getBukkitEntity = null;
        Field taskSchedulerField = null;

        try {
            logger = Logger.getLogger(FoliaCompat.class.getName());
            logger.setLevel(Level.WARNING);
        } catch (Exception ignored) {
        }

        try {
            Class<?> minecraftEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            minecraftEntityClass.getMethod("getBukkitEntity");

            Class<?> taskSchedulerClass = Class.forName("io.papermc.paper.threadedregions.EntityScheduler");
            scheduleMethod = taskSchedulerClass.getMethod("schedule", Runnable.class, Runnable.class, long.class);
            scheduleFixedMethod = taskSchedulerClass.getMethod("scheduleAtFixedRate", Runnable.class, Runnable.class, long.class, long.class);

            getHandle = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity").getMethod("getHandle");
            getBukkitEntity = minecraftEntityClass.getMethod("getBukkitEntity");
            taskSchedulerField = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity").getField("taskScheduler");

            luminol = true;
        } catch (Exception ignored) {
        }

        try {
            Class<?> taskSchedulerClass = Class.forName("io.papermc.paper.threadedregions.EntityScheduler");
            runDelayedMethod = taskSchedulerClass.getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class);
            runMethod = taskSchedulerClass.getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class);
            runFixedMethod = taskSchedulerClass.getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class, long.class);
            getScheduler = Entity.class.getMethod("getScheduler");
        } catch (Exception ignored) {
        }

        LUMINOL = luminol;
        LOGGER = logger;
        LUMINOL_SCHEDULE = scheduleMethod;
        LUMINOL_SCHEDULE_FIXED = scheduleFixedMethod;
        FOLIA_RUN_DELAYED = runDelayedMethod;
        FOLIA_RUN = runMethod;
        FOLIA_RUN_FIXED = runFixedMethod;
        GET_SCHEDULER = getScheduler;
        GET_HANDLE = getHandle;
        GET_BUKKIT_ENTITY = getBukkitEntity;
        TASK_SCHEDULER_FIELD = taskSchedulerField;
    }

    private static Object getLuminolScheduler(Entity entity) {
        if (GET_HANDLE == null || GET_BUKKIT_ENTITY == null || TASK_SCHEDULER_FIELD == null) return null;
        try {
            Object minecraftEntity = GET_HANDLE.invoke(entity);
            Object bukkitEntity = GET_BUKKIT_ENTITY.invoke(minecraftEntity);
            return TASK_SCHEDULER_FIELD.get(bukkitEntity);
        } catch (Exception e) {
            if (LOGGER != null) LOGGER.log(Level.WARNING, "Failed to get Luminol scheduler: " + e.getMessage());
            return null;
        }
    }

    public static void scheduleDelayed(Entity entity, Plugin plugin, Runnable task, long delay) {
        if (LUMINOL && LUMINOL_SCHEDULE != null) {
            Object scheduler = getLuminolScheduler(entity);
            if (scheduler != null) {
                try {
                    LUMINOL_SCHEDULE.invoke(scheduler, task, null, delay);
                    return;
                } catch (Exception e) {
                    if (LOGGER != null) LOGGER.log(Level.WARNING, "Luminol schedule failed, falling back: " + e.getMessage());
                }
            }
        }
        if (FOLIA_RUN_DELAYED != null && GET_SCHEDULER != null) {
            try {
                Object scheduler = GET_SCHEDULER.invoke(entity);
                FOLIA_RUN_DELAYED.invoke(scheduler, plugin, (java.util.function.Consumer<?>) t -> task.run(), null, delay);
                return;
            } catch (Exception e) {
                if (LOGGER != null) LOGGER.log(Level.WARNING, "Folia schedule failed, falling back: " + e.getMessage());
            }
        }
        Location loc = entity.getLocation();
        Bukkit.getRegionScheduler().runDelayed(plugin, loc, t -> {
            if (entity.isValid() && Bukkit.isOwnedByCurrentRegion(entity)) {
                task.run();
            }
        }, delay);
    }

    public static void schedule(Entity entity, Plugin plugin, Runnable task) {
        if (LUMINOL && LUMINOL_SCHEDULE != null) {
            Object scheduler = getLuminolScheduler(entity);
            if (scheduler != null) {
                try {
                    LUMINOL_SCHEDULE.invoke(scheduler, task, null, 1L);
                    return;
                } catch (Exception e) {
                    if (LOGGER != null) LOGGER.log(Level.WARNING, "Luminol schedule failed, falling back: " + e.getMessage());
                }
            }
        }
        if (FOLIA_RUN != null && GET_SCHEDULER != null) {
            try {
                Object scheduler = GET_SCHEDULER.invoke(entity);
                FOLIA_RUN.invoke(scheduler, plugin, (java.util.function.Consumer<?>) t -> task.run(), null);
                return;
            } catch (Exception e) {
                if (LOGGER != null) LOGGER.log(Level.WARNING, "Folia schedule failed, falling back: " + e.getMessage());
            }
        }
        Location loc = entity.getLocation();
        Bukkit.getRegionScheduler().run(plugin, loc, t -> {
            if (entity.isValid() && Bukkit.isOwnedByCurrentRegion(entity)) {
                task.run();
            }
        });
    }

    public static void scheduleAtFixedRate(Entity entity, Plugin plugin, Runnable task, long initialDelay, long period) {
        if (LUMINOL && LUMINOL_SCHEDULE_FIXED != null) {
            Object scheduler = getLuminolScheduler(entity);
            if (scheduler != null) {
                try {
                    LUMINOL_SCHEDULE_FIXED.invoke(scheduler, task, null, initialDelay, period);
                    return;
                } catch (Exception e) {
                    if (LOGGER != null) LOGGER.log(Level.WARNING, "Luminol schedule failed, falling back: " + e.getMessage());
                }
            }
        }
        if (FOLIA_RUN_FIXED != null && GET_SCHEDULER != null) {
            try {
                Object scheduler = GET_SCHEDULER.invoke(entity);
                FOLIA_RUN_FIXED.invoke(scheduler, plugin, (java.util.function.Consumer<?>) t -> task.run(), null, initialDelay, period);
                return;
            } catch (Exception e) {
                if (LOGGER != null) LOGGER.log(Level.WARNING, "Folia schedule failed, falling back: " + e.getMessage());
            }
        }
        Location loc = entity.getLocation();
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc, t -> {
            if (entity.isValid() && Bukkit.isOwnedByCurrentRegion(entity)) {
                task.run();
            }
        }, initialDelay, period);
    }
}
