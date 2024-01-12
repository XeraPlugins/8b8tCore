package me.txmc.core.chat.io;

import com.google.gson.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import me.txmc.core.IStorage;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/03/04 1:41 AM
 * This file was created as a part of L2X9RebootCore
 */
@RequiredArgsConstructor
public class ChatFileIO implements IStorage<ChatInfo, Player> {
    private final File dataDir;
    private final ChatSection cm;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void save(ChatInfo chatInfo, Player player) {
        File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));

        if (chatInfo.shouldNotSave()) {
            if (file.exists()) delete(player);
            return;
        }

        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("togglechat", chatInfo.isToggledChat());
            obj.addProperty("togglejoinmessages", chatInfo.isJoinMessages());
            JsonArray arr = new JsonArray();
            chatInfo.getIgnoring().forEach(u -> arr.add(u.toString()));
            obj.add("ignores", arr);

            @Cleanup FileWriter fw = new FileWriter(file, false);
            gson.toJson(obj, fw);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE,"Failed to save ChatInfo for&r&a %s. Please see the stacktrace below for more info", player.getUniqueId());
            t.printStackTrace();
        }
    }

    @Override
    public ChatInfo load(Player player) {
        try {
            File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));
            if (file.exists()) {

                @Cleanup FileReader reader = new FileReader(file);
                JsonObject obj = gson.fromJson(reader, JsonObject.class);

                boolean toggleChat = obj.has("togglechat") && obj.get("togglechat").getAsBoolean();
                boolean toggleJoinMessages = obj.has("togglejoinmessages") && obj.get("togglejoinmessages").getAsBoolean();
                HashSet<UUID> ignores = (obj.has("ignores")) ? parse(obj.get("ignores").getAsJsonArray()) : new HashSet<>();

                return new ChatInfo(player, cm, ignores, toggleChat, toggleJoinMessages);
            } else return new ChatInfo(player, cm);
        } catch (Throwable t) {
            GlobalUtils.log(Level.SEVERE, "Failed to parse %s. This is most likely due to malformed json", player.getUniqueId());
            t.printStackTrace();
            return null;
        }
    }

    private HashSet<UUID> parse(JsonArray arr) {
        HashSet<UUID> buf = new HashSet<>();
        arr.forEach(u -> buf.add(UUID.fromString(u.getAsString())));
        return buf;
    }

    @Override
    public void delete(Player player) {
        File file = new File(dataDir, player.getUniqueId().toString().concat(".json"));
        file.delete();
        GlobalUtils.log(Level.INFO,"Deleted ChatInfo file for %s", player.getName());
    }
}
