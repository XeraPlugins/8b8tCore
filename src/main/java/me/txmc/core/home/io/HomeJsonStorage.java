package me.txmc.core.home.io;

import com.google.gson.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import me.txmc.core.IStorage;
import me.txmc.core.home.Home;
import me.txmc.core.home.HomeData;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/19 8:25 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class HomeJsonStorage implements IStorage<HomeData, Player> {
    private final File dataDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void save(HomeData homeData, Player player) {
        File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));

        if (homeData.getHomes().isEmpty()) {
            if (file.exists()) delete(player);
            return;
        }

        try {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            homeData.getHomes().forEach(h -> {
                JsonObject homeObj = new JsonObject();
                homeObj.addProperty("name", h.getName());
                homeObj.addProperty("world", h.getWorldName());
                homeObj.addProperty("yaw", h.getLocation().getYaw());
                homeObj.addProperty("pitch", h.getLocation().getPitch());
                homeObj.addProperty("x", h.getLocation().x());
                homeObj.addProperty("y", h.getLocation().y());
                homeObj.addProperty("z", h.getLocation().z());
                arr.add(homeObj);
            });
            obj.add("homes", arr);

            @Cleanup FileWriter fw = new FileWriter(file, false);
            gson.toJson(obj, fw);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to save Homes for&r&a %s&r&c. Please see the stacktrace below for more info", player.getUniqueId());
            t.printStackTrace();
        }
    }

    @Override
    public HomeData load(Player player) {
        try {
            File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));
            ArrayList<Home> buf = new ArrayList<>();
            if (file.exists()) {
                @Cleanup FileReader reader = new FileReader(file);
                JsonObject obj = gson.fromJson(reader, JsonObject.class);
                JsonArray homesArr = obj.get("homes").getAsJsonArray();
                for (JsonElement element : homesArr) {
                    JsonObject homeObj = element.getAsJsonObject();
                    String name = homeObj.get("name").getAsString();
                    String worldName = homeObj.get("world").getAsString();
                    Location location = new Location(
                            Bukkit.getWorld(worldName),
                            homeObj.get("x").getAsDouble(),
                            homeObj.get("y").getAsDouble(),
                            homeObj.get("z").getAsDouble(),
                            homeObj.get("yaw").getAsFloat(),
                            homeObj.get("pitch").getAsFloat()
                    );
                    buf.add(new Home(name, worldName, location));
                }
            }
            return new HomeData(buf);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "&cFailed to parse&r&a %s&r&c. This is most likely due to malformed json", player.getUniqueId());
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Player player) {
        File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));
        file.delete();
        GlobalUtils.log(Level.INFO, "&3Deleted homes file for&r&a %s&r", player.getName());
    }
}
