package me.txmc.core.patch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.patch.workers.ElytraWorker;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private ConfigurationSection config;

    @Override
    public void enable() {
        positions = new HashMap<>();
        config = plugin.getSectionConfig(this);
        plugin.getExecutorService().scheduleAtFixedRate(new ElytraWorker(this), 0, 1, TimeUnit.SECONDS);
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
    }
}
