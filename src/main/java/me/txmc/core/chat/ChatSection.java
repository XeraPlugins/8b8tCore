package me.txmc.core.chat;

import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.IStorage;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.chat.commands.*;
import me.txmc.core.chat.io.ChatFileIO;
import me.txmc.core.chat.listeners.ChatListener;
import me.txmc.core.chat.listeners.CommandWhitelist;
import me.txmc.core.chat.listeners.JoinLeaveListener;
import me.txmc.core.chat.tasks.AnnouncementTask;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

@RequiredArgsConstructor
public class ChatSection implements Section {
    @Getter private final Main plugin;
    private final HashMap<UUID, ChatInfo> map = new HashMap<>();
    @Getter private ConfigurationSection config;
    @Getter private IStorage<ChatInfo, Player> chatInfoStore;

    @Override
    public void enable() {
        File dataFolder = plugin.getSectionDataFolder(this);
        if (!dataFolder.exists()) dataFolder.mkdir();
        File tldFile = new File(dataFolder, "tlds.txt");
        if (!tldFile.exists()) GlobalUtils.unpackResource("tlds.txt", tldFile);
        File ignoresFolder = new File(dataFolder, "IgnoreLists");
        chatInfoStore = new ChatFileIO(ignoresFolder, this);
        if (!ignoresFolder.exists()) ignoresFolder.mkdir();
        config = plugin.getSectionConfig(this);
        plugin.register(new JoinLeaveListener(this));
        plugin.register(new CommandWhitelist(this));
        plugin.register(new ChatListener(this, parseTLDS(tldFile)));
        plugin.getCommand("ignore").setExecutor(new IgnoreCommand(this));
        plugin.getCommand("msg").setExecutor(new MessageCommand(this));
        plugin.getCommand("reply").setExecutor(new ReplyCommand(this));
        plugin.getCommand("togglechat").setExecutor(new ToggleChatCommand(this));
        plugin.getCommand("unignore").setExecutor(new UnIgnoreCommand(this));
        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(this::registerPlayer);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new AnnouncementTask(), 60L, plugin.getConfig().getInt("AnnouncementInterval")*20);


    }

    private HashSet<String> parseTLDS(File tldFile) {
        try {
            HashSet<String> buf = new HashSet<>();
            @Cleanup BufferedReader reader = new BufferedReader(new FileReader(tldFile));
            reader.lines().filter(l -> !l.startsWith("#")).forEach(s -> buf.add(s.toLowerCase()));
            return buf;
        } catch (Throwable t) {
            GlobalUtils.log(Level.WARNING, "&cFailed to parse the TLD file please see the stacktrace below for more info!");
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void disable() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            ChatInfo ci = getInfo(p);
            if (ci != null) ci.saveChatInfo();
        });
    }

    @Override
    public void reloadConfig() {
        this.config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "ChatControl";
    }

    public void registerPlayer(Player player) {
        map.put(player.getUniqueId(), chatInfoStore.load(player));
    }

    public void removePlayer(Player player) {
        ChatInfo ci = getInfo(player);
        if (ci != null) ci.saveChatInfo();
        map.remove(player.getUniqueId());
    }

    public ChatInfo getInfo(Player player) {
        return map.getOrDefault(player.getUniqueId(), null);
    }
}
