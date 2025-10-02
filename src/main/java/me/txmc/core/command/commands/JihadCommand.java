package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.txmc.core.util.GlobalUtils.sendMessage;

/**
 * Jihad command that gives players TNT and flint & steel with cooldown
 * Code ported from Tristans /jihad command
 * @author 8b8tCore
 * @since 10/2/2025 
 */
public class JihadCommand extends BaseCommand {
    
    private final Main plugin;
    private final Map<UUID, Long> cooldowns;
    private static final long COOLDOWN_TIME = 15000; // 15 seconds in milliseconds
    
    public JihadCommand(Main plugin) {
        super(
                "jihad",
                "/jihad",
                "8b8tcore.command.jihad",
                "Gives TNT and flint & steel for explosive fun"
        );
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cThis command can only be used by players!");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown
        if (cooldowns.containsKey(playerId)) {
            long lastUsed = cooldowns.get(playerId);
            long timeLeft = (lastUsed + COOLDOWN_TIME) - currentTime;
            
            if (timeLeft > 0) {
                long secondsLeft = timeLeft / 1000;
                sendMessage(player, "&cYou must wait &e" + secondsLeft + " &cseconds before using this command again!");
                return;
            }
        }
        
        // Create TNT stack
        ItemStack tnt = new ItemStack(Material.TNT, 64);
        ItemMeta tntMeta = tnt.getItemMeta();
        tntMeta.setDisplayName(ChatColor.RED + "WINST0N");
        tnt.setItemMeta(tntMeta);
        
        // Create flint and steel
        ItemStack lighter = new ItemStack(Material.FLINT_AND_STEEL, 1);
        ItemMeta lighterMeta = lighter.getItemMeta();
        lighterMeta.setDisplayName(ChatColor.GOLD + "John's ALLAHU AKBAR");
        lighterMeta.addEnchant(Enchantment.UNBREAKING, 3, false);
        lighter.setItemMeta(lighterMeta);
        
        // Give items to player
        player.getInventory().addItem(tnt, lighter);
        
        // Update cooldown
        cooldowns.put(playerId, currentTime);
        
        sendMessage(player, "&a&lALLAHU AKBAR! &r&7You have received explosive materials!");
        sendMessage(player, "&eUse &6/jihad &eagain in 15 seconds!");
    }
}