package me.txmc.core;

import lombok.RequiredArgsConstructor;
import me.txmc.core.util.GlobalUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


@RequiredArgsConstructor
public class Localization {
    private static HashMap<String, Localization> localizationMap;
    private static final HashMap<String, Localization> localeCache = new HashMap<>();
    private final Configuration config;
    private final ConcurrentHashMap<String, String> stringCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> listCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> rawCache = new ConcurrentHashMap<>();
    private String prefix;

    protected static void loadLocalizations(File dataFolder) {
        if (localizationMap != null) localizationMap.clear();
        localeCache.clear();
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
        if (localeCache.containsKey(locale)) return localeCache.get(locale);
        
        Localization loc;
        if (localizationMap.containsKey(locale)) {
            loc = localizationMap.get(locale);
        } else {
            String base = locale.split("[_-]")[0];
            if (localizationMap.containsKey(base)) {
                loc = localizationMap.get(base);
            } else {
                loc = localizationMap.getOrDefault("en", new Localization(YamlConfiguration.loadConfiguration(new File(""))));
            }
        }
        
        localeCache.put(locale, loc);
        return loc;
    }

    public String getPrefix() {
        if (prefix == null) prefix = config.getString("prefix", "&8[&98b&78t&8]");
        return prefix;
    }

    public String get(String key) {
        return stringCache.computeIfAbsent(key, k -> {
            String val = config.getString(k, String.format("Unknown key %s", k));
            String withPrefix = val.replace("%prefix%", getPrefix());
            return GlobalUtils.convertToMiniMessageFormat(withPrefix);
        });
    }

    public List<String> getStringList(String key) {
        return listCache.computeIfAbsent(key, k -> {
            List<String> list = config.getStringList(k);
            return list.stream()
                    .map(line -> {
                        String withPrefix = line.replace("%prefix%", getPrefix());
                        return GlobalUtils.convertToMiniMessageFormat(withPrefix);
                    })
                    .toList();
        });
    }

    public List<net.kyori.adventure.text.TextComponent> getComponentList(String key) {
        return getStringList(key).stream().map(GlobalUtils::translateChars).toList();
    }

    public String getWithPlaceholders(String key, String... replacements) {
        String val = rawCache.computeIfAbsent(key, k -> config.getString(k, String.format("Unknown key %s", k)));
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                val = val.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        String withPrefix = val.replace("%prefix%", getPrefix());
        return GlobalUtils.convertToMiniMessageFormat(withPrefix);
    }
}
