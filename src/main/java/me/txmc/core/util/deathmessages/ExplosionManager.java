package me.txmc.core.util.deathmessages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import me.txmc.core.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class ExplosionManager {
    private final UUID pyro;

    private final Material material;

    @Setter
    private Location location;

    private final List<UUID> effected;

    public static List<ExplosionManager> explosions = new ArrayList<>();

    public ExplosionManager(UUID pyro, Material material, Location location, List<UUID> effected) {
        this.pyro = pyro;
        this.material = material;
        this.location = location;
        this.effected = effected;
        explosions.add(this);
        (new BukkitRunnable() {
            public void run() {
                ExplosionManager.this.destroy();
            }
        }).runTaskLater(Main.getInstance(), 100L);
    }

    public static ExplosionManager getExplosion(Location location) {
        for (ExplosionManager ex : explosions) {
            if (ex.getLocation().equals(location))
                return ex;
        }
        return null;
    }

    public static ExplosionManager getManagerIfEffected(UUID uuid) {
        for (ExplosionManager ex : explosions) {
            if (ex.getEffected().contains(uuid))
                return ex;
        }
        return null;
    }

    private void destroy() {
        explosions.remove(this);
    }
}
