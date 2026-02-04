package me.txmc.core.dupe.command;

import lombok.RequiredArgsConstructor;
import me.txmc.core.dupe.DupeSection;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * This file is apart of 8b8tcore.
 * @author MindComplexity (Libalpm)
 * @since 2026/02/04
 */

@RequiredArgsConstructor
public class DupeCommand implements CommandExecutor {

    private final DupeSection main;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final String PERM_NORMAL = "8b8tcore.command.dupe";
    private static final String PERM_FULL = "8b8tcore.command.fulldupe";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cYou must be a player to use this command.");
            return true;
        }

        if (!main.plugin().getConfig().getBoolean("DupeCommand.enabled", false)) return true;
        
        boolean votersOnly = main.plugin().getConfig().getBoolean("DupeCommand.votersOnly", false);
        if (votersOnly && !player.hasPermission("8b8tcore.dupe.command")) return true;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendPrefixedLocalizedMessage(player, "dupe_failed");
            return true;
        }

        if (player.hasPermission(PERM_FULL) || player.isOp()) {
            handleFullDupe(player, item, args);
        } else if (player.hasPermission(PERM_NORMAL)) {
            handleNormalDupe(player, item);
        } else {
            sendPrefixedLocalizedMessage(player, "dupe_permission");
            player.setHealth(0);
        }

        return true;
    }

    private void handleFullDupe(Player player, ItemStack item, String[] args) {
        if (checkCooldown(player, 5000L)) return;

        int copies = 1;
        if (args.length > 0) {
            try {
                copies = Integer.parseInt(args[0]);
                if (copies < 1 || copies > 9) copies = 1;
            } catch (NumberFormatException ignored) {}
        }
        processDuplication(player, item, copies, 60);
    }

    private void handleNormalDupe(Player player, ItemStack item) {
        if (checkCooldown(player, 30000L)) return;
        processDuplication(player, item, 1, 9);
    }

    private boolean checkCooldown(Player player, long cooldownTime) {
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() - lastUsed < cooldownTime) {
            sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
            return true;
        }
        return false;
    }

    private void processDuplication(Player player, ItemStack item, int copies, int chunkLimit) {
        int currentItems = countItemsInChunk(player.getLocation().getChunk());

        if (currentItems + copies > chunkLimit) {
            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
            return;
        }

        for (int i = 0; i < copies; i++) {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }

        sendPrefixedLocalizedMessage(player, "dupe_success");
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private int countItemsInChunk(Chunk chunk) {
        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item) {
                count++;
            }
        }
        return count;
    }
}