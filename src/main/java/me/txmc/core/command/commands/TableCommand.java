package me.txmc.core.command.commands;

import me.txmc.core.command.BaseTabCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.sendPrefixedLocalizedMessage;

/**
 * Opens different block interfaces like crafting table, enchanting table, etc.
 *
 * <p>This command is part of the 8b8tCore plugin and allows players to
 * open utility interfaces without placing blocks.</p>
 *
 * @author agarciacorte
 * @since 2025/04/16
 */

public class TableCommand extends BaseTabCommand {
    private final List<String> tableTypes;

    public TableCommand() {
        super("table", "/table <type>", "8b8tcore.command.table");
        tableTypes = getTableTypes();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, PLAYER_ONLY);
            return;
        }

        if (args.length != 1) {
            sendPrefixedLocalizedMessage(player, "table_usage");
            return;
        }

        String type = args[0].toLowerCase();

        switch (type) {
            case "crafting", "crafting_table" -> player.openWorkbench(null, true);
            case "cartography", "cartography_table" -> player.openCartographyTable(player.getLocation(), true);
            case "stonecutter" -> player.openStonecutter(player.getLocation(), true);
            case "enchanting", "enchanting_table" -> player.openEnchanting(player.getLocation(), true);
            case "anvil" -> player.openAnvil(player.getLocation(), true);
            case "grindstone" -> player.openGrindstone(player.getLocation(), true);
            case "loom" -> player.openLoom(player.getLocation(), true);
            case "smithing", "smithing_table" -> player.openSmithingTable(player.getLocation(), true);
            default -> {
                sendPrefixedLocalizedMessage(player, "table_invalid_type");
            }
        }

    }

    @Override
    public List<String> onTab(String[] args) {
        if (args.length == 1) {
            return tableTypes.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getTableTypes() {
        return List.of(
                "crafting",
                "cartography",
                "stonecutter",
                "enchanting",
                "anvil",
                "grindstone",
                "loom",
                "smithing"
        );
    }
}
