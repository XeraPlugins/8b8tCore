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
import me.txmc.core.antiillegal.listeners.EntityEffectListener;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
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
            new IllegalDataCheck(),
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
                new PlayerEffectListener(plugin),
                new EntityEffectListener(plugin)
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
        String enchants = (item.hasItemMeta() && item.getItemMeta().hasEnchants()) ? item.getItemMeta().getEnchants().toString() : "{}";
        String evt = cancellable != null ? cancellable.getClass().getSimpleName() : "none";
        // Bukkit.getConsoleSender().sendMessage("[AntiIllegal] Checking type=" + item.getType() + " amount=" + item.getAmount() + " enchants=" + enchants + " event=" + evt);

        for (Check check : checks) {
            boolean should = check.shouldCheck(item);
            boolean fail = should && check.check(item);
            if (fail) {
                // Bukkit.getConsoleSender().sendMessage("[AntiIllegal] FAILED check=" + check.getClass().getSimpleName() + " type=" + item.getType() + " event=" + evt);

                
                // fix server crash
                // Without this the stack value would be be mutated and the location address 
                // would be removed for the item so when Folia or Paper tries to call this 
                // when dispensing the address doesn't exist crashing the server.
                if (cancellable instanceof io.papermc.paper.event.block.BlockPreDispenseEvent) {
                    cancellable.setCancelled(true);
                    return;
                }
                
                check.fix(item);
                item.setItemMeta(item.getItemMeta());
                String after = (item.hasItemMeta() && item.getItemMeta().hasEnchants()) ? item.getItemMeta().getEnchants().toString() : "{}";
                // Bukkit.getConsoleSender().sendMessage("[AntiIllegal] Fixed by=" + check.getClass().getSimpleName() + " newAmount=" + item.getAmount() + " enchants=" + after);
            }
        }
    }
}
