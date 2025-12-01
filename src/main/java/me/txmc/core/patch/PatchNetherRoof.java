package me.txmc.core.patch;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.block.BlockFace;

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
    public void onBlockDamage(BlockDamageEvent event) {
        if (!featureEnabled) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;
        if (block.getType() != Material.BEDROCK) return;

        if (event.getBlockFace() != BlockFace.UP) return;

        player.setHealth(0.0);
    }
}



// If you want to enable/disable this, you may do it at almost the end of the file changing true for false.
// Last edited by MrBeax31 (https://github.com/MrBeax31)
