package me.txmc.core.util.deathmessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import me.txmc.core.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PlayerManager {
    private final UUID uuid;

    private final String name;

    private EntityDamageEvent.DamageCause damageCause;

    @Getter
    private Entity lastEntityDamager;

    @Getter
    @Setter
    private Entity lastExplosiveEntity;

    @Setter
    @Getter
    private Projectile lastProjectileEntity;

    private Material climbing;

    @Getter
    @Setter
    private Location explosionCauser;

    private Location location;

    @Getter
    @Setter
    private Inventory cachedInventory;

    private BukkitTask lastEntityTask;

    private static final List<PlayerManager> players = new ArrayList<>();

    public PlayerManager(Player p) {
        this.uuid = p.getUniqueId();
        this.name = p.getName();
        this.damageCause = EntityDamageEvent.DamageCause.CUSTOM;
        players.add(this);
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(this.uuid);
    }

    public UUID getUUID() {
        return Objects.<UUID>requireNonNull(this.uuid);
    }

    public String getName() {
        return Objects.<String>requireNonNull(this.name);
    }

    public void setLastDamageCause(EntityDamageEvent.DamageCause dc) {
        this.damageCause = dc;
    }

    public EntityDamageEvent.DamageCause getLastDamage() {
        return this.damageCause;
    }

    public void setLastEntityDamager(Entity e) {
        setLastExplosiveEntity(null);
        setLastProjectileEntity(null);
        this.lastEntityDamager = e;
        if (e == null)
            return;
        if (this.lastEntityTask != null)
            this.lastEntityTask.cancel();
        this

                .lastEntityTask = (new BukkitRunnable() {
            public void run() {
                PlayerManager.this.setLastEntityDamager(null);
            }
        }).runTaskLater(Main.getInstance(), Main.getInstance().getConfig().getInt("Expire-Last-Damage.Expire-Player") * 20L);
    }

    public Material getLastClimbing() {
        return this.climbing;
    }

    public void setLastClimbing(Material climbing) {
        this.climbing = climbing;
    }

    public Location getLastLocation() {
        return getPlayer().getLocation();
    }

    public static PlayerManager getPlayer(Player p) {
        for (PlayerManager pm : players) {
            if (pm.getUUID().equals(p.getUniqueId()))
                return pm;
        }
        return null;
    }

    public static PlayerManager getPlayer(UUID uuid) {
        for (PlayerManager pm : players) {
            if (pm.getUUID().equals(uuid))
                return pm;
        }
        return null;
    }

    public void removePlayer() {
        players.remove(this);
    }
}
