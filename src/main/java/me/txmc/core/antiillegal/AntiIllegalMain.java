/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity
 * @since 01/02/2026
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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Cancellable;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class AntiIllegalMain implements Section {
    private final Main plugin;
    private final List<Check> checks;
    private ConfigurationSection config;

    public AntiIllegalMain(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getSectionConfig(this);
        this.checks = new ArrayList<>(Arrays.asList(
                new OverStackCheck(),
                new DurabilityCheck(),
                new AttributeCheck(),
                new EnchantCheck(),
                new PotionCheck(),
                new BookCheck(),
                new LegacyTextCheck(),
                new IllegalItemCheck(),
                new IllegalDataCheck(),
                new AntiPrefilledContainers()));
    }

    @Override
    public void enable() {
        if (config == null) config = plugin.getSectionConfig(this);
        checks.add(new NameCheck(config));

        plugin.register(
                new PlayerListeners(this),
                new MiscListeners(this),
                new InventoryListeners(this),
                new AttackListener(plugin),
                new PlayerEffectListener(plugin),
                new EntityEffectListener(plugin));

        if (config.getBoolean("EnableIllegalBlocksCleaner", true)) {
            plugin.register(new IllegalBlocksCleaner(plugin, config));
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

        boolean isInventoryOpen = cancellable instanceof InventoryOpenEvent;

        for (Check check : checks) {
            if (check instanceof AntiPrefilledContainers) {
                if (!isInventoryOpen) continue;
            }

            if (!check.shouldCheck(item)) continue;
            if (!check.check(item)) continue;

            check.fix(item);
            
            if (item.hasItemMeta()) {
                item.setItemMeta(item.getItemMeta());
            }

            if (item.getType() == Material.AIR || item.getAmount() <= 0) {
                if (cancellable instanceof io.papermc.paper.event.block.BlockPreDispenseEvent) {
                    cancellable.setCancelled(true);
                }
            }
        }
    }
}