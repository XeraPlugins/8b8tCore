package me.txmc.core.antiillegal.check.checks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import lombok.RequiredArgsConstructor;
import me.txmc.core.antiillegal.AntiIllegalMain;
import me.txmc.core.antiillegal.check.Check;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author MindComplexity
 * @since 2026/02/09
 * Checks if a container has illegal items inside.
 */
@RequiredArgsConstructor
public class ContainerContentCheck implements Check {
    private final AntiIllegalMain main;

    @Override
    public boolean check(ItemStack item) {
        if (item == null || !item.hasData(DataComponentTypes.CONTAINER)) return false;
        ItemContainerContents contents = item.getData(DataComponentTypes.CONTAINER);
        if (contents == null) return false;
        
        for (ItemStack content : contents.contents()) {
            if (content == null || content.getType().isAir()) continue;
            for (Check check : main.checks()) {
                if (check == this || check instanceof AntiPrefilledContainers) continue;
                // Fix some Items because it doesn't really matter it's not a collectible
                if (check instanceof EnchantCheck || check instanceof IllegalDataCheck) continue;
                if (check.shouldCheck(content) && check.check(content)) {
                    if (me.txmc.core.antiillegal.AntiIllegalMain.debug) me.txmc.core.util.GlobalUtils.log(java.util.logging.Level.INFO, "&cContainerContentCheck flagged shulker because of item %s flagged by %s", content.getType(), check.getClass().getSimpleName());
                    item.editPersistentDataContainer(pdc -> {
                        pdc.set(new org.bukkit.NamespacedKey(main.plugin(), "last_failed_check"), org.bukkit.persistence.PersistentDataType.STRING, check.getClass().getSimpleName());
                    });
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item != null && item.hasData(DataComponentTypes.CONTAINER);
    }

    @Override
    public void fix(ItemStack item) {
        if (item == null || !item.hasData(DataComponentTypes.CONTAINER)) return;
        ItemContainerContents contents = item.getData(DataComponentTypes.CONTAINER);
        if (contents == null) return;

        List<ItemStack> newContents = new ArrayList<>();
        boolean changed = false;
        for (ItemStack content : contents.contents()) {
            if (content == null || content.getType().isAir()) {
                newContents.add(content);
                continue;
            }
            for (Check check : main.checks()) {
                if (check == this || check instanceof AntiPrefilledContainers) continue;
                if (check.shouldCheck(content) && check.check(content)) {
                    // Only fix if it's an EnchantCheck or IllegalDataCheck (for names)
                    if (check instanceof EnchantCheck || check instanceof IllegalDataCheck) {
                        check.fix(content);
                        changed = true;
                    }
                }
            }
            newContents.add(content);
        }
        if (changed) {
            item.setData(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(newContents));
        }
    }
}
