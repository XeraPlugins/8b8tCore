package me.txmc.core.antiillegal.check.checks;

import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
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
        Component name =  meta.displayName();
        if (name.hasStyling()) return false;
        if (hasDecorations(name)) return false;
        if (GlobalUtils.getStringContent(name).length() > 50) return true;
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Component name = meta.displayName();
        String strName = GlobalUtils.getStringContent(name);
        meta.displayName(Component.text(strName.substring(0, Math.min(config.getInt("MaxItemNameLength"), strName.length()))));
        item.setItemMeta(meta);
    }

    private boolean hasDecorations(Component component) {
        for (TextDecoration decoration : TextDecoration.values()) {
            if (component.hasDecoration(decoration)) return true;
        }
        return false;
    }
}
