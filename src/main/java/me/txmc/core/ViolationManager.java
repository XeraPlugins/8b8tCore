package me.txmc.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author MindComplexity 
 * @since 2026/01/26
 * This file was created as a part of 8b8tCore
*/

public class ViolationManager {
    private final ConcurrentHashMap<UUID, Integer> map = new ConcurrentHashMap<>();
    private final int addAmount;
    private final int removeAmount;
    protected final Main plugin;

    public ViolationManager(int addAmount, Main main) {
        this(addAmount, addAmount, main);
    }

    public ViolationManager(int addAmount, int removeAmount, Main main) {
        this.addAmount = addAmount;
        this.removeAmount = removeAmount;
        this.plugin = main;
    }

    public void decrementAll() {
        for (UUID uuid : map.keySet()) {
            map.computeIfPresent(uuid, (key, val) -> {
                int newVal = val - removeAmount;
                return (newVal <= 0) ? null : newVal;
            });
        }
    }

    public void increment(UUID uuid) {
        map.compute(uuid, (key, val) -> (val == null) ? addAmount : val + addAmount);
    }

    public int getVLS(UUID uuid) {
        return map.getOrDefault(uuid, 0); 
    }

    public void remove(UUID uuid) {
        map.remove(uuid);
    }
}
