package me.txmc.core.patch.tasks;

import me.txmc.core.patch.listeners.ChestLagFix;

public class ChestLagFixTask implements Runnable {

    @Override
    public void run() {
        ChestLagFix.chestHashMap.clear();
    }
}
