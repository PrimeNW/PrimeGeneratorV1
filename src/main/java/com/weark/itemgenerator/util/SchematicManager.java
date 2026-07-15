package com.weark.itemgenerator.util;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Oyuncunun oyun icinde sectigi iki kose (pos1/pos2) ve bir merkez nokta (origin) baz alinarak
 * blok blok yapiyi bir .yml dosyasina kaydeder ve daha sonra herhangi bir generator konumuna
 * (origin ile hizalanarak) yeniden insa edilmesini saglar. Harici WorldEdit/schematic dosyasi
 * gerektirmez.
 */
public class SchematicManager {

    private final ItemGeneratorPlugin plugin;
    private final File folder;

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Location> origin = new HashMap<>();

    public SchematicManager(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "schematics");
        if (!folder.exists()) folder.mkdirs();
    }

    public void setPos1(Player player, Location loc) {
        pos1.put(player.getUniqueId(), loc);
    }

    public void setPos2(Player player, Location loc) {
        pos2.put(player.getUniqueId(), loc);
    }

    public void setOrigin(Player player, Location loc) {
        origin.put(player.getUniqueId(), loc);
    }

    public Location getPos1(Player player) { return pos1.get(player.getUniqueId()); }
    public Location getPos2(Player player) { return pos2.get(player.getUniqueId()); }
    public Location getOrigin(Player player) { return origin.get(player.getUniqueId()); }

    public String getSelectionStatus(Player player) {
        Location p1 = getPos1(player);
        Location p2 = getPos2(player);
        Location o = getOrigin(player);
        String none = plugin.getMessages().getRaw("hologram-boost-none"); // "yok/none/nenhum" kelimesini yeniden kullan
        return plugin.getMessages().getPlain("selection-status",
                "{pos1}", p1 == null ? none : format(p1),
                "{pos2}", p2 == null ? none : format(p2),
                "{origin}", o == null ? none : format(o));
    }

    private String format(Location l) {
        return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    /**
     * Secili bolgeyi (pos1-pos2) merkez noktaya (origin) gore relatif olarak kaydeder.
     * @return true basarili, false eksik secim / farkli dunya
     */
    public boolean saveSchematic(Player player, String name) throws IOException {
        Location p1 = getPos1(player);
        Location p2 = getPos2(player);
        Location o = getOrigin(player);
        if (p1 == null || p2 == null || o == null) return false;
        if (!p1.getWorld().equals(p2.getWorld()) || !p1.getWorld().equals(o.getWorld())) return false;

        World world = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        List<Map<String, Object>> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) continue;

                    Map<String, Object> entry = new HashMap<>();
                    entry.put("dx", x - o.getBlockX());
                    entry.put("dy", y - o.getBlockY());
                    entry.put("dz", z - o.getBlockZ());
                    entry.put("material", block.getType().name());
                    entry.put("data", block.getData());
                    blocks.add(entry);
                }
            }
        }

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("blocks", blocks);
        cfg.save(new File(folder, name + ".yml"));
        return true;
    }

    public boolean exists(String name) {
        return new File(folder, name + ".yml").exists();
    }

    @SuppressWarnings("unchecked")
    public List<BlockDef> load(String name) {
        File file = new File(folder, name + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<?> rawList = cfg.getList("blocks");
        List<BlockDef> result = new ArrayList<>();
        if (rawList == null) return result;

        for (Object obj : rawList) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) obj;
            int dx = (int) map.get("dx");
            int dy = (int) map.get("dy");
            int dz = (int) map.get("dz");
            Material material = Material.valueOf((String) map.get("material"));
            byte data = ((Number) map.get("data")).byteValue();
            result.add(new BlockDef(dx, dy, dz, material, data));
        }
        return result;
    }

    /** Verilen schematic'i konuma (origin hizali) insa eder. Basarisizsa false doner. */
    public boolean build(String name, Location location) {
        List<BlockDef> blocks = load(name);
        if (blocks == null || blocks.isEmpty()) return false;

        World world = location.getWorld();
        if (world == null) return false;

        for (BlockDef def : blocks) {
            Block block = world.getBlockAt(
                    location.getBlockX() + def.dx,
                    location.getBlockY() + def.dy,
                    location.getBlockZ() + def.dz
            );
            block.setType(def.material);
            if (def.data != 0) {
                block.setData(def.data);
            }
        }
        return true;
    }

    /** Insa edilen schematic'i (ayni offsetleri kullanarak) havaya cevirir. */
    public void clear(String name, Location location) {
        List<BlockDef> blocks = load(name);
        if (blocks == null) return;

        World world = location.getWorld();
        if (world == null) return;

        for (BlockDef def : blocks) {
            Block block = world.getBlockAt(
                    location.getBlockX() + def.dx,
                    location.getBlockY() + def.dy,
                    location.getBlockZ() + def.dz
            );
            if (block.getType() == def.material) {
                block.setType(Material.AIR);
            }
        }
    }

    public static class BlockDef {
        public final int dx, dy, dz;
        public final Material material;
        public final byte data;

        public BlockDef(int dx, int dy, int dz, Material material, byte data) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.material = material;
            this.data = data;
        }
    }
}
