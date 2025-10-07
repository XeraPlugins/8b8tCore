package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
//import me.txmc.core.tpa.ToggledPlayer;
import me.txmc.core.tpa.commands.TPAAcceptCommand;
import me.txmc.core.tpa.commands.TPACancelCommand;
import me.txmc.core.tpa.commands.TPACommand;
import me.txmc.core.tpa.commands.TPADenyCommand;
import me.txmc.core.tpa.commands.TPAHereCommand;
import me.txmc.core.tpa.commands.TPAToggleCommand;
import me.txmc.core.tpa.commands.TPAIgnoreCommand;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.Listener;

//import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//import static me.txmc.core.util.GlobalUtils.info;

import javax.management.RuntimeErrorException;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/18 4:07 PM
 * Edited by 5aks
 * This file was created as a part of 8b8tCore
 */



@RequiredArgsConstructor
public class TPASection implements Section {
    public final Main plugin;
    private static final ConcurrentHashMap<Player, Player> lastRequest = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Player, List<Player>> requests = new ConcurrentHashMap<>();
    private ConfigurationSection config;
    private static final ConcurrentHashMap<Player, List<Player>> hereRequests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Player, ToggledPlayer> toggledPlayers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Player, List<Player>> blockedPlayers = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        config = plugin.getSectionConfig(this);
        plugin.register(new LeaveListener(this));
        plugin.getCommand("tpa").setExecutor(new TPACommand(this));
        plugin.getCommand("tpahere").setExecutor(new TPAHereCommand(this));
        plugin.getCommand("tpayes").setExecutor(new TPAAcceptCommand(this));
        plugin.getCommand("tpano").setExecutor(new TPADenyCommand(this));
        plugin.getCommand("tpacancel").setExecutor(new TPACancelCommand(this));
        plugin.getCommand("tpatoggle").setExecutor(new TPAToggleCommand(this));
        plugin.getCommand("tpaignore").setExecutor(new TPAIgnoreCommand(this));
    }

    @Override
    public void disable() {

    }

    @Override
    public void reloadConfig() {
        config = plugin.getSectionConfig(this);
    }

    @Override
    public String getName() {
        return "TPA";
    }

    public void registerHereRequest(Player requester, Player requested) {
        synchronized (hereRequests) {
            lastRequest.put(requested, requester);
            hereRequests.computeIfAbsent(requested, k -> new ArrayList<>()).add(requester);
        }

        plugin.getExecutorService().schedule(() -> {
            synchronized (hereRequests) {
                List<Player> requests = hereRequests.get(requested);
                if (requests == null || !requests.contains(requester)) return;
                
                sendPrefixedLocalizedMessage(requested, "tpa_request_timeout_to", requester.getName());
                sendPrefixedLocalizedMessage(requester, "tpa_request_timeout_from", requested.getName());
                
                requests.remove(requester);
                if (requests.isEmpty()) {
                    hereRequests.remove(requested);
                    lastRequest.remove(requested);
                } else {
                    lastRequest.put(requested, requests.get(0));
                }
            }
        }, config.getInt("RequestTimeoutInMinutes", 5), TimeUnit.MINUTES);
    }

    public boolean hasHereRequested(Player requester, Player requested) {
        if (hereRequests.get(requested) == null) return false;
        return hereRequests.get(requested).contains(requester);
    }

    public void removeHereRequest(Player requester, Player requested) {
        synchronized (hereRequests) {
            List<Player> requests = hereRequests.get(requested);
            if (requests == null) return;
            
            if (requests.indexOf(requester) == 0) {
                requests.remove(0);
                if (requests.size() > 1) {
                    lastRequest.put(requested, requests.get(1));
                } else {
                    lastRequest.remove(requested);
                    hereRequests.remove(requested);
                }
            } else {
                requests.remove(requester);
                if (requests.isEmpty()) {
                    hereRequests.remove(requested);
                    lastRequest.remove(requested);
                }
            }
        }
    }

    public void registerRequest(Player requester, Player requested) {
        synchronized (requests) {
            lastRequest.put(requested, requester);
            requests.computeIfAbsent(requested, k -> new ArrayList<>()).add(requester);
        }

        plugin.getExecutorService().schedule(() -> {
            synchronized (requests) {
                List<Player> requestList = requests.get(requested);
                if (requestList == null || !requestList.contains(requester)) return;
                
                sendPrefixedLocalizedMessage(requested, "tpa_request_timeout_to", requester.getName());
                sendPrefixedLocalizedMessage(requester, "tpa_request_timeout_from", requested.getName());
                
                requestList.remove(requester);
                if (requestList.isEmpty()) {
                    requests.remove(requested);
                    lastRequest.remove(requested);
                } else {
                    lastRequest.put(requested, requestList.get(0));
                }
            }
        }, config.getInt("RequestTimeoutInMinutes", 5), TimeUnit.MINUTES);
    }

    public Player getLastRequest(Player requested) {
        return lastRequest.get(requested);
    }

    public boolean hasRequested(Player requester, Player requested) {
        if (requests.get(requested) == null) return false;
        return requests.get(requested).contains(requester);
    }

    public void removeRequest(Player requester, Player requested) {
        synchronized (requests) {
            List<Player> requestList = requests.get(requested);
            if (requestList == null) return;
            
            if (requestList.indexOf(requester) == 0) {
                requestList.remove(0);
                if (requestList.size() > 1) {
                    lastRequest.put(requested, requestList.get(1));
                } else {
                    lastRequest.remove(requested);
                    requests.remove(requested);
                }
            } else {
                requestList.remove(requester);
                if (requestList.isEmpty()) {
                    requests.remove(requested);
                    lastRequest.remove(requested);
                }
            }
        }
    }

    public void removeAllInRequests(Player player){
        synchronized (requests) {
            List<Player> requesters = requests.get(player);
            if (requesters != null) {
                requesters.forEach(r -> sendPrefixedLocalizedMessage(r, "tpa_request_denied_from", player.getName()));
                requests.remove(player);
            }
        }
        synchronized (hereRequests) {
            List<Player> requesters = hereRequests.get(player);
            if (requesters != null) {
                requesters.forEach(r -> sendPrefixedLocalizedMessage(r, "tpa_request_denied_from", player.getName()));
                hereRequests.remove(player);
            }
        }
    }

    public void removeInRequestsFrom(Player requested, Player requester){
        synchronized (requests) {
            if(requests.containsKey(requested)){
                List<Player> requesters = requests.get(requested);
                if(requesters.contains(requester)){
                    sendPrefixedLocalizedMessage(requester, "tpa_request_denied_from", requested.getName());
                    requesters.remove(requester);
                    if (requesters.isEmpty()) {
                        requests.remove(requested);
                        lastRequest.remove(requested);
                    } else if (lastRequest.get(requested) == requester) {
                        lastRequest.put(requested, requesters.get(0));
                    }
                }
            }
        }
        synchronized (hereRequests) {
            if(hereRequests.containsKey(requested)){
                List<Player> requesters = hereRequests.get(requested);
                if(requesters.contains(requester)){
                    sendPrefixedLocalizedMessage(requester, "tpa_request_denied_from", requested.getName());
                    requesters.remove(requester);
                    if (requesters.isEmpty()) {
                        hereRequests.remove(requested);
                        lastRequest.remove(requested);
                    } else if (lastRequest.get(requested) == requester) {
                        lastRequest.put(requested, requesters.get(0));
                    }
                }
            }
        }
    }

    public List<Player> getRequests(Player to) {
        requests.computeIfAbsent(to, k -> new ArrayList<>());
        return requests.get(to);
    }

    public List<Player> getHereRequests(Player to) {
        hereRequests.computeIfAbsent(to, k -> new ArrayList<>());
        return hereRequests.get(to);
    }

    public void togglePlayer(Player requested){
        synchronized (toggledPlayers) {
            if(toggledPlayers.get(requested) == null){
                ToggledPlayer toggledPlayer = new ToggledPlayer(requested, this);
                toggledPlayers.put(requested, toggledPlayer);
            }else toggledPlayers.remove(requested);
        }
        removeAllInRequests(requested);
    }

    public boolean checkToggle(Player cPlayer){
        if(toggledPlayers.get(cPlayer) == null){
            return false;
        }else return true;
    }

    public void addBlockedPlayer(Player requested, Player requester){
        synchronized (blockedPlayers) {
            if(blockedPlayers.get(requested) == null){
                blockedPlayers.put(requested, new ArrayList<>());
            }
            if(!blockedPlayers.get(requested).contains(requester)) blockedPlayers.get(requested).add(requester);
        }
        removeInRequestsFrom(requested, requester);
    }

    public void removeBlockedPlayer(Player requested, Player requester){
        synchronized (blockedPlayers) {
            if(blockedPlayers.get(requested) != null && blockedPlayers.get(requested).contains(requester)) {
                blockedPlayers.get(requested).remove(requester);
            }
        }
    }

    public boolean checkBlocked(Player requested, Player requester){
        synchronized (blockedPlayers) {
            if(blockedPlayers.get(requested) == null) return false;
            return blockedPlayers.get(requested).contains(requester);
        }
    }
}