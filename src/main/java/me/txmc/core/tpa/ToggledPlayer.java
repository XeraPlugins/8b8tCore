package me.txmc.core.tpa;

import lombok.RequiredArgsConstructor;
import me.txmc.core.Main;
import me.txmc.core.Section;
import me.txmc.core.tpa.commands.*;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

@RequiredArgsConstructor
public class ToggledPlayer extends Player{
    private boolean toggledOff = False;

    public void toggle(ToggledPlayer player){
        if(toggledOff){
            player.toggledOff = True;
        }else player.toggledOff = False;
    }
    public boolean isToggledOff(ToggledPlayer player){
        return toggledOff;
    }
}