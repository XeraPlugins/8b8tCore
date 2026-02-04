package me.txmc.core.tablist;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.tablist.listeners.PlayerJoinListener;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;


/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

@RequiredArgsConstructor
public class TabSection implements Section {
    private final Main plugin;
    private ConfigurationSection config;
    private final PrefixManager prefixManager = new PrefixManager();
    private long tickCount = 0;

    private me.txmc.core.chat.ChatSection cachedChatSection;

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        int interval = (config != null) ? config.getInt("UpdateInterval", 1) : 1;
        if (interval < 1) interval = 1;

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            try {
                tickCount++;
                if (tickCount % 4 == 0) return;

                if (cachedChatSection == null) {
                    cachedChatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
                    if (cachedChatSection == null) return;
                }

                boolean updatePlaceholders = (tickCount % 10 == 0); 
                long animTick = me.txmc.core.util.GradientAnimator.getAnimationTick();
                
                java.util.Map<String, Component> prefixCache = new java.util.HashMap<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    setTab(p, updatePlaceholders, animTick, prefixCache);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Error in TabList update task: " + t.getMessage());
            }
        }, 1L, 1L);
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

    public Main getPlugin() {
        return plugin;
    }

    public void setTab(Player player, boolean updatePlaceholders, long animTick, java.util.Map<String, Component> prefixCache) {
        if (cachedChatSection == null) {
            cachedChatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
        }
        if (cachedChatSection == null) return;
        me.txmc.core.chat.ChatInfo info = cachedChatSection.getInfo(player);
        if (info == null || !info.isDataLoaded()) return;

        if (info.isUseVanillaLeaderboard()) {
            player.sendPlayerListHeader(Component.empty());
            player.sendPlayerListFooter(Component.empty());
            player.playerListName(null);
            return;
        }

        Component displayNameComponent = info.getDisplayNameComponent(animTick);
        
        String tag = prefixManager.getPrefix(info, animTick);
        Component tagComponent = prefixCache.computeIfAbsent(tag, t -> {
            String converted = GlobalUtils.convertToMiniMessageFormat(t);
            return MiniMessage.miniMessage().deserialize(converted);
        });

        Component fullName = tagComponent.append(displayNameComponent);
        player.playerListName(fullName);

        if (updatePlaceholders) {
            Localization loc = Localization.getLocalization(player.locale().getLanguage());
            Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Header")), player, plugin.getStartTime())
                    .thenAccept(player::sendPlayerListHeader);
            Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Footer")), player, plugin.getStartTime())
                    .thenAccept(player::sendPlayerListFooter);
        }
    }

    public void setTab(Player player, boolean updatePlaceholders) {
        setTab(player, updatePlaceholders, me.txmc.core.util.GradientAnimator.getAnimationTick(), new java.util.HashMap<>());
    }

    public void setTab(Player player) {
        setTab(player, true, me.txmc.core.util.GradientAnimator.getAnimationTick(), new java.util.HashMap<>());
    }
}