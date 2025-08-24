package me.txmc.core.dupe.command;

import lombok.AllArgsConstructor;
import me.txmc.core.dupe.DupeSection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendMessage;
import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Handles the /dupe command which allows players to duplicate items in their hand.
 *
 * <p>This command is part of the 8b8tCore plugin and can be used by players with the appropriate permissions to duplicate
 * items in their inventory or kill himself.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Checking if the player has the required permission or is an operator</li>
 *     <li>Duplicating the item in hand and dropping it in the world</li>
 *     <li>Enforcing a limit on the number of items that can be present in a chunk</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/01 11:28 AM
 */

@AllArgsConstructor
public class DupeCommand implements CommandExecutor {
    private final DupeSection main;
    private final Map<UUID, Long> lastDupeTimes = new HashMap<>();
    private static final String PERMISSION = "8b8tcore.command.dupe";
    private static final String FULLPERMISSION = "8b8tcore.command.fulldupe";
    private static final long DUPE_COOLDOWN = 30000;
    private static final long FULLDUPE_COOLDOWN = 5000;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (sender instanceof Player player) {
            
            final boolean VOTERS_ONLY = main.plugin().getConfig().getBoolean("DupeCommand.votersOnly", false);
            if (VOTERS_ONLY && !player.hasPermission("8b8tcore.dupe.command")) return true;

            UUID playerId = player.getUniqueId();

            long currentTime = System.currentTimeMillis();
            long lastDupeTime = lastDupeTimes.getOrDefault(playerId, 0L);

            if (player.hasPermission(FULLPERMISSION) || player.isOp()) {

                if (currentTime - lastDupeTime < FULLDUPE_COOLDOWN) {
                    sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
                    return true;
                }

                int copies = 1;
                if (args.length > 0) {
                    try {
                            copies = Integer.parseInt(args[0]);

                            if (copies < 1 || copies > 9) {
                                copies = 1;
                            }
                    } catch (NumberFormatException ignore) {}
                }
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                Block block = player.getLocation().getBlock();

                if (itemInHand != null && !itemInHand.getType().isAir()) {
                    for (int i = 0; i < copies; i++) {
                        if (getItemCountInChunk(block) >= 60) {
                            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
                            return true;
                        }
                        ItemStack duplicatedItem = itemInHand.clone();
                        player.getWorld().dropItemNaturally(player.getLocation(), duplicatedItem);
                    }
                    sendPrefixedLocalizedMessage(player, "dupe_success");
                    lastDupeTimes.put(playerId, currentTime);
                } else {
                    sendPrefixedLocalizedMessage(player, "dupe_failed");
                }
            } else if (player.hasPermission(PERMISSION)) {

                if (currentTime - lastDupeTime < DUPE_COOLDOWN) {
                    sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
                    return true;
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                Block block = player.getLocation().getBlock();

                if (itemInHand != null && !itemInHand.getType().isAir()) {
                    if (getItemCountInChunk(block) >= 9) {
                        sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
                        return true;
                    }
                    ItemStack duplicatedItem = itemInHand.clone();
                    player.getWorld().dropItemNaturally(player.getLocation(), duplicatedItem);
                    sendPrefixedLocalizedMessage(player, "dupe_success");
                    lastDupeTimes.put(playerId, currentTime);
                } else {
                    sendPrefixedLocalizedMessage(player, "dupe_failed");
                }
            } else {
                sendPrefixedLocalizedMessage(player, "dupe_permission");
                player.setHealth(0);
            }
        } else {
            sendMessage(sender, "&3You must be a player to use this command.");
        }
        return true;
    }

    private int getItemCountInChunk(Block block) {
        return (int) Arrays.stream(block.getChunk().getEntities())
                .filter(entity -> entity instanceof Item)
                .map(entity -> (Item) entity)
                .count();
    }
}
