package me.txmc.core.chat.tasks;

import me.txmc.core.Localization;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AnnouncementTask implements Runnable {
    private final Random random;

    public AnnouncementTask() {
        this.random = new Random();
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Localization loc = Localization.getLocalization(p.locale().getLanguage());
            List<String> announcements = loc.getStringList("announcements")
                    .stream()
                    .map(s -> s.replace("%prefix%", loc.getPrefix()))
                    .map(this::c).toList();
            String announcement = announcements.get(random.nextInt(announcements.size()));
            p.sendMessage(announcement);
        }
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
