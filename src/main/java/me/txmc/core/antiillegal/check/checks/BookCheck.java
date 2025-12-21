package me.txmc.core.antiillegal.check.checks;

import me.txmc.core.antiillegal.check.Check;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author 254n_m
 * @since 2024/01/29 12:03 PM
 * This file was created as a part of 8b8tCore
 */
public class BookCheck implements Check {
    private final CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();

    @Override
    public boolean check(ItemStack item) {
        BookMeta meta;
        try {
            meta = (BookMeta) item.getItemMeta();
        } catch (Exception e) {
            return false;
        }
        String[] pages = getPages(meta).orElse(new String[0]);
        return !(pages != null && encoder.canEncode(String.join(" ", pages)));
    }

    @Override
    public boolean shouldCheck(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta() instanceof BookMeta;
    }

    @Override
    public void fix(ItemStack item) {
        BookMeta meta = (BookMeta) item.getItemMeta();
        List<Component> cleanPages = new ArrayList<>();

        String[] currPages = getPages(meta).orElse(new String[0]);

        for (String page : currPages) {
            StringBuilder builder = new StringBuilder();
            for (char c : page.toCharArray()) {
                if (encoder.canEncode(c)) {
                    builder.append(c);
                }
            }
            TextComponent cleanComponent = Component.text(builder.toString());
            cleanPages.add(cleanComponent);
        }

        meta.pages(cleanPages);
        item.setItemMeta(meta);
    }

    private Optional<String[]> getPages(BookMeta meta) {
        if (!meta.hasPages()) {
            return Optional.empty();
        }

        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        return Optional.of(
                meta.pages().stream()
                        .map(serializer::serialize)
                        .toArray(String[]::new)
        );
    }
}
