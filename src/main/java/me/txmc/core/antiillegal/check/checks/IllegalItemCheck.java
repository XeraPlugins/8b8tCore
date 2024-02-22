package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.Main;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/10/13 8:57 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class IllegalItemCheck implements Check {
    private final List<Material> illegals;

    public IllegalItemCheck() {
        illegals = parseConfig();
    }
    @Override
    public boolean check(ItemStack item) {
        return illegals.contains(item.getType()); // O(1) thanks HashSet
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        item.setAmount(0);
    }

    private List<Material> parseConfig() {
        List<String> materialNames = Arrays.stream(Material.values()).map(Material::name).toList();
        List<String> strList = Main.getInstance().getConfig().getStringList("AntiIllegal.IllegalItems");
        List<Material> output = new ArrayList<>();
        for (String raw : strList) {
            try {
                raw = raw.toUpperCase();
                if (raw.contains("*")) {
                    raw = raw.replace("*", "");
                    for (String materialName : materialNames) {
                        if (materialName.contains(raw)) output.add(Material.getMaterial(materialName, false));
                    }
                    continue;
                }
                Material material = Material.getMaterial(raw, false);
                if (material == null) throw  new EnumConstantNotPresentException(Material.class, raw);
                output.add(material);
            } catch (EnumConstantNotPresentException | IllegalArgumentException e) {
                GlobalUtils.log(Level.WARNING, "&3Unknown material&r&a %s&r&3 in blocks section of the config", raw);
            }
        }
        return Collections.unmodifiableList(output);
    }
}
