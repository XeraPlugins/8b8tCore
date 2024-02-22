package me.txmc.core.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import org.bukkit.configuration.ConfigurationSection;

import static me.txmc.core.util.GlobalUtils.info;


@Getter
@RequiredArgsConstructor
public class CommandSection implements Section {
    private final Main plugin;
    private CommandHandler commandHandler;
    private ConfigurationSection config;

    @Override
    public void enable() {
        commandHandler = new CommandHandler(this);
        config = plugin.getSectionConfig(this);
        commandHandler.registerCommands();
    }

    @Override
    public void disable() {

    }

    @Override
    public String getName() {
        return "Commands";
    }

    @Override
    public void reloadConfig() {
        this.config = plugin.getSectionConfig(this);
    }

}
