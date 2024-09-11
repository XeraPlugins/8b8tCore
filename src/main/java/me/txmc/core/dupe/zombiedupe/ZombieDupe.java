package me.txmc.core.dupe.zombiedupe;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.*;

/**
 * Handles the duplication of items held by zombies when shot with an arrow by an elytra-flying player.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft Folia Server, including item duplication features.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *     <li><b>ZombieDupe.enabled</b>: Enable or disable the zombie dupe.</li>
 *     <li><b>ZombieDupe.dupeCooldown</b>: Time in milliseconds between allowed duplications.</li>
 *     <li><b>ZombieDupe.limitItemsPerChunk</b>: Maximum number of items allowed in a chunk before
 *         duplication is prevented.</li>
 *     <li><b>ZombieDupe.probabilityPercentage</b>: Probability percentage of item duplication occurring.</li>
 *     <li><b>ZombieDupe.rottenFleshDropPercentage</b>: Probability percentage of dropping rotten flesh.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/24 02:14 AM
 */
public class ZombieDupe implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> lastDuplicationTimes = new HashMap<>();

    public ZombieDupe(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityHitByArrow(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Arrow arrow) {

            if (event.getEntity() instanceof Zombie zombie) {

                if (arrow.getShooter() instanceof Player player) {

                    final boolean ENABLED = plugin.getConfig().getBoolean("ZombieDupe.enabled", true);
                    if (!ENABLED) return;

                    if (player.isGliding()) {

                        zombie.damage(0, player);
                        event.setDamage(0);
                        arrow.remove();

                        final int PROBABILITY_PERCENTAGE = plugin.getConfig().getInt("ZombieDupe.probabilityPercentage", 100);
                        int randomSuccess = random.nextInt(100);
                        if (randomSuccess >= PROBABILITY_PERCENTAGE) return;

                        Block block = zombie.getLocation().getBlock();
                        UUID chunkId = getChunkUUID(block);
                        final long DUPLICATION_INTERVAL = plugin.getConfig().getLong("ZombieDupe.dupeCooldown", 200L);

                        if (System.currentTimeMillis() - lastDuplicationTimes.getOrDefault(chunkId, 0L) < DUPLICATION_INTERVAL) {
                            sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
                            return;
                        }

                        final int MAX_ITEMS_IN_CHUNK = plugin.getConfig().getInt("ZombieDupe.limitItemsPerChunk", 18);
                        if (getItemCountInChunk(block) >= MAX_ITEMS_IN_CHUNK) {
                            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
                            return;
                        }

                        ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
                        if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                            ItemStack duplicateItem = itemInHand.clone();
                            zombie.getWorld().dropItemNaturally(zombie.getLocation(), duplicateItem);

                            final int ROTTEN_FLESH_DROP_PERCENTAGE = plugin.getConfig().getInt("ZombieDupe.rottenFleshDropPercentage", 50);
                            if (random.nextInt(100) < ROTTEN_FLESH_DROP_PERCENTAGE) {
                                ItemStack rottenFlesh = new ItemStack(Material.ROTTEN_FLESH, 1);
                                zombie.getWorld().dropItemNaturally(zombie.getLocation(), rottenFlesh);
                            }

                            lastDuplicationTimes.put(chunkId, System.currentTimeMillis());
                        }
                    }
                }
            }
        }
    }
}
