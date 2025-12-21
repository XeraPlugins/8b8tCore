package me.txmc.core.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Home {
    private final String name;
    private final String worldName;
    private Location location;
}
