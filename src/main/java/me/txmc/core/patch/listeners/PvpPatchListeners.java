package me.txmc.core.patch.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/**
 * Handles player suffocation damage based on the block they are suffocating in.
 *
 * <p>This class is part of the 8b8tCore plugin, which provides custom functionalities
 * including specific damage adjustments for certain blocks.</p>
 *
 * <p>Functionality includes:</p>
 * <ul>
 *     <li>Adjusting the damage dealt to a player when they suffocate in specific types of blocks</li>
 * </ul>
 *
 * <p>Damage Adjustment:</p>
 * <ul>
 *     <li>If the player is suffocating and the block causing the suffocation is one of the following types:
 *         <ul>
 *             <li>Obsidian</li>
 *             <li>Bedrock</li>
 *             <li>Netherite Block</li>
 *             <li>Barrier</li>
 *             <li>End Portal Frame</li>
 *         </ul>
 *     The damage dealt to the player is set to 7.0 points</li>
 * </ul>
 *
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/18 17:04 PM
 */
public class PvpPatchListeners implements Listener {

    @EventHandler
    public void onPlayerSuffocation(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (event.getCause() == DamageCause.SUFFOCATION) {

                Block block = player.getLocation().getBlock();
                Material blockType = block.getType();

                if (blockType == Material.OBSIDIAN
                        || blockType == Material.BEDROCK
                        || blockType == Material.NETHERITE_BLOCK
                        || blockType == Material.BARRIER
                        || blockType == Material.END_PORTAL_FRAME
                        || blockType == Material.ANVIL
                        || blockType == Material.CHIPPED_ANVIL
                        || blockType == Material.DAMAGED_ANVIL
                        || blockType == Material.REINFORCED_DEEPSLATE
                ) {
                    event.setDamage(7.0);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        Block block = player.getLocation().getBlock();
        Material blockType = block.getType();

        if (blockType == Material.OBSIDIAN
                || blockType == Material.BEDROCK
                || blockType == Material.NETHERITE_BLOCK
                || blockType == Material.BARRIER
                || blockType == Material.END_PORTAL_FRAME
                || blockType == Material.ANVIL
                || blockType == Material.CHIPPED_ANVIL
                || blockType == Material.DAMAGED_ANVIL
                || blockType == Material.REINFORCED_DEEPSLATE
        ) {
            player.damage(7.0);

            Location currentLocation = player.getLocation();
            Location newLocation = currentLocation.clone().add(new Vector(0, 1, 0));
            player.teleportAsync(newLocation);
        }

    }
}