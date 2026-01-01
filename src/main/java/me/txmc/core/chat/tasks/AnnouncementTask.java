package me.txmc.core.chat.tasks;

import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AnnouncementTask implements Runnable {

    private ThreadLocalRandom random = ThreadLocalRandom.current();
    private GeneralDatabase database;
    @Override
    public void run() {
        Bukkit.getGlobalRegionScheduler().runDelayed(me.txmc.core.Main.getInstance(), (task) -> {
            if (database == null && Main.getInstance() != null) {
                this.database = GeneralDatabase.getInstance();
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                database.getPlayerHideAnnouncementsAsync(p.getName()).thenAccept(hideAnnouncements -> {
                    if (hideAnnouncements || !p.isOnline()) {
                        return;
                    }
                    p.getScheduler().run(Main.getInstance(), (playerTask) -> {
                        if (!p.isOnline()) return;
                        Localization loc = Localization.getLocalization(p.locale().getLanguage());
                        List<TextComponent> announcements = loc.getStringList("announcements")
                                .stream()
                                .map(s -> s.replace("%prefix%", loc.getPrefix()))
                                .map(GlobalUtils::translateChars).toList();
                        Component announcement = announcements.get(random.nextInt(announcements.size()));
                        p.sendMessage(announcement);
                    }, null);
                });
            }
        }, 1L);
    }
}
