package me.txmc.core.patch;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

public class PatchNetherRoof extends JavaPlugin implements Listener {

    private boolean featureEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        featureEnabled = cfg.getBoolean("ForceKillNetherRoof.enabled", true);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!featureEnabled) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;

        Block blockUnder = player.getLocation().subtract(0, 1, 0).getBlock();
        if (blockUnder.getType() == Material.BEDROCK) {
            player.setHealth(0.0);
        }
    }
}

// If you want to enable/disable this, you may do it at almost the end of the file changing true for false.
// THIS MIGHT NOT WORK AND NEED TO DO ANOTHER FIX (i'll handle that fix)
// Last edited by MrBeax31  (https://github.com/MrBeax31)
