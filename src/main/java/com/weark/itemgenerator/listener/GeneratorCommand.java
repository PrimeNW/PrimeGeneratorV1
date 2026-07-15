package com.weark.itemgenerator.listener;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import com.weark.itemgenerator.model.ItemGenerator;
import com.weark.itemgenerator.util.GeneratorItemUtil;
import com.weark.itemgenerator.util.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class GeneratorCommand implements CommandExecutor {

    private final ItemGeneratorPlugin plugin;

    public GeneratorCommand(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager msg = plugin.getMessages();

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("itemgenerator.admin")) {
            player.sendMessage(msg.get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(msg.get("usage-main"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create": {
                Block target = player.getTargetBlock((Set<Material>) null, 6);
                if (target == null || target.getType() == Material.AIR) {
                    player.sendMessage(msg.get("no-target-block"));
                    return true;
                }

                int level = 1;
                if (args.length >= 2) {
                    try {
                        level = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(msg.get("invalid-level"));
                        return true;
                    }
                }

                if (!plugin.getGeneratorManager().getLevelConfig().hasLevel(level)) {
                    player.sendMessage(msg.get("level-not-defined"));
                    return true;
                }

                ItemStack heldItem = player.getItemInHand();
                if (heldItem == null || heldItem.getType() == Material.AIR) {
                    player.sendMessage(msg.get("no-item-in-hand"));
                    return true;
                }

                if (plugin.getGeneratorManager().getGeneratorAt(target.getLocation()) != null) {
                    player.sendMessage(msg.get("already-generator"));
                    return true;
                }

                ItemGenerator gen = plugin.getGeneratorManager().createGenerator(
                        target.getLocation(), player.getUniqueId(), player.getName(), level, heldItem, player.getLocation().getYaw());

                player.sendMessage(msg.get("generator-created",
                        "{level}", String.valueOf(gen.getLevel()),
                        "{item}", gen.getProducedItem().getType().name()));
                return true;
            }
            case "give": {
                if (args.length < 3) {
                    player.sendMessage(msg.get("give-usage"));
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(msg.get("give-player-not-found"));
                    return true;
                }
                int giveLevel;
                try {
                    giveLevel = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    player.sendMessage(msg.get("give-invalid-level"));
                    return true;
                }
                if (!plugin.getGeneratorManager().getLevelConfig().hasLevel(giveLevel)) {
                    player.sendMessage(msg.get("give-level-not-defined"));
                    return true;
                }
                ItemStack sourceItem = player.getItemInHand();
                if (sourceItem == null || sourceItem.getType() == Material.AIR) {
                    player.sendMessage(msg.get("give-need-held-item"));
                    return true;
                }

                ItemStack genItem = GeneratorItemUtil.createGeneratorItem(giveLevel, sourceItem);
                Map<Integer, ItemStack> leftover = target.getInventory().addItem(genItem);
                for (ItemStack extra : leftover.values()) {
                    target.getWorld().dropItem(target.getLocation(), extra);
                }

                player.sendMessage(msg.get("give-success-sender",
                        "{player}", target.getName(), "{level}", String.valueOf(giveLevel)));
                target.sendMessage(msg.get("give-success-target"));
                return true;
            }
            case "setitem": {
                Block target = player.getTargetBlock((Set<Material>) null, 6);
                if (target == null) {
                    player.sendMessage(msg.get("no-target-block"));
                    return true;
                }
                ItemGenerator gen = plugin.getGeneratorManager().getGeneratorAt(target.getLocation());
                if (gen == null) {
                    player.sendMessage(msg.get("no-generator-here"));
                    return true;
                }
                if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) {
                    player.sendMessage(msg.get("no-item-in-hand"));
                    return true;
                }
                Material newMaterial = player.getItemInHand().getType();
                plugin.getGeneratorManager().setGeneratorItem(gen, player.getItemInHand());
                player.sendMessage(msg.get("item-updated", "{item}", newMaterial.name()));
                return true;
            }
            case "wand": {
                ItemStack wand = new ItemStack(WandListener.WAND_MATERIAL, 1);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName(WandListener.WAND_NAME);
                meta.setLore(Arrays.asList(msg.getPlain("wand-lore-1"), msg.getPlain("wand-lore-2")));
                wand.setItemMeta(meta);
                player.getInventory().addItem(wand);
                player.sendMessage(msg.get("wand-given"));
                return true;
            }
            case "setorigin": {
                Block target = player.getTargetBlock((Set<Material>) null, 6);
                if (target == null) {
                    player.sendMessage(msg.get("no-target-block"));
                    return true;
                }
                plugin.getSchematicManager().setOrigin(player, target.getLocation());
                player.sendMessage(msg.get("origin-set"));
                return true;
            }
            case "selection": {
                player.sendMessage(ChatColor.YELLOW + plugin.getSchematicManager().getSelectionStatus(player));
                return true;
            }
            case "savestructure": {
                if (args.length < 2) {
                    player.sendMessage(msg.get("savestructure-usage"));
                    return true;
                }
                try {
                    boolean success = plugin.getSchematicManager().saveSchematic(player, args[1]);
                    if (success) {
                        player.sendMessage(msg.get("savestructure-success", "{name}", args[1]));
                    } else {
                        player.sendMessage(msg.get("savestructure-missing"));
                    }
                } catch (IOException ex) {
                    player.sendMessage(msg.get("savestructure-error", "{error}", ex.getMessage()));
                }
                return true;
            }
            case "remove": {
                Block target = player.getTargetBlock((Set<Material>) null, 6);
                if (target == null) {
                    player.sendMessage(msg.get("no-target-block"));
                    return true;
                }
                ItemGenerator gen = plugin.getGeneratorManager().getGeneratorAt(target.getLocation());
                if (gen == null) {
                    player.sendMessage(msg.get("no-generator-here"));
                    return true;
                }
                plugin.getGeneratorManager().removeGenerator(gen);
                player.sendMessage(msg.get("generator-removed"));
                return true;
            }
            case "reload": {
                plugin.getGeneratorManager().reloadConfigs();
                player.sendMessage(msg.get("config-reloaded"));
                return true;
            }
            default:
                player.sendMessage(msg.get("usage-main"));
                return true;
        }
    }
}
