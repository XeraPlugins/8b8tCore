package me.txmc.core.home;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.IStorage;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.home.commands.DelHomeCommand;
import me.txmc.core.home.commands.HomeCommand;
import me.txmc.core.home.commands.SetHomeCommand;
import me.txmc.core.home.io.HomeJsonStorage;
import me.txmc.core.home.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class HomeManager implements Section {
    private IStorage<HomeData, Player> storage;
    private final HashMap<Player, HomeData> homes = new HashMap<>();
    private ConfigurationSection config;
    private final Main plugin;

    @Override
    public void enable() {
        File homesFolder = new File(plugin.getSectionDataFolder(this), "PlayerHomes");
        if (!homesFolder.exists()) homesFolder.mkdir();
        storage = new HomeJsonStorage(homesFolder);
        config = plugin.getSectionConfig(this);
        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(p -> homes.put(p, storage.load(p)));
        plugin.register(new JoinListener(this));
        plugin.getCommand("home").setExecutor(new HomeCommand(this));
        plugin.getCommand("sethome").setExecutor(new SetHomeCommand(this));
        plugin.getCommand("delhome").setExecutor(new DelHomeCommand(this));
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            HomeData homes = homes().getOrDefault(player, null);
            if (homes == null) return Collections.emptyList();
            if (args.length < 1) {
                return homes.stream().map(Home::getName).sorted(String::compareToIgnoreCase).collect(Collectors.toList());
            } else {
                return homes.stream().map(Home::getName).filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).sorted(String::compareToIgnoreCase).collect(Collectors.toList());
            }
        } else return Collections.emptyList();
    }

    @Override
    public void disable() {
        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(p -> {
            HomeData data = homes.get(p);
            storage.save(data,p);
            homes.remove(p);
        });
    }

    @Override
    public void reloadConfig() {
        this.config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "Home";
    }
}
