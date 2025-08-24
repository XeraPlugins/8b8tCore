package me.txmc.core.dupe.framedupe;

import me.txmc.core.util.GlobalUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Handles the duplication of items from ItemFrames when a player interacts with them.
 *
 * <p>This class is part of the 8b8tCore plugin, which adds custom functionalities
 * to Minecraft, including item duplication features.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Preventing item duplication if the cooldown interval has not passed</li>
 *     <li>Limiting the number of items in a chunk to prevent excessive item drops</li>
 *     <li>Controlling the probability of item duplication based on a configurable percentage</li>
 *     <li>Dropping items naturally in the world when conditions are met</li>
 * </ul>
 *
 *
 * <p>Configuration:</p>
 * <ul>
 *     <li><b>FrameDupe.enabled</b>: Enable or disable frame dupe.</li>
 *     <li><b>FrameDupe.dupeCooldown</b>: Time in milliseconds between allowed duplications.</li>
 *     <li><b>FrameDupe.limitItemsPerChunk</b>: Maximum number of items allowed in a chunk before
 *         duplication is prevented.</li>
 *     <li><b>FrameDupe.probabilityPercentage</b>: Probability percentage of item duplication occurring.</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/01 11:28 AM
 */
public class FrameDupe implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastDuplicationTimes = new HashMap<>(); // Map to track last duplication time for each item frame
    private final Map<UUID, Integer> itemCounts = new HashMap<>(); // Map to track item counts per chunk

    public FrameDupe(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFrameInteract(EntityDamageByEntityEvent event) {

        final boolean ENABLED = plugin.getConfig().getBoolean("FrameDupe.enabled", true);
        final long DUPLICATION_INTERVAL = plugin.getConfig().getLong("FrameDupe.dupeCooldown", 200L);
        final int MAX_ITEMS_IN_CHUNK = plugin.getConfig().getInt("FrameDupe.limitItemsPerChunk", 18);
        final int PROBABILITY_PERCENTAGE = plugin.getConfig().getInt("FrameDupe.probabilityPercentage", 100);

        if (!(event.getEntity() instanceof ItemFrame)) return;

        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) return;

        Player player = (Player) damager;

        if(!ENABLED){
            return;
        }

        final boolean VOTERS_ONLY = plugin.getConfig().getBoolean("FrameDupe.votersOnly", false);
        if (VOTERS_ONLY && !player.hasPermission("8b8tcore.dupe.frame")) return;

        int randomSuccess = (int)Math.round(Math.random() * 100);
        if (!(randomSuccess <= PROBABILITY_PERCENTAGE)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        UUID frameId = itemFrame.getUniqueId();
        Block block = itemFrame.getLocation().getBlock();
        UUID chunkId = GlobalUtils.getChunkId(block);


        if (System.currentTimeMillis() - lastDuplicationTimes.getOrDefault(chunkId /*frameId*/, 0L) < DUPLICATION_INTERVAL) {
            sendPrefixedLocalizedMessage(player, "framedupe_cooldown");
            return;
        }

        if (GlobalUtils.getItemCountInChunk(block) >= MAX_ITEMS_IN_CHUNK) {
            sendPrefixedLocalizedMessage(player, "framedupe_items_limit");
            return;
        }

        ItemStack itemStack = itemFrame.getItem();
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack);
        }

        lastDuplicationTimes.put(chunkId /*frameId*/, System.currentTimeMillis());
    }

}
