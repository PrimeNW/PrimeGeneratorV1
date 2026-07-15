package com.weark.itemgenerator.util;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * config.yml'deki "language: en|tr|br" ayarina gore lang/<dil>.yml dosyasini yukler.
 * Dosya plugin klasorunde yoksa jar icindeki varsayilani diske kopyalar.
 * Kullanim: messages.get("generator-created", "{level}", "5")
 */
public class MessageManager {

    private final ItemGeneratorPlugin plugin;
    private YamlConfiguration messages;
    private String currentLanguage;

    public MessageManager(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();
        if (!lang.equals("en") && !lang.equals("tr") && !lang.equals("br")) {
            plugin.getLogger().warning("Gecersiz 'language' degeri: " + lang + " - 'en' kullanilacak.");
            lang = "en";
        }
        this.currentLanguage = lang;

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, langFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("lang/" + lang + ".yml kopyalanamadi: " + e.getMessage());
            }
        }

        if (langFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(langFile);
        } else {
            // Son care: jar icinden dogrudan bellege oku
            messages = new YamlConfiguration();
            try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
                if (in != null) {
                    messages.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
            }
        }
    }

    public String getLanguage() {
        return currentLanguage;
    }

    /** Ham (renk kodu cevrilmemis) deger dondurur - lore gibi ozel durumlar icin. */
    public String getRaw(String key) {
        if (messages == null) return key;
        return messages.getString(key, key);
    }

    /** {@code /placeholder-value/...} ciftleriyle degistirilmis, renk kodlari islenmis mesaj. */
    public String get(String key, String... replacements) {
        String prefix = ChatColor.translateAlternateColorCodes('&', getRaw("prefix"));
        String raw = getRaw(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + raw);
    }

    /** Prefix eklemeden, sadece placeholder + renk islemesi (hologram satirlari icin). */
    public String getPlain(String key, String... replacements) {
        String raw = getRaw(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
