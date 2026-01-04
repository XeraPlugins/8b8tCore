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
import me.txmc.core.chat.listeners.VanishTabListener;
import me.txmc.core.util.GlobalUtils;
import me.txmc.core.database.GeneralDatabase;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


@RequiredArgsConstructor
public class ChatSection implements Section {
    @Getter private final Main plugin;
    private final ConcurrentHashMap<UUID, ChatInfo> map = new ConcurrentHashMap<>();
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
        plugin.register(new VanishTabListener(this.plugin));
        plugin.getCommand("ignore").setExecutor(new ChatCommands.IgnoreCommand(this));
        plugin.getCommand("msg").setExecutor(new ChatCommands.MessageCommand(this));
        plugin.getCommand("reply").setExecutor(new ChatCommands.ReplyCommand(this));
        plugin.getCommand("togglechat").setExecutor(new ChatCommands.ToggleChatCommand(this));
        plugin.getCommand("unignore").setExecutor(new ChatCommands.UnIgnoreCommand(this));
        plugin.getCommand("ignorelist").setExecutor(new ChatCommands.IgnoreListCommand(this));
        if (!Bukkit.getOnlinePlayers().isEmpty()) Bukkit.getOnlinePlayers().forEach(this::registerPlayer);
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
        ChatInfo info = chatInfoStore.load(player);
        map.put(player.getUniqueId(), info);
        loadAllDataAsync(info);
    }

    public void loadAllDataAsync(ChatInfo info) {
        String username = info.getPlayer().getName();
        GeneralDatabase db = GeneralDatabase.getInstance();
        me.txmc.core.customexperience.util.PrefixManager pm = new me.txmc.core.customexperience.util.PrefixManager();

        db.getMutedUntilAsync(username).thenAccept(info::setMutedUntil);
        pm.refreshPrefixDataAsync(info);

        java.util.concurrent.CompletableFuture.allOf(
            db.getNicknameAsync(username).thenAccept(info::setNickname),
            db.isVanillaLeaderboardAsync(username).thenAccept(info::setUseVanillaLeaderboard),
            db.getCustomGradientAsync(username).thenAccept(info::setNameGradient),
            db.getGradientAnimationAsync(username).thenAccept(info::setNameAnimation),
            db.getGradientSpeedAsync(username).thenAccept(info::setNameSpeed),
            db.getPlayerDataAsync(username, "nameDecorations").thenAccept(info::setNameDecorations)
        ).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
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
