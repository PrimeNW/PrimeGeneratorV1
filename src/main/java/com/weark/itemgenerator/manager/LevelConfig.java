package com.weark.itemgenerator.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * config.yml icindeki "levels" bolumunu okuyup bellekte tutar.
 */
public class LevelConfig {

    public static class LevelData {
        public final int intervalSeconds;
        public final int maxStorage;
        public final int produceAmount;

        public LevelData(int intervalSeconds, int maxStorage, int produceAmount) {
            this.intervalSeconds = intervalSeconds;
            this.maxStorage = maxStorage;
            this.produceAmount = produceAmount;
        }
    }

    private final Map<Integer, LevelData> levels = new HashMap<>();
    private int maxLevel = 1;

    public void load(FileConfiguration config) {
        levels.clear();
        ConfigurationSection section = config.getConfigurationSection("levels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                ConfigurationSection levelSection = section.getConfigurationSection(key);
                if (levelSection == null) continue;

                int interval = levelSection.getInt("interval-seconds", 5);
                int maxStorage = levelSection.getInt("max-storage", 128);
                int produceAmount = levelSection.getInt("produce-amount", 1);

                levels.put(level, new LevelData(interval, maxStorage, produceAmount));
                if (level > maxLevel) maxLevel = level;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public LevelData get(int level) {
        return levels.getOrDefault(level, levels.get(1));
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean hasLevel(int level) {
        return levels.containsKey(level);
    }
}
