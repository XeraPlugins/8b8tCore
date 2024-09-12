package me.txmc.core.tablist;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.tablist.listeners.PlayerJoinListener;
import me.txmc.core.tablist.util.Utils;
import me.txmc.core.tablist.worker.TabWorker;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
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
    private final PrefixManager prefixManager = new PrefixManager();

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
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

        String tag = prefixManager.getPrefix(player);

        tag = GlobalUtils.convertToMiniMessageFormat(tag);
        Component displayNameComponent = player.displayName();


        Component tagComponent = MiniMessage.miniMessage().deserialize(tag);

        Component fullName = tagComponent.append(displayNameComponent);

        player.playerListName(fullName);

        Localization loc = Localization.getLocalization(player.locale().getLanguage());
        Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Header")), player, plugin.getStartTime())
                .thenAccept(player::sendPlayerListHeader);
        Utils.parsePlaceHolders(String.join("\n", loc.getStringList("TabList.Footer")), player, plugin.getStartTime())
                .thenAccept(player::sendPlayerListFooter);
    }
}