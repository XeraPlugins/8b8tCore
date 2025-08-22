/*
 * AntiIllegalMain.java
 */
package me.txmc.core.antiillegal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.antiillegal.check.checks.*;
import me.txmc.core.antiillegal.listeners.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main section for AntiIllegal features.
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class AntiIllegalMain implements Section {
    private final Main plugin;
    private final List<Check> checks = new ArrayList<>(Arrays.asList(
            new OverStackCheck(),
            new DurabilityCheck(),
            new AttributeCheck(),
            new EnchantCheck(),
            new PotionCheck(),
            new BookCheck(),
            new LegacyTextCheck(),
            new StackedTotemCheck(),
            new ShulkerCeptionCheck(),
            new IllegalItemCheck(),
            new antiprefilledchests()
    ));

    private ConfigurationSection config;

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        checks.add(new NameCheck(config));

        plugin.register(
                new PlayerListeners(this),
                new MiscListeners(this),
                new InventoryListeners(this),
                new AttackListener(),
                new StackedTotemsListener(),
                new PlayerEffectListener(plugin)
        );

        if (config.getBoolean("EnableIllegalBlocksCleaner", true)) {
            List<String> illegalBlocks = config.getStringList("IllegalBlocks");
            plugin.register(new IllegalBlocksCleaner(plugin, illegalBlocks));
        }
    }

    @Override
    public void disable() {}

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "AntiIllegal";
    }

    public void checkFixItem(ItemStack item, Cancellable cancellable) {
        if (item == null || item.getType() == Material.AIR) return;
        for (Check check : checks) {
            if (check.shouldCheck(item) && check.check(item)) {
                if (cancellable != null && !cancellable.isCancelled()) cancellable.setCancelled(true);
                check.fix(item);
                item.setItemMeta(item.getItemMeta());
            }
        }
    }
}
