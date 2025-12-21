package me.txmc.core.command.commands;

import me.txmc.core.Main;
import me.txmc.core.command.BaseTabCommand;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.*;
import java.util.stream.Collectors;

import static me.txmc.core.util.GlobalUtils.*;

public class ClearEntitiesCommand extends BaseTabCommand {
    private final List<String> clearEntitiesOptions;

    public ClearEntitiesCommand() {
        super(
                "clearentities",
                "/clearentities <nearest | unnecessary | hostile>",
                "8b8tcore.command.clearentities",
                "Clear entities in a safe manner"
        );
        clearEntitiesOptions = List.of("nearest", "unnecessary", "hostile");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "&cSyntax error: /clearentities <nearest | unnecessary | hostile>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "nearest":
                clearNearestEntity(sender);
                break;
            case "unnecessary":
                clearUnnecessaryEntities(sender);
                break;
            case "hostile":
                clearHostileEntities(sender);
                break;
            default:
                sendMessage(sender, "&cInvalid Option: /clearentities <nearest | unnecessary | hostile>");
                break;
        }
    }

    private void clearNearestEntity(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cThis command can only be executed by a player.");
            return;
        }

        Player player = (Player) sender;
        Location playerLocation = player.getLocation();
        World world = player.getWorld();

        Bukkit.getRegionScheduler().execute(Main.getInstance(), playerLocation, () -> {
            Entity nearestEntity = world.getNearbyEntities(playerLocation, 10, 10, 10).stream()
                    .filter(entity -> !isProtectedEntity(entity) && !(entity instanceof Player))
                    .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(playerLocation)))
                    .orElse(null);

            if (nearestEntity != null) {
                if (nearestEntity.isValid()) {
                    nearestEntity.remove();
                    sendMessage(sender, "&aCleared the nearest entity.");
                }
            } else {
                sendMessage(sender, "&cNo entities found to remove.");
            }
        });
    }

    private void clearUnnecessaryEntities(CommandSender sender) {
        int[] totalCount = {0};

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Bukkit.getRegionScheduler().execute(Main.getInstance(), chunk.getBlock(0, 0, 0).getLocation(), () -> {
                    int count = 0;
                    for (Entity entity : chunk.getEntities()) {
                        if (!isProtectedEntity(entity) && !(entity instanceof Player)) {
                            if (entity.isValid()) {
                                entity.remove();
                                count++;
                            }
                        }
                    }
                    totalCount[0] += count;
                });
            }
        }

        Bukkit.getGlobalRegionScheduler().execute(Main.getInstance(), () -> {
            sendMessage(sender, "&aCleared " + totalCount[0] + " unnecessary entities.");
        });
    }

    private void clearHostileEntities(CommandSender sender) {
        int[] totalCount = {0};

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Bukkit.getRegionScheduler().execute(Main.getInstance(), chunk.getBlock(0, 0, 0).getLocation(), () -> {
                    int count = 0;
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof Enemy || entity instanceof Boss || entity instanceof Monster) {
                            if (entity.isValid()) {
                                entity.remove();
                                count++;
                            }
                        }
                    }
                    totalCount[0] += count;
                });
            }
        }

        Bukkit.getGlobalRegionScheduler().execute(Main.getInstance(), () -> {
            sendMessage(sender, "&aCleared " + totalCount[0] + " hostile entities.");
        });
    }

    private boolean isProtectedEntity(Entity entity) {
        return entity instanceof ItemFrame ||
                entity instanceof Painting ||
                entity instanceof Minecart ||
                entity instanceof Boat ||
                entity instanceof Tameable ||
                entity instanceof ArmorStand ||
                entity instanceof LeashHitch ||
                entity instanceof IronGolem;
    }

    @Override
    public List<String> onTab(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1) {
            return clearEntitiesOptions.stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}