package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.keys.DataComponentTypeKeys;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Pattern;

/**
 * @author MindComplexity
 * @since 2026/01/04
 * This file was created as a part of 8b8tAntiIllegal
 */

@RequiredArgsConstructor
public class NameCheck implements Check {

    private final ConfigurationSection config;

    private static final int STRICT_MAX_LENGTH = 255;
    private static final int MAX_JSON_LENGTH = 8192;

    private static final DataComponentType ENTITY_DATA =
            Registry.DATA_COMPONENT_TYPE.getOrThrow(DataComponentTypeKeys.ENTITY_DATA);

    @Override
    public boolean check(ItemStack item) {
        if (item == null) return false;

        if (hasIllegalEntityData(item)) return true;

        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;

        Component name = meta.displayName();

        return isIllegalNameContent(name);
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null) return;

        if (hasIllegalEntityData(item)) {
            item.unsetData(ENTITY_DATA);
        }

        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        Component name = meta.displayName();

        if (!isIllegalNameContent(name)) return;

        meta.displayName(null);
        item.setItemMeta(meta);
    }

    private boolean isIllegalNameContent(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        if (json.length() > MAX_JSON_LENGTH) return true;

        String content = GlobalUtils.getStringContent(component);
        if (content.length() > STRICT_MAX_LENGTH) return true;
        
        return content.codePoints().anyMatch(cp -> 
                Character.isISOControl(cp) || 
                Character.getType(cp) == Character.PRIVATE_USE
        );
    }

    private boolean hasIllegalEntityData(ItemStack item) {
        return item.hasData(ENTITY_DATA);
    }
}