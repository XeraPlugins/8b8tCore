package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.TPAAcceptCommand;
import me.txmc.core.tpa.commands.TPACommand;
import me.txmc.core.tpa.commands.TPADenyCommand;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author 254n_m
 * @since 2023/12/18 4:07 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TPASection implements Section {
    private final Main plugin;
    private ConcurrentHashMap<Player, Player> requestMap;
    private ConfigurationSection config;

    @Override
    public void enable() {
        requestMap = new ConcurrentHashMap<>();
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

    }

    @Override
    public String getName() {
        return "TPA";
    }

    public void registerRequest(Player from, Player to) {
        if (findTpRequester(to) != null) {
            GlobalUtils.sendPrefixedLocalizedMessage(from, "tpa_pending_request", to.getName());
            return;
        }
        requestMap.put(to, from);
        plugin.getExecutorService().schedule(() -> {
            GlobalUtils.sendPrefixedLocalizedMessage(to, "tpa_request_timeout");
            GlobalUtils.sendPrefixedLocalizedMessage(from, "tpa_request_timeout");
            requestMap.remove(to);
        }, config.getInt("RequestTimeout"), TimeUnit.MINUTES);
    }

    public Player findTpRequester(Player to) {
        return requestMap.getOrDefault(to, null);
    }

    public void removeRequest(Player to) {
        requestMap.remove(to);
    }
}
