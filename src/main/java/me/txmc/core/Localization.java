package me.txmc.core;

import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;


@RequiredArgsConstructor
public class Localization {
    private static HashMap<String, Localization> localizationMap;
    private final Configuration config;

    protected static void loadLocalizations(File dataFolder) {
        if (localizationMap != null) localizationMap.clear();
        localizationMap = new HashMap<>();

        File localeDir = new File(dataFolder, "Localization");
        if (!localeDir.exists() && !localeDir.mkdirs()) {
            GlobalUtils.log(Level.SEVERE, "Could not create localization directory: %s", localeDir.getAbsolutePath());
            return;
        }

        String[] locales = {"ar", "en", "es", "fr", "hi", "it", "jp", "pt", "ru", "tr", "zh"};
        for (String locale : locales) {
            File file = new File(localeDir, locale + ".yml");
            GlobalUtils.unpackResource("Localization/" + locale + ".yml", file);
        }

        File[] ymlFiles = localeDir.listFiles(f -> f.isFile() && f.getName().endsWith(".yml"));
        if (ymlFiles == null) return;

        for (File ymlFile : ymlFiles) {
            try {
                Configuration cfg = YamlConfiguration.loadConfiguration(ymlFile);
                String key = ymlFile.getName().substring(0, ymlFile.getName().length() - 4);
                localizationMap.put(key, new Localization(cfg));
            } catch (Exception e) {
                GlobalUtils.log(Level.SEVERE, "Failed to load localization file %s: %s", ymlFile.getName(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Localization getLocalization(String locale) {
        if (localizationMap.containsKey(locale)) return localizationMap.get(locale);
        String base = locale.split("[_-]")[0];
        if (localizationMap.containsKey(base)) return localizationMap.get(base);
        return localizationMap.getOrDefault("en", new Localization(YamlConfiguration.loadConfiguration(new File(""))));
    }

    public String getPrefix() { return config.getString("prefix", "&8[&98b&78t&8]"); }

    public String get(String key) {
        String val = config.getString(key, String.format("Unknown key %s", key));
        String withPrefix = val.replace("%prefix%", getPrefix());
        return GlobalUtils.convertToMiniMessageFormat(withPrefix);
    }

    public List<String> getStringList(String key) {
        List<String> list = config.getStringList(key);
        return list.stream()
                .map(line -> {
                    String withPrefix = line.replace("%prefix%", getPrefix());
                    return GlobalUtils.convertToMiniMessageFormat(withPrefix);
                })
                .toList();
    }

    public String getWithPlaceholders(String key, String... replacements) {
        String val = config.getString(key, String.format("Unknown key %s", key));
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                val = val.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        String withPrefix = val.replace("%prefix%", getPrefix());
        return GlobalUtils.convertToMiniMessageFormat(withPrefix);
    }
}
