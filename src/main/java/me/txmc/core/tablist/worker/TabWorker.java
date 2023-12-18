package me.txmc.core.tablist.worker;

import lombok.RequiredArgsConstructor;
import me.txmc.core.tablist.TabSection;
import org.bukkit.Bukkit;

/**
 * @author 254n_m
 * @since 2023/12/17 11:58 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class TabWorker implements Runnable {
    private final TabSection main;
    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(main::setTab);
    }
}
