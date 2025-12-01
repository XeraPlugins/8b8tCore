package package me.txmc.core.patch;

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

public class Main extends JavaPlugin implements Listener {

    private boolean featureEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        featureEnabled = cfg.getBoolean("ForceKillNetherRoof", true);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (!featureEnabled) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;

        if (block.getType() != Material.BEDROCK) return;

        BlockFace face = event.getBlockFace();
        if (face != BlockFace.UP) return;

        player.setHealth(0.0);
    }
}

// Last edited by MrBeax31 (https://github.com/MrBeax31)
