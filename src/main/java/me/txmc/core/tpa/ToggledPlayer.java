package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.TPASection;
import me.txmc.core.tpa.commands.*;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class ToggledPlayer{
    private Player player;
    private final TPASection main;
    private boolean toggledOff = false;


    public void toggle(){
        this.toggledOff = !this.toggledOff;
        main.togglePlayer(this.player);
        
    }

    public boolean isToggledOff(){
        return this.toggledOff;
    }
}

    
