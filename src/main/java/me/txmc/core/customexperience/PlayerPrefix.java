package me.txmc.core.customexperience;

import me.txmc.core.customexperience.util.PrefixManager;
import me.txmc.core.database.GeneralDatabase;
import me.txmc.core.util.GlobalUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Quản lý và thiết lập tiền tố của người chơi trong danh sách người chơi khi họ tham gia máy chủ.
 *
 * <p>Lớp này là một phần của plugin 8b8tCore, cung cấp các chức năng tùy chỉnh
 * bao gồm tiền tố người chơi dựa trên quyền của họ.</p>
 *
 * <p>Chức năng bao gồm:</p>
 * <ul>
 *     <li>Gán tiền tố thích hợp cho người chơi dựa trên quyền của họ khi tham gia máy chủ</li>
 *     <li>Cập nhật tên hiển thị của người chơi trong danh sách người chơi với tiền tố đã gán</li>
 * </ul>
 *
 * @author Minelord9000 (agarciacorte)
 * @since 2024/08/11 03:42 PM
 */
public class PlayerPrefix implements Listener {
    private final JavaPlugin plugin;
    private final PrefixManager prefixManager = new PrefixManager();
    private final GeneralDatabase database;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerPrefix(JavaPlugin plugin) {
        this.plugin = plugin;
        this.database = new GeneralDatabase(plugin.getDataFolder().getAbsolutePath());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String tag = prefixManager.getPrefix(event.getPlayer());
        setupTag(event.getPlayer(), tag);

        String displayName = database.getNickname(event.getPlayer().getName());
        if (displayName != null && !displayName.isEmpty()) {
            Component component = miniMessage.deserialize(GlobalUtils.convertToMiniMessageFormat(displayName));
            event.getPlayer().displayName(component);
        }
    }

    public void setupTag(Player player, String tag) {
        player.setPlayerListName(tag + player.getDisplayName());
    }
}
