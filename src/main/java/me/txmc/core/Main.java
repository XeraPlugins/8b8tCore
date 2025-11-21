package me.txmc.core;

import lombok.Getter;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.chat.listeners.OpWhiteListListener;
import me.txmc.core.chat.tasks.AnnouncementTask;
import me.txmc.core.command.CommandSection;
import me.txmc.core.customexperience.listeners.CustomExperienceJoinLeave;
import me.txmc.core.deathmessages.DeathMessageListener;
import me.txmc.core.dupe.DupeSection;
import me.txmc.core.home.HomeManager;
import me.txmc.core.patch.PatchSection;
import me.txmc.core.patch.tasks.EndPortalBuilder;
import me.txmc.core.patch.tasks.EndExitPortalBuilder;
import me.txmc.core.tablist.TabSection;
import me.txmc.core.util.MapCreationLogger;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.vote.VoteSection;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;

import static me.txmc.core.util.GlobalUtils.log;

public class Main extends JavaPlugin {
    @Getter private static Main instance;
    @Getter public static String prefix;
    @Getter private ScheduledExecutorService executorService;
    private List<Section> sections;
    private List<Reloadable> reloadables;
    private List<ViolationManager> violationManagers;
    @Getter private long startTime;
    public final Map<Player, Location> lastLocations = new HashMap<>();
    @Getter private final Set<UUID> vanishedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("\u00A79   ___    _        \u00A79___    _      \u00A77____                       ");
        Bukkit.getConsoleSender().sendMessage("\u00A79  ( _ )  | |__    \u00A79( _ )  | |_   \u00A77/ ___|   ___    _ __    ___ ");
        Bukkit.getConsoleSender().sendMessage("\u00A79  / _ \\  | '_ \\   \u00A79/ _ \\  | __| \u00A77| |      / _ \\  | '__|  / _ \\");
        Bukkit.getConsoleSender().sendMessage("\u00A79 | (_) | | |_) | \u00A79| (_) | | |_  \u00A77| |___  | (_) | | |    |  __/");
        Bukkit.getConsoleSender().sendMessage("\u00A79  \\___/  |_.__/   \u00A79\\___/   \\__|  \u00A77\\____|  \\___/  |_|     \\___|");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("\u00A78+============================================================+");
        Bukkit.getConsoleSender().sendMessage("\u00A79  8b\u00A798t\u00A77Core \u00A75a Folia Core Plugin for 8b8t and Anarchy Servers   ");
        Bukkit.getConsoleSender().sendMessage("\u00A78+============================================================+");
        Bukkit.getConsoleSender().sendMessage("\u00A72 v" + getDescription().getVersion() + "                              \u00A73by 254n_m, agarciacorte & Leeewith3Es");
        Bukkit.getConsoleSender().sendMessage("");

        sections = new ArrayList<>();
        reloadables = new ArrayList<>();
        violationManagers = new ArrayList<>();
        instance = this;
        executorService = Executors.newScheduledThreadPool(4);
        startTime = System.currentTimeMillis();
        prefix = getConfig().getString("prefix", "&8[&98b&78t&8]");
        saveDefaultConfig();
        getLogger().addHandler(new LoggerHandler());
        Localization.loadLocalizations(getDataFolder());

        executorService.scheduleAtFixedRate(() -> violationManagers.forEach(ViolationManager::decrementAll), 0, 1, TimeUnit.SECONDS);
        getExecutorService().scheduleAtFixedRate(new AnnouncementTask(), 10L, getConfig().getInt("AnnouncementInterval"), TimeUnit.SECONDS);

        getExecutorService().scheduleAtFixedRate(new EndPortalBuilder(this), 200L, 10, TimeUnit.SECONDS);
        getExecutorService().scheduleAtFixedRate(new EndExitPortalBuilder(this), 200L, 10, TimeUnit.SECONDS);


        register(new TabSection(this));
        register(new ChatSection(this));
        register(new TPASection(this));
        register(new HomeManager(this));
        register(new CommandSection(this));
        register(new PatchSection(this));
        register(new DupeSection(this));
        register(new DeathMessageListener());
        register(new me.txmc.core.achievements.AchievementsListener(this));
        register(new CustomExperienceJoinLeave(this));
        register(new OpWhiteListListener(this));

        if(getConfig().getBoolean("AntiIllegal.Enabled", true)) register(new AntiIllegalMain(this));
        // Force Votifier plus by the server uncommented for compability reasons. 
        // if (getServer().getPluginManager().getPlugin("VotifierPlus") != null) register(new VoteSection(this));
        register(new VoteSection(this));

        for (Section section : sections) {
            try {
                section.enable();
            } catch (Exception e) {
                log(Level.SEVERE, "Failed to enable section %s. Please see stacktrace below for more info", section.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        violationManagers.clear();
        sections.forEach(Section::disable);
        sections.clear();

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        for (Handler handler : MapCreationLogger.getLogger().getHandlers()) {
            handler.close();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        sections.forEach(s -> {
            ConfigurationSection section = getConfig().getConfigurationSection(s.getName());
            if (section != null) s.reloadConfig();
        });
        reloadables.forEach(Reloadable::reloadConfig);
    }

    private void register(Section section) {
        if (getSectionByName(section.getName()) != null)
            throw new IllegalArgumentException("Section has already been registered " + section.getName());
        sections.add(section);
    }

    public void register(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
            if (listener instanceof Reloadable reloadable) reloadables.add(reloadable);
        }
    }

    public void register(ViolationManager manager) {
//        if (violationManagers.contains(manager)) throw new IllegalArgumentException("Attempted to register violation manager twice");
        if (violationManagers.contains(manager)) return;
        violationManagers.add(manager);
        if (manager instanceof Listener) register((Listener) manager);
    }

    public Reloadable registerReloadable(Reloadable reloadable) {
        reloadables.add(reloadable);
        return reloadable;
    }

    public ConfigurationSection getSectionConfig(Section section) {
        return getConfig().getConfigurationSection(section.getName());
    }

    public File getSectionDataFolder(Section section) {
        File dataFolder = new File(getDataFolder(), section.getName());
        if (!dataFolder.exists()) dataFolder.mkdirs();
        return dataFolder;
    }

    public Section getSectionByName(String name) {
        return sections.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
    }

    public Location getLastLocation(Player player) {
        return lastLocations.get(player);
    }
}
