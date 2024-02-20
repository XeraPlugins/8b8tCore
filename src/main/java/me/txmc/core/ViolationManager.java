package me.txmc.core;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 254n_m
 * @since 2024/01/26 4:28 PM
 * This file was created as a part of 8b8tCore
 */
public class ViolationManager {
    private final ConcurrentHashMap<Object, Integer> map;
    private final int addAmount;
    private final int removeAmount;
    protected final Main plugin;

    public ViolationManager(int addAmount, Main main) {
        this(addAmount, addAmount, main);
    }

    public ViolationManager(int addAmount, int removeAmount, Main main) {
        this.addAmount = addAmount;
        this.removeAmount = removeAmount;
        map = new ConcurrentHashMap<>();
        plugin = main;
        main.register(this);
    }

    public void decrementAll() {
        map.forEach((key, val) -> {
            if (val <= removeAmount) {
                map.remove(key);
                return;
            }
            map.replace(key, val - removeAmount);
        });
    }

    public void increment(Object obj) {
        map.putIfAbsent(obj, 0);
        map.computeIfPresent(obj, (o, vls) -> vls + addAmount);
    }

    public int getVLS(Object obj) {
        return map.getOrDefault(obj, -1);
    }

    public void remove(Object obj) {
        map.remove(obj);
    }
}
