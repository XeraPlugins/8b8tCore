package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.*;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

//@RequiredArgsConstructor
public class ToggledPlayer{
    private final Player player;
    private boolean toggledOff = false;

    public ToggledPlayer(Player player){
        this.player = player;
    }

    public void toggle(){
        if(toggledOff){
            this.toggledOff = false;
        }else this.toggledOff = true;
    }

    public boolean isToggledOff(){
        return this.toggledOff;
    }
}