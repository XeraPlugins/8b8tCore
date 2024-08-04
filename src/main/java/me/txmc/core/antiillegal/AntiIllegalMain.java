package me.txmc.core.antiillegal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.antiillegal.check.checks.*;
import me.txmc.core.antiillegal.listeners.*;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/17 9:44 PM
 * This file was created as a part of 8b8tCore
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
            new LoreCheck(),
            new EnchantCheck(),
            new PotionCheck(),
            new BookCheck(),
            new IllegalItemCheck()
    ));

    private ConfigurationSection config;

    @Override
    public void enable() {

        config = plugin.getSectionConfig(this);
        checks.add(new NameCheck(config));
//        checks.add(new ItemSizeCheck());

        plugin.register(new PlayerListeners(this), new MiscListeners(this), new InventoryListeners(this), new AttackListener(), new StackedTotemsListener());
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
        return "AntiIllegal";
    }

    public void checkFixItem(ItemStack item, Cancellable cancellable) {
        if (item == null || item.getType() == Material.AIR) return;
        for (Check check : checks) {
            if (check.shouldCheck(item) && check.check(item)) {
                if (cancellable != null && !cancellable.isCancelled()) cancellable.setCancelled(true);
                GlobalUtils.log(Level.INFO, "Item %s failed the %s check and has been fixed.", getItemName(item), check.getClass().getSimpleName());
                check.fix(item);
                item.setItemMeta(item.getItemMeta());
            }
        }
    }
    private String getItemName(ItemStack itemStack) {
        return (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) ? GlobalUtils.getStringContent(itemStack.getItemMeta().displayName()) : itemStack.getType().name();
    }
}
