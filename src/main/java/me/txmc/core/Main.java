package me.txmc.core;

import lombok.Getter;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.tablist.TabSection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main extends JavaPlugin {
    @Getter
    private static Main instance;
    private List<Section> sections;
    @Getter
    private ScheduledExecutorService executorService;

    @Override
    public void onEnable() {
        sections = new ArrayList<>();
        instance = this;
        executorService = Executors.newScheduledThreadPool(4);
        saveDefaultConfig();
        getLogger().addHandler(new LoggerHandler());
        Localization.loadLocalizations(getDataFolder());

        register(new AntiIllegalMain(this));
        register(new TabSection(this));
        register(new ChatSection(this));

        sections.forEach(Section::enable);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        sections.forEach(Section::disable);
        sections.clear();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        sections.forEach(s -> {
            ConfigurationSection section = getConfig().getConfigurationSection(s.getName());
            if (section != null) s.reloadConfig();
        });
    }

    private void register(Section section) {
        if (sections.contains(section))
            throw new IllegalArgumentException("Section has already been registered " + section.getName());
        sections.add(section);
    }

    public void register(Listener... listeners) {
        for (Listener listener : listeners) getServer().getPluginManager().registerEvents(listener, this);
    }

    public ConfigurationSection getSectionConfig(Section section) {
        return getConfig().getConfigurationSection(section.getName());
    }
    public File getSectionDataFolder(Section section) {
        File dataFolder = new File(getDataFolder(), section.getName());
        if (!dataFolder.exists()) dataFolder.mkdirs();
        return dataFolder;
    }
}
