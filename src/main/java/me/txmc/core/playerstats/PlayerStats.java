package me.txmc.core.playerstats;

import static java.lang.System.currentTimeMillis;
import java.sql.Timestamp;

import java.util.Date;

import org.bukkit.entity.Player;
/**
 *
 * @author 5aks
 * @since 7/19/2025 5:30 PM This file was created as a part of 8b8tCore
 *
 */
public class PlayerStats {

    private Player player;
    private String name;
    private long joindate;
    private long seen;
    private long playtime;
    private long now = currentTimeMillis();

    public PlayerStats(Player player) {
        this.player = player;
        this.name = player.getName();        
    }

    public PlayerStats(String name, long joindate, long seen, long playtime) {
        this.name = name;
        this.joindate = joindate;
        this.seen = seen;
        this.playtime = playtime;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeen(long seen) {
        this.seen = seen;
    }

    public void setPlaytime(long playtime) {
        this.playtime = playtime;;
    }

    public Player getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public long getJoindate() {
        return joindate;
    }

    public long getSeen() {
        return seen;
    }

    public long getPlaytime() {
        return playtime;
    }
}
