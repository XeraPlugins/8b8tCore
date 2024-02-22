package me.txmc.core.patch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.patch.epc.*;
import me.txmc.core.patch.listeners.FallFlyListener;
import me.txmc.core.patch.workers.ElytraWorker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.info;
import static me.txmc.core.util.GlobalUtils.log;

/**
 * @author 254n_m
 * @since 2023/12/26 12:43 AM
 * This file was created as a part of 8b8tCore
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PatchSection implements Section {
    private final Main plugin;
    private Map<Player, Location> positions;
    private HashMap<String, Integer> maxEntityPerChunk;
    private HashMap<String, Integer> minEntityPerChunk;
    private HashMap<Material, Integer> maxTilePerChunk;
    private ConfigurationSection config;
    private static PatchSection instance;

    @Override
    public void enable() {
        info("Enabling PatchSection");
        positions = new HashMap<>();
        config = plugin.getSectionConfig(this);
        maxEntityPerChunk = new HashMap<>();
        minEntityPerChunk = new HashMap<>();
        maxTilePerChunk = new HashMap<>();
        plugin.getExecutorService().scheduleAtFixedRate(new ElytraWorker(this), 0, 1, TimeUnit.SECONDS);
        plugin.getExecutorService().scheduleAtFixedRate(new EntityCheckTask(this), 0, config.getInt("EntityPerChunk.CheckInterval"), TimeUnit.MINUTES);
        plugin.register(new EntitySpawnListener(this));
        plugin.register(new FallFlyListener(plugin));
        plugin.register(new BookBan());
        plugin.register(new TileEntityListener());
        plugin.register(new RedstoneListener());
        plugin.register(new EntityPortalListener());
        parseEntityConf();
        parseTileConf();
        instance = this;
        info("PatchSection has been enabled!");
    }

    @Override
    public void disable() {
    }

    @Override
    public String getName() {
        return "Patch";
    }

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
        if (maxEntityPerChunk != null) {
            maxEntityPerChunk.clear();
            parseEntityConf();
        }
        if (minEntityPerChunk != null) {
            minEntityPerChunk.clear();
            parseEntityConf();
        }
        if (maxTilePerChunk != null) {
            maxTilePerChunk.clear();
            parseTileConf();
        }
    }

    private void parseEntityConf() {
        List<String> raw = config.getStringList("Patch.EntityPerChunk.EntitiesPerChunk");
        for (String str : raw) {
            String[] split = str.split("::");
            try {
                String type = split[0].toLowerCase();
                int i = Integer.parseInt(split[1]);
                int j = Integer.parseInt(split[2]);
                minEntityPerChunk.put(type, i);
                maxEntityPerChunk.put(type, j);

            } catch (EnumConstantNotPresentException | NumberFormatException e) {
                if (e instanceof NumberFormatException) {
                    log(Level.INFO, "&a%s&r&c is not a number", split[1]);
                    continue;
                }
                log(Level.INFO, "&cUnknown EntityType&r&a %s", split[0]);
            }
        }
    }

    private void parseTileConf() {
        List<String> raw = config.getStringList("Patch.EntityPerChunk.TileEntitiesPerChunk");
        for (String str : raw) {
            String[] split = str.split("::");
            try {
                Material type = Material.valueOf(split[0].toUpperCase());
                int i = Integer.parseInt(split[1]);
                maxTilePerChunk.put(type, i);
            } catch (EnumConstantNotPresentException | NumberFormatException e) {
                if (e instanceof NumberFormatException) {
                    log(Level.INFO, "&a%s&r&c is not a number", split[1]);
                    continue;
                }
                log(Level.INFO, "&cUnknown Material&r&a %s", split[0]);
            }
        }
    }

    public static int getMobMaxSoft(EntityType ent) {
        return instance.minEntityPerChunk.getOrDefault(ent.toString().toLowerCase(), Main.getInstance().getConfig().getInt("Patch.EntityPerChunk.DefaultEntitiesPerChunk"));
    }

    public static int getMobMaxHard(EntityType ent) {
        return instance.maxEntityPerChunk.getOrDefault(ent.toString().toLowerCase(), Main.getInstance().getConfig().getInt("Patch.EntityPerChunk.DefaultEntitiesPerChunk"));
    }

    public static int getTileMax(Material mat) {
        return instance.maxTilePerChunk.getOrDefault(mat, Main.getInstance().getConfig().getInt("Patch.EntityPerChunk.DefaultTileEntitiesPerChunk"));
    }
}
