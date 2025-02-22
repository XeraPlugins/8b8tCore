package me.txmc.core;

import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * @author 254n_m
 * @since 2023/12/18 1:50 PM
 * This file was created as a part of 8b8tCore
 */
@RequiredArgsConstructor
public class Localization {
    private static HashMap<String, Localization> localizationMap;
    private final Configuration config;

    protected static void loadLocalizations(File dataFolder) {
        if (localizationMap != null) localizationMap.clear();
        localizationMap = new HashMap<>();
        File localeDir = new File(dataFolder, "Localization");

        if (!localeDir.exists()) {
            localeDir.mkdirs();
        } else {
            File[] existingYmlFiles = localeDir.listFiles(f -> f.getName().endsWith(".yml"));
            if (existingYmlFiles != null) {
                for (File file : existingYmlFiles) {
                    if (!file.delete()) {
                        GlobalUtils.log(Level.SEVERE, "Failed to delete localization file: " + file.getName());
                    }
                }
            }
        }

        GlobalUtils.unpackResource("localization/ar.yml", new File(localeDir, "ar.yml"));
        GlobalUtils.unpackResource("localization/en.yml", new File(localeDir, "en.yml"));
        GlobalUtils.unpackResource("localization/es.yml", new File(localeDir, "es.yml"));
        GlobalUtils.unpackResource("localization/fr.yml", new File(localeDir, "fr.yml"));
        GlobalUtils.unpackResource("localization/hi.yml", new File(localeDir, "hi.yml"));
        GlobalUtils.unpackResource("localization/it.yml", new File(localeDir, "it.yml"));
        GlobalUtils.unpackResource("localization/jp.yml", new File(localeDir, "jp.yml"));
        GlobalUtils.unpackResource("localization/pt.yml", new File(localeDir, "pt.yml"));
        GlobalUtils.unpackResource("localization/ru.yml", new File(localeDir, "ru.yml"));
        GlobalUtils.unpackResource("localization/tr.yml", new File(localeDir, "tr.yml"));
        GlobalUtils.unpackResource("localization/zh.yml", new File(localeDir, "zh.yml"));

        File[] ymlFiles = localeDir.listFiles(f -> f.getName().endsWith(".yml"));
        if (ymlFiles != null) {
            for (File ymlFile : ymlFiles) {
                Configuration config = YamlConfiguration.loadConfiguration(ymlFile);
                localizationMap.put(ymlFile.getName().replace(".yml", ""), new Localization(config));
            }
        }
    }

    public static Localization getLocalization(String locale) {
        if (localizationMap.containsKey(locale)) return localizationMap.get(locale);
        String first = locale.split("_")[0];
        if (localizationMap.containsKey(first)) return localizationMap.get(first);
        return localizationMap.get("en");
    }

    public String getPrefix() {
        return config.getString("prefix", "&8[&98b&78t&8]");
    }
    public String getColorPrimary() { return config.getString("PluginColors.color_primary", "&9"); }           //&6 GOLD
    public String getColorSecondary() { return config.getString("PluginColors.color_secondary", "&7"); }       //&3 DARK AQUA
    public String getColorPositive() { return config.getString("PluginColors.color_positive", "&a"); }         //&a GREEN
    public String getColorNegative() {
        return config.getString("PluginColors.color_negative", "&c");
    }         //&c RED

    public String get(String key) {
        String value = config.getString(key, String.format("Unknown key %s", key));

        if (key.endsWith("-messages")) {
            return value.replaceAll("%prefix%", getPrefix());
        }

        return value
                .replaceAll("%prefix%", getPrefix())
                .replaceAll("&6", getColorPrimary())
                .replaceAll("&3", getColorSecondary())
                .replaceAll("&a", getColorPositive())
                .replaceAll("&c", getColorNegative());
    }

    public List<String> getStringList(String key) {
        List<String> values = config.getStringList(key).stream()
                .map(s -> s.replaceAll("%prefix%", getPrefix()))
                .toList();

        if (key.endsWith("-messages")) {
            return values;
        }

        return values.stream()
                .map(s -> s.replaceAll("&6", getColorPrimary())
                        .replaceAll("&3", getColorSecondary())
                        .replaceAll("&a", getColorPositive())
                        .replaceAll("&c", getColorNegative()))
                .toList();
    }
}
