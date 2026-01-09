package me.txmc.core.chat.tasks;

import me.txmc.core.Localization;
import me.txmc.core.Main;
import me.txmc.core.chat.ChatInfo;
import me.txmc.core.chat.ChatSection;
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
        Main plugin = Main.getInstance();
        if (plugin == null) return;
        ChatSection chatSection = (ChatSection) plugin.getSectionByName("ChatControl");
        if (chatSection == null) return;

        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                me.txmc.core.chat.ChatInfo info = chatSection.getInfo(p);
                if (info == null || info.isHideAnnouncements()) continue;

                p.getScheduler().run(plugin, (playerTask) -> {
                    if (!p.isOnline()) return;
                    Localization loc = Localization.getLocalization(p.locale().getLanguage());
                    List<net.kyori.adventure.text.TextComponent> announcements = loc.getComponentList("announcements");
                    if (announcements.isEmpty()) return;
                    Component announcement = announcements.get(random.nextInt(announcements.size()));
                    p.sendMessage(announcement);
                }, null);
            }
        }, 1L);
    }
}
