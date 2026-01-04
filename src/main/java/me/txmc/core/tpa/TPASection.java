package me.txmc.core.tpa;

import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * This file is apart of 8b8tcore.
 * @author 254n_m
 * @author MindComplexity (aka Libalpm)
 * @since 2023/12/18
 */
public class TPASection implements Section {
    public final Main plugin;
    
    private final ConcurrentHashMap<UUID, UUID> lastRequest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<UUID>> requests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<UUID>> hereRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> toggledPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<UUID>> blockedPlayers = new ConcurrentHashMap<>();
    
    private volatile ConfigurationSection config;
    private volatile int requestTimeoutMinutes = 5;

    public TPASection(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        requestTimeoutMinutes = config.getInt("RequestTimeoutInMinutes", 5);
        plugin.register(new LeaveListener(this));
        plugin.getCommand("tpa").setExecutor(new TPACommands.TPACommand(this));
        plugin.getCommand("tpahere").setExecutor(new TPACommands.TPAHereCommand(this));
        plugin.getCommand("tpayes").setExecutor(new TPACommands.TPAAcceptCommand(this));
        plugin.getCommand("tpano").setExecutor(new TPACommands.TPADenyCommand(this));
        plugin.getCommand("tpacancel").setExecutor(new TPACommands.TPACancelCommand(this));
        plugin.getCommand("tpatoggle").setExecutor(new TPACommands.TPAToggleCommand(this));
    }

    @Override
    public void disable() {
        lastRequest.clear();
        requests.clear();
        hereRequests.clear();
        toggledPlayers.clear();
        blockedPlayers.clear();
    }

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
        requestTimeoutMinutes = config.getInt("RequestTimeoutInMinutes", 5);
    }

    @Override
    public String getName() {
        return "TPA";
    }

    public void registerRequest(Player requester, Player requested) {
        UUID requesterUUID = requester.getUniqueId();
        UUID requestedUUID = requested.getUniqueId();
        
        lastRequest.put(requestedUUID, requesterUUID);
        requests.computeIfAbsent(requestedUUID, k -> new CopyOnWriteArrayList<>()).addIfAbsent(requesterUUID);

        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            CopyOnWriteArrayList<UUID> requestList = requests.get(requestedUUID);
            if (requestList == null || !requestList.contains(requesterUUID)) return;
            
            sendTimeoutMessage(requesterUUID, requestedUUID);
            
            requestList.remove(requesterUUID);
            if (requestList.isEmpty()) {
                requests.remove(requestedUUID);
                lastRequest.remove(requestedUUID);
            } else {
                lastRequest.put(requestedUUID, requestList.get(0));
            }
        }, requestTimeoutMinutes, TimeUnit.MINUTES);
    }

    public void registerHereRequest(Player requester, Player requested) {
        UUID requesterUUID = requester.getUniqueId();
        UUID requestedUUID = requested.getUniqueId();
        
        lastRequest.put(requestedUUID, requesterUUID);
        hereRequests.computeIfAbsent(requestedUUID, k -> new CopyOnWriteArrayList<>()).addIfAbsent(requesterUUID);

        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
            CopyOnWriteArrayList<UUID> requestList = hereRequests.get(requestedUUID);
            if (requestList == null || !requestList.contains(requesterUUID)) return;
            
            sendTimeoutMessage(requesterUUID, requestedUUID);
            
            requestList.remove(requesterUUID);
            if (requestList.isEmpty()) {
                hereRequests.remove(requestedUUID);
                lastRequest.remove(requestedUUID);
            } else {
                lastRequest.put(requestedUUID, requestList.get(0));
            }
        }, requestTimeoutMinutes, TimeUnit.MINUTES);
    }

    private void sendTimeoutMessage(UUID requesterUUID, UUID requestedUUID) {
        Player requester = Bukkit.getPlayer(requesterUUID);
        Player requested = Bukkit.getPlayer(requestedUUID);
        
        if (requested != null && requested.isOnline()) {
            requested.getScheduler().run(plugin, t -> {
                Player req = Bukkit.getPlayer(requesterUUID);
                if (req != null) {
                    sendPrefixedLocalizedMessage(requested, "tpa_request_timeout_to", req.getName());
                }
            }, null);
        }
        
        if (requester != null && requester.isOnline()) {
            requester.getScheduler().run(plugin, t -> {
                Player reqd = Bukkit.getPlayer(requestedUUID);
                if (reqd != null) {
                    sendPrefixedLocalizedMessage(requester, "tpa_request_timeout_from", reqd.getName());
                }
            }, null);
        }
    }

    public Player getLastRequest(Player requested) {
        UUID uuid = lastRequest.get(requested.getUniqueId());
        return uuid != null ? Bukkit.getPlayer(uuid) : null;
    }

    public boolean hasRequested(Player requester, Player requested) {
        CopyOnWriteArrayList<UUID> list = requests.get(requested.getUniqueId());
        return list != null && list.contains(requester.getUniqueId());
    }

    public boolean hasHereRequested(Player requester, Player requested) {
        CopyOnWriteArrayList<UUID> list = hereRequests.get(requested.getUniqueId());
        return list != null && list.contains(requester.getUniqueId());
    }

    public void removeRequest(Player requester, Player requested) {
        removeFromMap(requests, requester.getUniqueId(), requested.getUniqueId());
    }

    public void removeHereRequest(Player requester, Player requested) {
        removeFromMap(hereRequests, requester.getUniqueId(), requested.getUniqueId());
    }

    private void removeFromMap(ConcurrentHashMap<UUID, CopyOnWriteArrayList<UUID>> map, 
                               UUID requesterUUID, UUID requestedUUID) {
        CopyOnWriteArrayList<UUID> list = map.get(requestedUUID);
        if (list == null) return;
        
        boolean wasFirst = !list.isEmpty() && list.get(0).equals(requesterUUID);
        list.remove(requesterUUID);
        
        if (list.isEmpty()) {
            map.remove(requestedUUID);
            lastRequest.remove(requestedUUID);
        } else if (wasFirst) {
            lastRequest.put(requestedUUID, list.get(0));
        }
    }

    public void removeAllInRequests(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        notifyAndRemove(requests, playerUUID, "tpa_request_denied_from", player.getName());
        notifyAndRemove(hereRequests, playerUUID, "tpa_request_denied_from", player.getName());
        lastRequest.remove(playerUUID);
    }

    private void notifyAndRemove(ConcurrentHashMap<UUID, CopyOnWriteArrayList<UUID>> map, 
                                  UUID playerUUID, String messageKey, String playerName) {
        CopyOnWriteArrayList<UUID> requesters = map.remove(playerUUID);
        if (requesters == null) return;
        
        for (UUID requesterUUID : requesters) {
            Player requester = Bukkit.getPlayer(requesterUUID);
            if (requester != null && requester.isOnline()) {
                requester.getScheduler().run(plugin, t -> 
                    sendPrefixedLocalizedMessage(requester, messageKey, playerName), null);
            }
        }
    }

    public List<Player> getRequests(Player to) {
        return uuidsToPlayers(requests.getOrDefault(to.getUniqueId(), new CopyOnWriteArrayList<>()));
    }

    public List<Player> getHereRequests(Player to) {
        return uuidsToPlayers(hereRequests.getOrDefault(to.getUniqueId(), new CopyOnWriteArrayList<>()));
    }

    private List<Player> uuidsToPlayers(List<UUID> uuids) {
        List<Player> players = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }
        return players;
    }

    public void togglePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (toggledPlayers.remove(uuid) == null) {
            toggledPlayers.put(uuid, Boolean.TRUE);
        }
        removeAllInRequests(player);
    }

    public boolean checkToggle(Player player) {
        return toggledPlayers.containsKey(player.getUniqueId());
    }

    public void addBlockedPlayer(Player requested, Player requester) {
        blockedPlayers.computeIfAbsent(requested.getUniqueId(), k -> new CopyOnWriteArrayList<>())
                      .addIfAbsent(requester.getUniqueId());
        removeRequest(requester, requested);
        removeHereRequest(requester, requested);
    }

    public void removeBlockedPlayer(Player requested, Player requester) {
        CopyOnWriteArrayList<UUID> list = blockedPlayers.get(requested.getUniqueId());
        if (list != null) {
            list.remove(requester.getUniqueId());
        }
    }

    public boolean checkBlocked(Player requested, Player requester) {
        CopyOnWriteArrayList<UUID> list = blockedPlayers.get(requested.getUniqueId());
        return list != null && list.contains(requester.getUniqueId());
    }

    public void cleanupPlayer(UUID uuid) {
        lastRequest.remove(uuid);
        requests.remove(uuid);
        hereRequests.remove(uuid);
        toggledPlayers.remove(uuid);
        blockedPlayers.remove(uuid);        
        requests.values().forEach(list -> list.remove(uuid));
        hereRequests.values().forEach(list -> list.remove(uuid));
        blockedPlayers.values().forEach(list -> list.remove(uuid));
    }
}