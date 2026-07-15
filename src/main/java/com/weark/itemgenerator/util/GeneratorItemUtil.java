package com.weark.itemgenerator.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Bir "Generator Item"i normal envanterde tasinabilen, elden verilebilen,
 * baska bir yere yerlestirilebilen ozel bir esyaya donusturur.
 * Item icine (lore'un son satirina gizlenmis, base64 kodlu) seviye + uretilen item bilgisi gomulur.
 */
public class GeneratorItemUtil {

    private static final String MARKER_PREFIX = ChatColor.BLACK + "IGDATA:";

    public static ItemStack createGeneratorItem(int level, ItemStack producedItem) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Item Generator" + ChatColor.GRAY + " [Nv." + level + "]");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Uretilen esya: " + ChatColor.WHITE + prettyName(producedItem));
        lore.add(ChatColor.YELLOW + "Yerlestirmek icin bir bloga tikla.");
        lore.add(encode(level, producedItem));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isGeneratorItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<String> lore = item.getItemMeta().getLore();
        return !lore.isEmpty() && lore.get(lore.size() - 1).startsWith(MARKER_PREFIX);
    }

    public static int getLevel(ItemStack item) {
        Decoded d = decode(item);
        return d == null ? 1 : d.level;
    }

    public static ItemStack getProducedItem(ItemStack item) {
        Decoded d = decode(item);
        return d == null ? new ItemStack(Material.DIAMOND, 1) : d.producedItem;
    }

    private static String prettyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private static String encode(int level, ItemStack producedItem) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("level", level);
        cfg.set("item", producedItem);
        String raw = cfg.saveToString();
        String base64 = Base64.getEncoder().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return MARKER_PREFIX + base64;
    }

    private static Decoded decode(ItemStack item) {
        if (!isGeneratorItem(item)) return null;
        List<String> lore = item.getItemMeta().getLore();
        String marked = lore.get(lore.size() - 1);
        String base64 = marked.substring(MARKER_PREFIX.length());
        try {
            String raw = new String(Base64.getDecoder().decode(base64), java.nio.charset.StandardCharsets.UTF_8);
            YamlConfiguration cfg = new YamlConfiguration();
            cfg.loadFromString(raw);
            int level = cfg.getInt("level", 1);
            ItemStack producedItem = cfg.getItemStack("item");
            if (producedItem == null) producedItem = new ItemStack(Material.DIAMOND, 1);
            return new Decoded(level, producedItem);
        } catch (Exception e) {
            return null;
        }
    }

    private static class Decoded {
        final int level;
        final ItemStack producedItem;

        Decoded(int level, ItemStack producedItem) {
            this.level = level;
            this.producedItem = producedItem;
        }
    }
}
