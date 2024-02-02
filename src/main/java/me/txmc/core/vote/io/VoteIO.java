package me.txmc.core.vote.io;

import com.google.gson.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/25 11:38 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class VoteIO {
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void save(HashMap<String, Integer> map) {
        try {
            JsonArray arr = new JsonArray();
            map.forEach((name, amount) -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", name);
                obj.addProperty("timesVoted", amount);
            });
            @Cleanup FileWriter fw = new FileWriter(file, false);
            gson.toJson(arr, fw);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to save future rewards map. Please see the stacktrace below for more info");
            t.printStackTrace();
        }
    }

    public HashMap<String, Integer> load() {
        HashMap<String, Integer> buf = new HashMap<>();
        try {
            @Cleanup FileReader reader = new FileReader(file);
            JsonArray obj = gson.fromJson(reader, JsonArray.class);
            for (JsonElement element : obj) {
                JsonObject record = element.getAsJsonObject();
                buf.put(record.get("name").getAsString(), record.get("timesVoted").getAsInt());
            }
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to load future rewards map. Please see the stacktrace below for more info");
            t.printStackTrace();
        }
        return buf;
    }
}
