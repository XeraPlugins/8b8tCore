package me.txmc.core.tablist;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.tablist.listeners.PlayerJoinListener;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import me.txmc.core.util.FoliaCompat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author MindComplexity (aka Libalpm)
 * @since 2026/03/22
 * This file was created as a part of 8b8tCore
*/

@RequiredArgsConstructor
public class TabSection implements Section {
    private final Main plugin;
    private ConfigurationSection config;
    private final PrefixManager prefixManager = new PrefixManager();
    private long tickCount = 0;

    private me.txmc.core.chat.ChatSection cachedChatSection;
    private final java.util.Map<String, Component> tagCache = new ConcurrentHashMap<>();
    private final java.util.Map<String, Localization> localeCache = new ConcurrentHashMap<>();

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

                for (Player p : Bukkit.getOnlinePlayers()) {
                    setTab(p, updatePlaceholders, animTick);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Error in TabList update task", t);
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
        tagCache.clear();
        localeCache.clear();
    }

    @Override
    public String getName() {
        return "TabList";
    }

    public Main getPlugin() {
        return plugin;
    }

    public void setTab(Player player, boolean updatePlaceholders, long animTick) {
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
        if (displayNameComponent == null) displayNameComponent = Component.empty();

        String tag = prefixManager.getPrefix(info, animTick);
        if (tag != null && !tag.isEmpty()) {
            Component tagComponent = tagCache.computeIfAbsent(tag, t -> {
                String converted = GlobalUtils.convertToMiniMessageFormat(t);
                return MiniMessage.miniMessage().deserialize(converted);
            });
            displayNameComponent = tagComponent.append(displayNameComponent);
        }

        player.playerListName(displayNameComponent);

        if (updatePlaceholders) {
            java.util.Locale locale = player.locale();
            String lang = (locale != null) ? locale.getLanguage() : "en";
            Localization loc = localeCache.computeIfAbsent(lang, Localization::getLocalization);
            Component header = Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Header")), player, plugin.getStartTime());
            Component footer = Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Footer")), player, plugin.getStartTime());
            FoliaCompat.schedule(player, plugin, () -> {
                if (!player.isOnline()) return;
                player.sendPlayerListHeader(header);
                player.sendPlayerListFooter(footer);
            });
        }
    }

    public void setTab(Player player, boolean updatePlaceholders) {
        setTab(player, updatePlaceholders, me.txmc.core.util.GradientAnimator.getAnimationTick());
    }

    public void setTab(Player player) {
        setTab(player, true, me.txmc.core.util.GradientAnimator.getAnimationTick());
    }
}
