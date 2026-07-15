package com.weark.itemgenerator.gui;

import com.weark.itemgenerator.model.ItemGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generator'un sanal deposunu (Cofre) gercek bir Inventory GUI'sinde gosterir.
 * Oyuncu itemleri buradan cekip alir; depodan (storage int) dusulur.
 */
public class GeneratorGUI {

    // Acik olan GUI'leri takip etmek icin: InventoryTitle bazli degil, Player -> Generator ID
    private static final Map<UUID, UUID> openGenerators = new HashMap<>();
    private static final int MAX_STACK_PER_SLOT = 64;
    private static final int ROWS = 3; // 27 slot

    public static void open(Player player, ItemGenerator gen) {
        int available = gen.getStorage();
        Inventory inv = Bukkit.createInventory(null, ROWS * 9,
                ChatColor.DARK_GRAY + "Cofre - " + ChatColor.GOLD + gen.getOwnerName());

        int slots = ROWS * 9;
        int remaining = available;
        ItemStack template = gen.getProducedItem();
        int maxStack = template.getMaxStackSize();
        for (int i = 0; i < slots && remaining > 0; i++) {
            int stackAmount = Math.min(maxStack, remaining);
            ItemStack item = template.clone();
            item.setAmount(stackAmount);
            inv.setItem(i, item);
            remaining -= stackAmount;
        }

        openGenerators.put(player.getUniqueId(), gen.getId());
        player.openInventory(inv);
    }

    public static UUID getOpenGeneratorId(Player player) {
        return openGenerators.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        openGenerators.remove(player.getUniqueId());
    }

    /**
     * GUI kapandiginda: envanterde kalan itemleri sayip generator'un
     * storage degerini gunceller (oyuncu kismen alip kapatmis olabilir).
     */
    public static int countRemainingItems(Inventory inv) {
        int total = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
