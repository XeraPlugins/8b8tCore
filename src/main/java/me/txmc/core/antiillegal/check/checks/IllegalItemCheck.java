package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.Main;
import me.txmc.core.antiillegal.check.Check;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
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
    private final HashSet<Material> illegals;

    public IllegalItemCheck() {
        illegals = parseConfig();
    }

    @Override
    public boolean check(ItemStack item) {
        boolean listed = illegals.contains(item.getType());
        if (!listed) return false;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasEnchants()) {
                if (isPumpkin(item) || isCarvedPumpkin(item) || isHead(item) || isSkull(item)) {
                    Map<Enchantment, Integer> ench = meta.getEnchants();
                    boolean onlyCurses = true;
                    for (Enchantment e : ench.keySet()) {
                        String k = e.getKey().getKey().toLowerCase();
                        if (!k.equals("binding_curse") && !k.equals("vanishing_curse")) { onlyCurses = false; break; }
                    }
                    if (onlyCurses) return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return true;
    }

    @Override
    public void fix(ItemStack item) {
        item.setAmount(0);
    }

    private HashSet<Material> parseConfig() {
        List<String> materialNames = Arrays.stream(Material.values()).map(Material::name).toList();
        List<String> strList = Main.getInstance().getConfig().getStringList("AntiIllegal.IllegalItems");
        List<Material> output = new ArrayList<>();
        for (String raw : strList) {
            try {
                raw = raw.toUpperCase();
                if (raw.contains("*")) {
                    raw = raw.replace("*", "");
                    for (String materialName : materialNames) {
                        if (materialName.contains(raw)) output.add(Material.getMaterial(materialName));
                    }
                    continue;
                }
                Material material = Material.getMaterial(raw);
                if (material == null) throw new EnumConstantNotPresentException(Material.class, raw);
                output.add(material);
            } catch (EnumConstantNotPresentException | IllegalArgumentException e) {
                GlobalUtils.log(Level.WARNING, "&3Unknown material&r&a %s&r&3 in blocks section of the config", raw);
            }
        }
        return new HashSet<>(output);
    }

    private boolean isPumpkin(ItemStack item) { return item.getType() == Material.PUMPKIN; }
    private boolean isCarvedPumpkin(ItemStack item) { return item.getType() == Material.CARVED_PUMPKIN; }
    private boolean isHead(ItemStack item) { String n = item.getType().name(); return n.endsWith("_HEAD"); }
    private boolean isSkull(ItemStack item) { String n = item.getType().name(); return n.endsWith("_SKULL"); }

}
