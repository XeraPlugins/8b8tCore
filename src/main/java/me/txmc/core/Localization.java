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
        return config.getString("prefix", "&6[8b8tCore]");
    }

    public String get(String key) {
        return config.getString(key, String.format("Unknown key %s", key))
                .replace("%prefix%", getPrefix());
    }

    public List<String> getStringList(String key) {
        return config.getStringList(key).stream()
                .map(s -> s.replace("%prefix%", getPrefix()))
                .toList();
    }
}
