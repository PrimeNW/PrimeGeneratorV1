package com.weark.itemgenerator.listener;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import com.weark.itemgenerator.gui.GeneratorGUI;
import com.weark.itemgenerator.model.ItemGenerator;
import com.weark.itemgenerator.util.GeneratorItemUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GeneratorListener implements Listener {

    private final ItemGeneratorPlugin plugin;

    public GeneratorListener(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player player = e.getPlayer();
        ItemStack hand = player.getItemInHand();

        // 1) Elinde Generator Item varsa: yerlestirme islemi (komutla olusturmus gibi ayni sonuc)
        if (GeneratorItemUtil.isGeneratorItem(hand)) {
            e.setCancelled(true);

            if (!player.hasPermission("itemgenerator.use")) {
                player.sendMessage(plugin.getMessages().get("no-permission"));
                return;
            }

            Block placedOn = e.getClickedBlock().getRelative(e.getBlockFace());
            Location placeLoc = placedOn.getLocation();

            if (plugin.getGeneratorManager().getGeneratorAt(placeLoc) != null) {
                player.sendMessage(plugin.getMessages().get("already-placed-here"));
                return;
            }

            int level = GeneratorItemUtil.getLevel(hand);
            ItemStack producedItem = GeneratorItemUtil.getProducedItem(hand);

            placedOn.setType(org.bukkit.Material.SANDSTONE); // generator'un oturdugu ana blok

            ItemGenerator gen = plugin.getGeneratorManager().createGenerator(
                    placeLoc, player.getUniqueId(), player.getName(), level, producedItem, player.getLocation().getYaw());

            if (hand.getAmount() > 1) {
                hand.setAmount(hand.getAmount() - 1);
            } else {
                player.setItemInHand(null);
            }

            player.sendMessage(plugin.getMessages().get("generator-placed", "{level}", String.valueOf(gen.getLevel())));
            return;
        }

        // 2) Var olan bir generator'a tiklandiysa: Cofre GUI'sini ac
        ItemGenerator gen = plugin.getGeneratorManager().getGeneratorAt(e.getClickedBlock().getLocation());
        if (gen == null) return;

        e.setCancelled(true);

        if (!player.hasPermission("itemgenerator.use")) {
            player.sendMessage(plugin.getMessages().get("no-permission"));
            return;
        }

        if (gen.getStorage() <= 0) {
            player.sendMessage(plugin.getMessages().get("chest-empty"));
            return;
        }

        GeneratorGUI.open(player, gen);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        ItemGenerator gen = plugin.getGeneratorManager().getGeneratorAt(e.getBlock().getLocation());
        if (gen == null) return;

        Player player = e.getPlayer();
        boolean isOwner = gen.getOwner().equals(player.getUniqueId());

        if (!isOwner && !player.hasPermission("itemgenerator.admin")) {
            e.setCancelled(true);
            player.sendMessage(plugin.getMessages().get("generator-not-yours"));
            return;
        }

        e.setCancelled(true);

        ItemStack generatorItem = GeneratorItemUtil.createGeneratorItem(gen.getLevel(), gen.getProducedItem());
        plugin.getGeneratorManager().removeGenerator(gen);
        e.getBlock().setType(org.bukkit.Material.AIR);

        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(generatorItem);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }

        player.sendMessage(plugin.getMessages().get("generator-picked-up"));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();

        UUID genId = GeneratorGUI.getOpenGeneratorId(player);
        if (genId == null) return;

        ItemGenerator gen = plugin.getGeneratorManager().getAllGenerators().stream()
                .filter(g -> g.getId().equals(genId))
                .findFirst()
                .orElse(null);

        GeneratorGUI.clear(player);
        if (gen == null) return;

        int remaining = GeneratorGUI.countRemainingItems(e.getInventory());
        gen.setStorage(remaining);
        plugin.getGeneratorManager().saveGenerator(gen);

        player.sendMessage(plugin.getMessages().get("chest-updated",
                "{storage}", String.valueOf(gen.getStorage()),
                "{max}", String.valueOf(gen.getMaxStorage())));
    }
}
