package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/10/13 8:57 PM
 * This file was created as a part of 8b8tAntiIllegal
 */
public class IllegalItemCheck implements Check {
    private final HashSet<Material> illegals;

    public IllegalItemCheck(ConfigurationSection config) {
        illegals = parseConfig(config);
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

    private HashSet<Material> parseConfig(ConfigurationSection config) {
        List<String> materialNames = Arrays.stream(Material.values()).map(Material::name).toList();
        List<String> strList = config.getStringList("IllegalItems");
        HashSet<Material> output = new HashSet<>();
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
        return output;
    }
}
