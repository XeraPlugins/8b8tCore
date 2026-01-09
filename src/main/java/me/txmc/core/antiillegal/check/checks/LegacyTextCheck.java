package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import java.util.List;
import java.util.regex.Pattern;


/**
 * @author MindComplexity (aka Libalpm)
 * @since 2025/12/21
 * This file was created as a part of 8b8tCore
*/

public class LegacyTextCheck implements Check {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIGNED_ITEM_PATTERN = Pattern.compile("§[0-9a-f]by §[0-9a-f]@[^§\n\r]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOK_AUTHOR_PATTERN = Pattern.compile("§[0-9a-f]Author: §[0-9a-f][^§\n\r]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAP_AUTHOR_PATTERN = Pattern.compile("§7by @[^§\n\r]*", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        if (isLegitimatelySigned(meta)) {
            return false; 
        }

        if (meta.hasDisplayName()) {
            String name = GlobalUtils.getStringContent(meta.displayName());
            if (LEGACY_COLOR_PATTERN.matcher(name).find()) return true;
        }

        if (meta.hasLore()) {
            for (Component lineComp : meta.lore()) {
                String line = GlobalUtils.getStringContent(lineComp);
                if (LEGACY_COLOR_PATTERN.matcher(line).find()) return true;
            }
        }

        return false;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (isLegitimatelySigned(meta)) {
            return;
        }

        if (meta.hasDisplayName()) {
            String legacy = GlobalUtils.getStringContent(meta.displayName());
            String stripped = LEGACY_COLOR_PATTERN.matcher(legacy).replaceAll("");
            meta.displayName(Component.text(stripped));
        }

        if (meta.hasLore()) {
            List<Component> oldLore = meta.lore();
            List<Component> newLore = oldLore.stream()
                    .<Component>map(lineComp -> {
                        String line = GlobalUtils.getStringContent(lineComp);
                        return Component.text(LEGACY_COLOR_PATTERN.matcher(line).replaceAll(""));
                    })
                    .toList();
            meta.lore(newLore);
        }

        item.setItemMeta(meta);
    }

    private boolean isLegitimatelySigned(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) return false;
        for (Component lineComp : meta.lore()) {
            String line = GlobalUtils.getStringContent(lineComp);
            if (SIGNED_ITEM_PATTERN.matcher(line).find() || 
                BOOK_AUTHOR_PATTERN.matcher(line).find() ||
                MAP_AUTHOR_PATTERN.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }
}
