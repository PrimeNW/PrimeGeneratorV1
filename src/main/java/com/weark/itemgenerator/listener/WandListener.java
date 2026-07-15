package com.weark.itemgenerator.listener;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class WandListener implements Listener {

    public static final Material WAND_MATERIAL = Material.BLAZE_ROD;
    public static final String WAND_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "Generator Wand";

    private final ItemGeneratorPlugin plugin;

    public WandListener(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != WAND_MATERIAL) return;
        if (!e.getItem().hasItemMeta() || !WAND_NAME.equals(e.getItem().getItemMeta().getDisplayName())) return;
        if (e.getClickedBlock() == null) return;

        Player player = e.getPlayer();
        if (!player.hasPermission("itemgenerator.admin")) return;

        e.setCancelled(true);

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getSchematicManager().setPos1(player, e.getClickedBlock().getLocation());
            player.sendMessage(plugin.getMessages().get("wand-pos1", "{loc}", formatLoc(e.getClickedBlock().getLocation())));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getSchematicManager().setPos2(player, e.getClickedBlock().getLocation());
            player.sendMessage(plugin.getMessages().get("wand-pos2", "{loc}", formatLoc(e.getClickedBlock().getLocation())));
        }
    }

    private String formatLoc(org.bukkit.Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
