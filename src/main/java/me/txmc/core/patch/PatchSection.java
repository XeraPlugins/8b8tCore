package me.txmc.core.patch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.patch.epc.EntityCheckTask;
import me.txmc.core.patch.epc.EntitySpawnListener;
import me.txmc.core.patch.listeners.*;
import me.txmc.core.patch.workers.ElytraWorker;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
    @Getter private HashMap<EntityType, Integer> entityPerChunk;
    private ConfigurationSection config;

    @Override
    public void enable() {
        positions = new HashMap<>();
        config = plugin.getSectionConfig(this);
        entityPerChunk = parseEntityConf();
        plugin.getExecutorService().scheduleAtFixedRate(new ElytraWorker(this), 0, 1, TimeUnit.SECONDS);
        plugin.getExecutorService().scheduleAtFixedRate(new EntityCheckTask(this), 0, config.getInt("EntityPerChunk.CheckInterval"), TimeUnit.MINUTES);
        plugin.register(new Redstone(this));
        plugin.register(new EntitySpawnListener(this));
        plugin.register(new FallFlyListener(plugin));
        plugin.register(new EntitySwitchWorldListener(plugin));
        plugin.register(new NbtBanPatch(plugin));
        plugin.register(new ChestLagFix(plugin));
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
        if (entityPerChunk != null) {
            entityPerChunk.clear();
            entityPerChunk = parseEntityConf();
        }
    }

    private HashMap<EntityType, Integer> parseEntityConf() {
        List<String> raw = config.getStringList("EntityPerChunk.EntitiesPerChunk");
        HashMap<EntityType, Integer> buf = new HashMap<>();
        for (String str : raw) {
            String[] split = str.split("::");
            try {
                EntityType type = EntityType.valueOf(split[0].toUpperCase());
                int i = Integer.parseInt(split[1]);
                buf.put(type, i);
            } catch (EnumConstantNotPresentException | NumberFormatException e) {
                if (e instanceof NumberFormatException) {
                    log(Level.INFO, "&a%s&r&c is not a number", split[1]);
                    continue;
                }
                log(Level.INFO, "&cUnknown EntityType&r&a %s", split[0]);
            }
        }
        int defMax = config.getInt("EntityPerChunk.DefaultMax");
        if (defMax != -1) {
            for (EntityType type : EntityType.values()) buf.putIfAbsent(type, defMax);
        }
        return buf;
    }
}
