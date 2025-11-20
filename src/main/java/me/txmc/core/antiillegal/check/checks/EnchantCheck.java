package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;

/**
 * @author MindComplexity
 * @since 2025/11/18
 * This file was created as a part of 8b8tAntiIllegal
 */

// https://minecraft.wiki/w/Enchanting
public class EnchantCheck implements Check {
    @Override
    public boolean check(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants()) return false;
        if (item.getType().isBlock()) {
            if (isCarvedPumpkin(item) || isPumpkin(item) || isHead(item) || isSkull(item)) {
                Map<Enchantment, Integer> enchants = meta.getEnchants();
                for (Enchantment e : enchants.keySet()) {
                    String k = keyOf(e);
                    if (!k.equals("binding_curse") && !k.equals("vanishing_curse")) return true;
                }
                return false;
            }
            return true;
        }
        Map<Enchantment, Integer> enchants = meta.getEnchants();
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            Enchantment ench = e.getKey();
            int lvl = e.getValue();
            if (lvl > ench.getMaxLevel()) return true;
            if (!ench.canEnchantItem(item)) return true;
        }
        java.util.Set<String> allowed = allowedKeysFor(item);
        for (Enchantment e : enchants.keySet()) {
            if (!allowed.contains(keyOf(e))) return true;
        }
        if (isArmor(item)) {
            int protCount = countPresent(enchants, Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION);
            if (protCount > 1) return true;
            if (isBoots(item) && enchants.containsKey(Enchantment.DEPTH_STRIDER) && enchants.containsKey(Enchantment.FROST_WALKER)) return true;
        }
        if (isSword(item) || isAxe(item)) {
            int dmgCount = countPresent(enchants, Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS);
            if (dmgCount > 1) return true;
        }
        if (isAxe(item) || isMiningTool(item)) {
            int fs = countPresent(enchants, Enchantment.FORTUNE, Enchantment.SILK_TOUCH);
            if (fs > 1) return true;
        }
        if (isBow(item)) {
            if (enchants.containsKey(Enchantment.INFINITY) && enchants.containsKey(Enchantment.MENDING)) return true;
        }
        if (isTrident(item)) {
            if (enchants.containsKey(Enchantment.RIPTIDE) && (enchants.containsKey(Enchantment.LOYALTY) || enchants.containsKey(Enchantment.CHANNELING))) return true;
        }
        if (isCrossbow(item)) {
            if (enchants.containsKey(Enchantment.PIERCING) && enchants.containsKey(Enchantment.MULTISHOT)) return true;
        }
        if (isMace(item)) {
            int maceDmg = countPresentByKeys(enchants, "smite", "bane_of_arthropods", "density", "breach");
            if (maceDmg > 1) return true;
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
        Map<Enchantment, Integer> enchants = new java.util.HashMap<>(meta.getEnchants());
        boolean changed = false;
        boolean removeAll = item.getType().isBlock() && !(isCarvedPumpkin(item) || isPumpkin(item) || isHead(item) || isSkull(item));
        for (Map.Entry<Enchantment, Integer> e : new java.util.HashMap<>(enchants).entrySet()) {
            Enchantment ench = e.getKey();
            int lvl = e.getValue();
            if (removeAll) {
                meta.removeEnchant(ench);
                enchants.remove(ench);
                changed = true;
                continue;
            }
            if (item.getType().isBlock() && (isCarvedPumpkin(item) || isPumpkin(item) || isHead(item) || isSkull(item))) {
                String k = keyOf(ench);
                if (!k.equals("binding_curse") && !k.equals("vanishing_curse")) {
                    meta.removeEnchant(ench);
                    enchants.remove(ench);
                    changed = true;
                } else if (lvl > ench.getMaxLevel()) {
                    meta.removeEnchant(ench);
                    meta.addEnchant(ench, ench.getMaxLevel(), false);
                    enchants.put(ench, ench.getMaxLevel());
                    changed = true;
                }
                continue;
            }
            if (lvl > ench.getMaxLevel()) {
                meta.removeEnchant(ench);
                meta.addEnchant(ench, ench.getMaxLevel(), false);
                enchants.put(ench, ench.getMaxLevel());
                changed = true;
            }
            if (!ench.canEnchantItem(item)) {
                meta.removeEnchant(ench);
                enchants.remove(ench);
                changed = true;
            }
        }
        java.util.Set<String> allowed = allowedKeysFor(item);
        for (Enchantment e : new java.util.HashSet<>(enchants.keySet())) {
            if (!allowed.contains(keyOf(e))) {
                meta.removeEnchant(e);
                enchants.remove(e);
                changed = true;
            }
        }
        if (isArmor(item)) {
            changed |= enforceExclusive(meta, enchants, Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION);
            if (isBoots(item) && enchants.containsKey(Enchantment.DEPTH_STRIDER) && enchants.containsKey(Enchantment.FROST_WALKER)) {
                Enchantment keep = enchants.get(Enchantment.DEPTH_STRIDER) >= enchants.get(Enchantment.FROST_WALKER) ? Enchantment.DEPTH_STRIDER : Enchantment.FROST_WALKER;
                Enchantment remove = keep == Enchantment.DEPTH_STRIDER ? Enchantment.FROST_WALKER : Enchantment.DEPTH_STRIDER;
                meta.removeEnchant(remove);
                enchants.remove(remove);
                changed = true;
            }
        }
        if (isSword(item) || isAxe(item)) {
            changed |= enforceExclusive(meta, enchants, Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS);
        }
        if (isAxe(item) || isMiningTool(item)) {
            changed |= enforceExclusive(meta, enchants, Enchantment.FORTUNE, Enchantment.SILK_TOUCH);
        }
        if (isBow(item)) {
            if (enchants.containsKey(Enchantment.INFINITY) && enchants.containsKey(Enchantment.MENDING)) {
                meta.removeEnchant(Enchantment.INFINITY);
                enchants.remove(Enchantment.INFINITY);
                changed = true;
            }
        }
        if (isTrident(item)) {
            if (enchants.containsKey(Enchantment.RIPTIDE)) {
                if (enchants.containsKey(Enchantment.LOYALTY)) { meta.removeEnchant(Enchantment.LOYALTY); enchants.remove(Enchantment.LOYALTY); changed = true; }
                if (enchants.containsKey(Enchantment.CHANNELING)) { meta.removeEnchant(Enchantment.CHANNELING); enchants.remove(Enchantment.CHANNELING); changed = true; }
            }
        }
        if (isCrossbow(item)) {
            if (enchants.containsKey(Enchantment.PIERCING) && enchants.containsKey(Enchantment.MULTISHOT)) {
                Enchantment keep = enchants.get(Enchantment.PIERCING) >= enchants.get(Enchantment.MULTISHOT) ? Enchantment.PIERCING : Enchantment.MULTISHOT;
                Enchantment remove = keep == Enchantment.PIERCING ? Enchantment.MULTISHOT : Enchantment.PIERCING;
                meta.removeEnchant(remove);
                enchants.remove(remove);
                changed = true;
            }
        }
        if (isMace(item)) {
            changed |= enforceExclusiveByKeys(meta, enchants, "smite", "bane_of_arthropods", "density", "breach");
        }
        if (changed) item.setItemMeta(meta);
    }

    private int countPresent(Map<Enchantment, Integer> enchants, Enchantment... set) {
        int c = 0;
        for (Enchantment e : set) if (enchants.containsKey(e)) c++;
        return c;
    }
    private boolean isArmor(ItemStack item) { return isHelmet(item) || isChest(item) || isLeggings(item) || isBoots(item); }
    private boolean isHelmet(ItemStack item) { String n = item.getType().name(); return n.endsWith("HELMET") || n.equals("TURTLE_HELMET"); }
    private boolean isChest(ItemStack item) { return item.getType().name().endsWith("CHESTPLATE"); }
    private boolean isLeggings(ItemStack item) { return item.getType().name().endsWith("LEGGINGS"); }
    private boolean isBoots(ItemStack item) { return item.getType().name().endsWith("BOOTS"); }
    private boolean isSword(ItemStack item) { return item.getType().name().endsWith("SWORD"); }
    private boolean isAxe(ItemStack item) { return item.getType().name().endsWith("AXE"); }
    private boolean isBow(ItemStack item) { return item.getType() == Material.BOW; }
    private boolean isCrossbow(ItemStack item) { return item.getType() == Material.CROSSBOW; }
    private boolean isTrident(ItemStack item) { return item.getType() == Material.TRIDENT; }
    private boolean isElytra(ItemStack item) { return item.getType() == Material.ELYTRA; }
    private boolean isPumpkin(ItemStack item) { return item.getType() == Material.PUMPKIN; }
    private boolean isCarvedPumpkin(ItemStack item) { return item.getType() == Material.CARVED_PUMPKIN; }
    private boolean isHead(ItemStack item) { String n = item.getType().name(); return n.endsWith("_HEAD"); }
    private boolean isSkull(ItemStack item) { String n = item.getType().name(); return n.endsWith("_SKULL"); }
    private boolean isShield(ItemStack item) { return item.getType() == Material.SHIELD; }
    private boolean isFishingRod(ItemStack item) { return item.getType() == Material.FISHING_ROD; }
    private boolean isBrush(ItemStack item) { return item.getType() == Material.BRUSH; }
    private boolean isFlint(ItemStack item) { return item.getType() == Material.FLINT_AND_STEEL; }
    private boolean isCarrotRod(ItemStack item) { return item.getType() == Material.CARROT_ON_A_STICK; }
    private boolean isWarpedRod(ItemStack item) { return item.getType() == Material.WARPED_FUNGUS_ON_A_STICK; }
    private boolean isCompass(ItemStack item) { return item.getType() == Material.COMPASS || item.getType() == Material.RECOVERY_COMPASS; }
    private boolean isShears(ItemStack item) { return item.getType() == Material.SHEARS; }
    private boolean isMiningTool(ItemStack item) { String n = item.getType().name(); return n.endsWith("PICKAXE") || n.endsWith("SHOVEL") || n.endsWith("HOE"); }
    private boolean isMace(ItemStack item) { try { return item.getType().name().equals("MACE"); } catch (Exception ignored) { return false; } }
    private boolean enforceExclusive(ItemMeta meta, Map<Enchantment, Integer> enchants, Enchantment... set) {
        int bestLevel = -1; Enchantment best = null;
        for (Enchantment e : set) { Integer lvl = enchants.get(e); if (lvl != null && lvl > bestLevel) { bestLevel = lvl; best = e; } }
        boolean changed = false;
        if (best != null) {
            for (Enchantment e : set) {
                if (e != best && enchants.containsKey(e)) {
                    meta.removeEnchant(e);
                    enchants.remove(e);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean enforceExclusiveByKeys(ItemMeta meta, Map<Enchantment, Integer> enchants, String... keys) {
        int bestLevel = -1; Enchantment best = null;
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            String k = keyOf(e.getKey());
            for (String target : keys) {
                if (k.equals(target) && e.getValue() > bestLevel) { bestLevel = e.getValue(); best = e.getKey(); }
            }
        }
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> e : new java.util.HashMap<>(enchants).entrySet()) {
            String k = keyOf(e.getKey());
            if (best != null && e.getKey() != best) {
                for (String target : keys) {
                    if (k.equals(target)) {
                        meta.removeEnchant(e.getKey());
                        enchants.remove(e.getKey());
                        changed = true;
                        break;
                    }
                }
            }
        }
        return changed;
    }

    private String keyOf(Enchantment e) { return e.getKey().getKey().toLowerCase(); }

    private int countPresentByKeys(Map<Enchantment, Integer> enchants, String... keys) {
        int c = 0;
        for (Enchantment e : enchants.keySet()) {
            String k = keyOf(e);
            for (String target : keys) if (k.equals(target)) { c++; break; }
        }
        return c;
    }

    private java.util.Set<String> baseKeys(String... keys) {
        return new java.util.HashSet<>(java.util.Arrays.asList(keys));
    }

    private java.util.Set<String> allowedKeysFor(ItemStack item) {
        if (isHelmet(item)) return baseKeys(
                "mending","unbreaking","thorns","respiration","aqua_affinity","binding_curse","vanishing_curse",
                "protection","projectile_protection","fire_protection","blast_protection");
        if (isChest(item)) return baseKeys(
                "mending","unbreaking","thorns","binding_curse","vanishing_curse",
                "protection","projectile_protection","fire_protection","blast_protection");
        if (isLeggings(item)) return baseKeys(
                "mending","unbreaking","thorns","swift_sneak","binding_curse","vanishing_curse",
                "protection","projectile_protection","fire_protection","blast_protection");
        if (isBoots(item)) return baseKeys(
                "mending","unbreaking","thorns","feather_falling","soul_speed","binding_curse","vanishing_curse",
                "protection","projectile_protection","fire_protection","blast_protection",
                "depth_strider","frost_walker");
        if (isSword(item)) return baseKeys(
                "mending","unbreaking","fire_aspect","looting","knockback","sweeping_edge","vanishing_curse",
                "sharpness","smite","bane_of_arthropods");
        if (isAxe(item)) return baseKeys(
                "mending","unbreaking","efficiency","vanishing_curse","fortune","silk_touch",
                "sharpness","smite","bane_of_arthropods","cleaving");
        if (isMiningTool(item)) return baseKeys(
                "mending","unbreaking","efficiency","vanishing_curse","fortune","silk_touch");
        if (isBow(item)) return baseKeys(
                "unbreaking","power","punch","flame","vanishing_curse","infinity","mending");
        if (isCrossbow(item)) return baseKeys(
                "mending","unbreaking","quick_charge","vanishing_curse","piercing","multishot");
        if (isTrident(item)) return baseKeys(
                "mending","unbreaking","impaling","vanishing_curse","channeling","loyalty","riptide");
        if (isFishingRod(item)) return baseKeys(
                "mending","unbreaking","lure","luck_of_the_sea","vanishing_curse");
        if (isShears(item)) return baseKeys(
                "mending","unbreaking","efficiency","vanishing_curse");
        if (isElytra(item)) return baseKeys(
                "mending","unbreaking","binding_curse","vanishing_curse");
        if (isShield(item) || isFlint(item) || isCarrotRod(item) || isWarpedRod(item) || isBrush(item)) return baseKeys(
                "mending","unbreaking","vanishing_curse");
        if (isCompass(item)) return baseKeys(
                "vanishing_curse");
        if (isMace(item)) return baseKeys(
                "mending","unbreaking","fire_aspect","wind_burst","vanishing_curse",
                "smite","bane_of_arthropods","density","breach");
        return new java.util.HashSet<>();
    }
}
