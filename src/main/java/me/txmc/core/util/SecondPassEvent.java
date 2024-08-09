package me.txmc.core.util;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import  me.txmc.core.Main;

import java.util.logging.Logger;

public class SecondPassEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Logger logger;
    private final Main plugin;

    public SecondPassEvent(Logger logger, Main main) {
        this.logger = logger;
        plugin = main;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Logger getLogger() {
        return logger;
    }

    public Main getPlugin() {
        return plugin;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}