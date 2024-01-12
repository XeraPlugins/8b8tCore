package me.txmc.core.antiillegal.check.checks;

import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.Check;import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author 254n_m
 * @since 2023/09/20 8:01 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
@RequiredArgsConstructor
public class NameCheck implements Check {
    private final ConfigurationSection config;
    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta.displayName() == null) return false;
        TextComponent name = (TextComponent) meta.displayName();
        if (name.hasStyling()) return true;
        if (hasDecorations(name)) return true;
        if (name.content().length() > 50) return true;
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        TextComponent name = (TextComponent) meta.displayName();
        String strName = name.content();
        meta.displayName(Component.text(strName.substring(0, Math.min(config.getInt("MaxItemNameLength"), strName.length()))));
        item.setItemMeta(meta);
    }

    private boolean hasDecorations(TextComponent component) {
        for (TextDecoration decoration : TextDecoration.values()) {
            if (component.hasDecoration(decoration)) return true;
        }
        return false;
    }
}
