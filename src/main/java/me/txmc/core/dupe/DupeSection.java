package me.txmc.core.dupe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.dupe.command.DupeCommand;
import me.txmc.core.dupe.framedupe.FrameDupe;

import me.txmc.core.dupe.zombiedupe.ZombieDupe;
import me.txmc.core.home.commands.HomeCommand;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * @author Minelord9000
 * @since 2024/30/07 12:36 PM
 * This file was created as a part of 8b8tCore
 */

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class DupeSection implements Section {
    private final Main plugin;
    private ConfigurationSection config;

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        plugin.register(new FrameDupe(plugin));
        plugin.register(new ZombieDupe(plugin));

        plugin.getCommand("dupe").setExecutor(new DupeCommand(this));
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
        return "Dupe";
    }

    private String getItemName(ItemStack itemStack) {
        return (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) ? GlobalUtils.getStringContent(itemStack.getItemMeta().displayName()) : itemStack.getType().name();
    }
}
