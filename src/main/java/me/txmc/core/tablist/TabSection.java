package me.txmc.core.tablist;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tablist.listeners.PlayerJoinListener;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.tablist.worker.TabWorker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * @author 254n_m
 * @since 2023/12/17 11:37 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TabSection implements Section {
    private final Main plugin;
    private ConfigurationSection config;
    private long startTime;

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        startTime = System.currentTimeMillis();
        plugin.getExecutorService().scheduleAtFixedRate(new TabWorker(this), 0, 1, TimeUnit.SECONDS);
        plugin.register(new PlayerJoinListener(this));
    }

    @Override
    public void disable() {

    }

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "TabList";
    }

    public void setTab(Player player) {
        //What the FUCK did they deprecate these methods for
        player.setPlayerListHeader(Utils.parsePlaceHolders(String.join("\n", config.getStringList("Header")), player, startTime));
        player.setPlayerListFooter(Utils.parsePlaceHolders(String.join("\n", config.getStringList("Footer")), player, startTime));
    }
}
