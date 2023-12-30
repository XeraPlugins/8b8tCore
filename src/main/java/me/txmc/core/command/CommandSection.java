package me.txmc.core.command;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.command.commands.BaseCommand;
import me.txmc.core.command.commands.HelpCommand;

/**
 * @author 254n_m
 * @since 2023/12/20 6:08 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class CommandSection implements Section {
    private final Main plugin;
    @Override
    public void enable() {
        plugin.getCommand("help").setExecutor(new HelpCommand());
        plugin.getCommand("lef").setExecutor(new BaseCommand(plugin));
    }

    @Override
    public void disable() {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public String getName() {
        return "Commands";
    }
}
