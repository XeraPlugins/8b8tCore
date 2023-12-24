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
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class HomeManager implements Section {
    private final HashMap<Player, HomeData> homes = new HashMap<>();
    private final Main plugin;
    private IStorage<HomeData, Player> storage;
    private ConfigurationSection config;

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

    @Override
    public void disable() {
        homes.forEach((p, d) -> storage.save(d, p));
        homes.clear();
    }

    @Override
    public void reloadConfig() {
        this.config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "Home";
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

    public int getMaxHomes(Player player) {
        Set<PermissionAttachmentInfo> perms = player.getEffectivePermissions();
        List<Integer> maxL = perms.stream().map(PermissionAttachmentInfo::getPermission).filter(p -> p.startsWith("8b8tcore.home.max.")).map(s -> Integer.parseInt(s.substring(s.lastIndexOf('.') + 1))).toList();
        return (!maxL.isEmpty()) ? Collections.max(maxL) : config().getInt("MaxHomes");
    }
}
