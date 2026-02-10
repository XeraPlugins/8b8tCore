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
    private final PlayerEffectCheck effectCheck;
    public static boolean debug = false;
    public static boolean informPlayer = true;

    public AntiIllegalMain(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getSectionConfig(this);
        this.effectCheck = new PlayerEffectCheck();
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
                new AntiPrefilledContainers(),
                new ContainerContentCheck(this),
                effectCheck));
    }

    public PlayerEffectCheck getEffectCheck() {
        return effectCheck;
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
                new PlayerEffectListener(plugin, this),
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

    public boolean checkFixItem(ItemStack item, Cancellable cancellable) {
        if (item == null || item.getType() == Material.AIR) return false;

        boolean isInventoryOpen = cancellable instanceof InventoryOpenEvent;
        boolean wasIllegal = false;

        for (Check check : checks) {
            if (check instanceof AntiPrefilledContainers) {
                if (!isInventoryOpen) continue;
            }

            if (!check.shouldCheck(item)) continue;
            if (!check.check(item)) continue;

            wasIllegal = true;
            if (debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "&cItem %s flagged by %s", item.getType(), check.getClass().getSimpleName());
            if (informPlayer && (cancellable instanceof org.bukkit.event.block.BlockPlaceEvent || cancellable instanceof org.bukkit.event.player.PlayerInteractEvent)) {
                org.bukkit.entity.Player player = (cancellable instanceof org.bukkit.event.block.BlockPlaceEvent bpe) ? bpe.getPlayer() : ((org.bukkit.event.player.PlayerInteractEvent) cancellable).getPlayer();
                if (player != null) {
                    try {
                        String checkName = check.getClass().getSimpleName();
                        if (check instanceof me.txmc.core.antiillegal.check.checks.ContainerContentCheck) {
                            io.papermc.paper.persistence.PersistentDataContainerView pdc = item.getPersistentDataContainer();
                            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "last_failed_check");
                            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                                checkName = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                            }
                        }
                        me.txmc.core.Localization loc = me.txmc.core.Localization.getLocalization(player.locale().getLanguage());
                        String msg = String.format(loc.get("antiillegal_flagged_placement"), checkName);
                        player.sendMessage(me.txmc.core.util.GlobalUtils.translateChars(me.txmc.core.util.GlobalUtils.getPREFIX().concat(" &r&7>>&r ").concat(msg)));
                        if (debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "Sent message to %s: %s", player.getName(), msg);
                    } catch (Throwable t) {
                        if (debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.WARNING, "Failed to send localized message: %s", t.getMessage());
                    }
                }
            }
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
        return wasIllegal;
    }
}