package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.TPAAcceptCommand;
import me.txmc.core.tpa.commands.TPACommand;
import me.txmc.core.tpa.commands.TPADenyCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final HashMap<Player, Player> lastRequest = new HashMap<>();
    private static final HashMap<Player, List<Player>> requests = new HashMap<>();
    private ConfigurationSection config;

    @Override
    public void enable() {
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

    public void registerRequest(Player requester, Player requested) {
        lastRequest.put(requested, requester);
        if (requests.get(requested) == null) {
            requests.put(requested, new ArrayList<>());
            requests.get(requested).add(requester);
        } else {
            requests.get(requested).add(requester);
        }

        plugin.getExecutorService().schedule(() -> {
            if (!requests.get(requested).contains(requester)) return;
            sendPrefixedLocalizedMessage(requested, "tpa_request_timeout");
            sendPrefixedLocalizedMessage(requester, "tpa_request_timeout");
            lastRequest.remove(requested);
            requests.get(requested).remove(requester);
            if (!requests.get(requested).isEmpty()) lastRequest.put(requested, requests.get(requested).get(0));
        }, config.getInt("RequestTimeout"), TimeUnit.MINUTES);
    }

    public Player getLastRequest(Player requested) {
        return lastRequest.get(requested);
    }

    public boolean hasRequested(Player requester, Player requested) {
        if (requests.get(requested) == null) return false;
        if (!requests.get(requested).contains(requester)) return false;
        return requests.get(requested).contains(requester);
    }

    public void removeRequest(Player requester, Player requested) {
        if (requests.get(requested).indexOf(requester) == 0) {
            requests.get(requested).remove(0);
            if (requests.get(requested).size() > 1) lastRequest.put(requested, requests.get(requested).get(1));
            else lastRequest.remove(requested);
        } else requests.get(requested).remove(requester);;
    }

    public List<Player> getRequests(Player to) {
        requests.computeIfAbsent(to, k -> new ArrayList<>());
        return requests.get(to);
    }
}
