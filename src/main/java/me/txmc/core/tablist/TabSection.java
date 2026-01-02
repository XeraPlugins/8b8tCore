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


/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class TabSection implements Section {
    private final Main plugin;
    private ConfigurationSection config;
    private final PrefixManager prefixManager = new PrefixManager();
    private long tickCount = 0;

    public TabSection(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            tickCount++;
            boolean updatePlaceholders = (tickCount % 10 == 0);
            for (Player p : Bukkit.getOnlinePlayers()) {
                setTab(p, updatePlaceholders);
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

    public void setTab(Player player, boolean updatePlaceholders) {
        me.txmc.core.chat.ChatSection chatSection = (me.txmc.core.chat.ChatSection) plugin.getSectionByName("ChatControl");
        if (chatSection == null) return;
        me.txmc.core.chat.ChatInfo info = chatSection.getInfo(player);
        if (info == null) return;

        if (info.isUseVanillaLeaderboard()) {
            player.sendPlayerListHeader(Component.empty());
            player.sendPlayerListFooter(Component.empty());
            player.playerListName(null);
            return;
        }

        Component displayNameComponent = info.getDisplayNameComponent();
        player.displayName(displayNameComponent);

        String tag = prefixManager.getPrefix(info);
        String convertedTag = GlobalUtils.convertToMiniMessageFormat(tag);
        Component tagComponent = MiniMessage.miniMessage().deserialize(convertedTag);
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

    public void setTab(Player player) {
        setTab(player, true);
    }
}