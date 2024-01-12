package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.TPAAcceptCommand;
import me.txmc.core.tpa.commands.TPACommand;
import me.txmc.core.tpa.commands.TPADenyCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * @author 254n_m
 * @since 2023/12/18 4:07 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPASection implements Section {
    private final Main plugin;
    private ConcurrentHashMap<Player, Player> lastRequest;
    private ConcurrentHashMap<Player, List<Player>> requests;
    private ConfigurationSection config;

    @Override
    public void enable() {
        lastRequest = new ConcurrentHashMap<>();
        requests = new ConcurrentHashMap<>();
        config = plugin.getSectionConfig(this);
        plugin.register(new LeaveListener(this));
        plugin.getCommand("tpa").setExecutor(new TPACommand(this));
        plugin.getCommand("tpayes").setExecutor(new TPAAcceptCommand(this));
        plugin.getCommand("tpano").setExecutor(new TPADenyCommand(this));
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

    public void registerRequest(Player from, Player to) {

        if (getLastRequest(to) != null) {
            sendPrefixedLocalizedMessage(from, "tpa_pending_request", to.getName());
            return;
        }
        lastRequest.put(to, from);
        if (requests.getOrDefault(to, null) == null) {
            requests.put(to, Collections.singletonList(from));
        } else {
            requests.get(to).add(from);
        }
        plugin.getExecutorService().schedule(() -> {
            sendPrefixedLocalizedMessage(to, "tpa_request_timeout");
            sendPrefixedLocalizedMessage(from, "tpa_request_timeout");
            lastRequest.remove(to);
        }, config.getInt("RequestTimeout"), TimeUnit.MINUTES);
    }

    public Player getLastRequest(Player requested) {
        return lastRequest.getOrDefault(requested, null);
    }

    public Player getRequest(Player requester, Player requested) {
        if (requests.getOrDefault(requested, null) == null) return null;
        if (!requests.get(requested).contains(requester)) return null;
        return requests.get(requested).get(requests.get(requested).indexOf(requester));
    }

    public void removeLastRequest(Player requested) {
        lastRequest.remove(requested);
        requests.computeIfAbsent(requested, k -> new ArrayList<>());

        if (!requests.get(requested).isEmpty()) {
            lastRequest.put(requested, getRequests(requested).get(0));
        }
    }

    public void removeRequest(Player requester, Player requested) {
        if (requests.getOrDefault(requested, null) == null) return;
        if (!requests.get(requested).contains(requester)) return;
        if (!requests.get(requested).isEmpty() && requests.get(requested).indexOf(requester) == 0) {
            removeLastRequest(requested);
        }
        requests.get(requested).remove(requester);
    }

    public List<Player> getRequests(Player to) {
        requests.computeIfAbsent(to, k -> new ArrayList<>());
        return requests.getOrDefault(to, null);
    }
}
