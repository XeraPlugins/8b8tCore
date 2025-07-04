package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects and replaces legacy §-formatted text in item display names and lore.
 * Acts as a prevention layer against DFU-triggering strings in NBT.
 *
 * @author ChatGPT
 * @since 2025-06-22
 */
public class LegacyTextCheck implements Check {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        // Check display name
        if (meta.hasDisplayName()) {
            String name = GlobalUtils.getStringContent(meta.displayName());
            if (LEGACY_COLOR_PATTERN.matcher(name).find()) return true;
        }

        // Check lore
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (LEGACY_COLOR_PATTERN.matcher(line).find()) return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return;

        // Fix display name
        if (meta.hasDisplayName()) {
            String legacy = GlobalUtils.getStringContent(meta.displayName());
            String stripped = LEGACY_COLOR_PATTERN.matcher(legacy).replaceAll("");
            meta.setDisplayName(stripped);
        }

        // Fix lore
        if (meta.hasLore()) {
            List<String> oldLore = meta.getLore();
            List<String> newLore = oldLore.stream()
                    .map(line -> LEGACY_COLOR_PATTERN.matcher(line).replaceAll(""))
                    .toList();
            meta.setLore(newLore);
        }

        item.setItemMeta(meta);
    }
}