package me.txmc.core.util.deathmessages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import me.txmc.core.Main;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class EntityManager {
    @Getter
    private Entity entity;

    @Getter
    private UUID entityUUID;

    private EntityDamageEvent.DamageCause damageCause;

    @Getter
    private PlayerManager lastPlayerDamager;

    @Getter
    @Setter
    private Entity lastExplosiveEntity;

    private Projectile lastPlayerProjectile;

    @Getter
    @Setter
    private Location lastLocation;

    private BukkitTask lastPlayerTask;

    private static final List<EntityManager> entities = new ArrayList<>();

    public EntityManager(Entity entity, UUID entityUUID) {
        this.entity = entity;
        this.entityUUID = entityUUID;
        entities.add(this);
    }

    public void setLastDamageCause(EntityDamageEvent.DamageCause dc) {
        this.damageCause = dc;
    }

    public EntityDamageEvent.DamageCause getLastDamage() {
        return this.damageCause;
    }

    public void setLastPlayerDamager(PlayerManager pm) {
        setLastExplosiveEntity(null);
        setLastProjectileEntity(null);
        this.lastPlayerDamager = pm;
        if (pm == null)
            return;
        if (this.lastPlayerTask != null)
            this.lastPlayerTask.cancel();
        this

                .lastPlayerTask = (new BukkitRunnable() {
            public void run() {
                EntityManager.this.destroy();
            }
        }).runTaskLater(Main.getInstance(), Main.getInstance().getConfig().getInt("Expire-Last-Damage.Expire-Entity") * 20L);
        this.damageCause = EntityDamageEvent.DamageCause.CUSTOM;
    }

    public void setLastProjectileEntity(Projectile projectile) {
        this.lastPlayerProjectile = projectile;
    }

    public Projectile getLastProjectileEntity() {
        return this.lastPlayerProjectile;
    }

    public static EntityManager getEntity(UUID uuid) {
        for (EntityManager em : entities) {
            if (em.getEntityUUID().equals(uuid))
                return em;
        }
        return null;
    }

    public void destroy() {
        entities.remove(this);
    }
}
