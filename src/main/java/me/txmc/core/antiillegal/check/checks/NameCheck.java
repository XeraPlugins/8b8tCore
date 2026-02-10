package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.registry.keys.DataComponentTypeKeys;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author MindComplexity
 * @since 2026/01/04
 * Strict Name & EntityData check to prevent client-side crashes and overflows.
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
        if (item == null || item.getType() == Material.AIR) return false;

        if (item.hasData(ENTITY_DATA) && !item.getType().name().contains("SHULKER_BOX")) {
            if (me.txmc.core.antiillegal.AntiIllegalMain.debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "&cNameCheck flagged item %s for having ENTITY_DATA", item.getType());
            return true;
        }

        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        boolean illegal = isIllegalNameContent(meta.displayName());
        if (illegal) {
            if (me.txmc.core.antiillegal.AntiIllegalMain.debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "&cNameCheck flagged item %s for illegal name content: %s", item.getType(), me.txmc.core.util.GlobalUtils.getStringContent(meta.displayName()));
        }
        return illegal;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item != null && (item.hasItemMeta() || item.hasData(ENTITY_DATA));
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null) return;

        if (item.hasData(ENTITY_DATA)) {
            item.unsetData(ENTITY_DATA);
        }

        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            if (isIllegalNameContent(meta.displayName())) {
                meta.displayName(null);
                item.setItemMeta(meta);
            }
        }
    }

    private boolean isIllegalNameContent(Component component) {
        if (component == null) return false;

        // Check Length First before running the expensive serialization call.
        String content = GlobalUtils.getStringContent(component);
        if (content.length() > STRICT_MAX_LENGTH) return true;

        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            if (Character.isISOControl(cp) || Character.getType(cp) == Character.PRIVATE_USE) {
                return true;
            }
            i += Character.charCount(cp);
        }

        String json = GsonComponentSerializer.gson().serialize(component);
        return json.length() > MAX_JSON_LENGTH;
    }
}
