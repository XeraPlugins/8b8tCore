package me.txmc.core;

import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;

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
        if (!localeDir.exists()) localeDir.mkdirs();
        GlobalUtils.unpackResource("localization/en.yml", new File(localeDir, "en.yml"));
        GlobalUtils.unpackResource("localization/es.yml", new File(localeDir, "es.yml"));
        for (File ymlFile : localeDir.listFiles(f -> f.getName().endsWith(".yml"))) {
            Configuration config = YamlConfiguration.loadConfiguration(ymlFile);
            localizationMap.put(ymlFile.getName().replace(".yml", ""), new Localization(config));
        }
    }

    public static Localization getLocalization(String locale) {
        if (localizationMap.containsKey(locale)) return localizationMap.get(locale);
        String first = locale.split("_")[0];
        if (localizationMap.containsKey(first)) return localizationMap.get(first);
        return localizationMap.get("en");
    }

    public String get(String key) {
        return config.getString(key, String.format("Unknown key %s", key));
    }
    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }
}
