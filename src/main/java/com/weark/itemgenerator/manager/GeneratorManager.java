package com.weark.itemgenerator.manager;

import com.weark.itemgenerator.ItemGeneratorPlugin;
import com.weark.itemgenerator.model.ItemGenerator;
import com.weark.itemgenerator.util.SchematicManager;
import com.weark.itemgenerator.util.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GeneratorManager {

    private final ItemGeneratorPlugin plugin;
    private final LevelConfig levelConfig = new LevelConfig();
    private final SchematicManager schematicManager;

    private final Map<UUID, ItemGenerator> generators = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public GeneratorManager(ItemGeneratorPlugin plugin) {
        this.plugin = plugin;
        this.schematicManager = new SchematicManager(plugin);
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public void init() {
        plugin.saveDefaultConfig();
        levelConfig.load(plugin.getConfig());

        dataFile = new File(plugin.getDataFolder(), "generators.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("generators.yml olusturulamadi: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        loadAll();
        startProductionTask();
        startHologramTask();
        startParticleTask();
    }

    public LevelConfig getLevelConfig() {
        return levelConfig;
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        levelConfig.load(plugin.getConfig());
        plugin.getMessages().load();
    }

    // ----------------- CRUD -----------------

    public ItemGenerator createGenerator(Location location, UUID owner, String ownerName,
                                          int level, ItemStack item, float facingYaw) {
        LevelConfig.LevelData data = levelConfig.get(level);
        UUID id = UUID.randomUUID();

        ItemStack representative = item.clone();
        representative.setAmount(1);

        ItemGenerator gen = new ItemGenerator(
                id, location, owner, ownerName, level, representative,
                data.maxStorage, data.produceAmount, data.intervalSeconds
        );
        gen.setFacingYaw(facingYaw);

        String structureName = plugin.getConfig().getString("structure", "default");
        gen.setStructureName(structureName);

        generators.put(id, gen);
        spawnHologram(gen);
        spawnFloatingItem(gen);
        if (structureName != null && !structureName.equalsIgnoreCase("none")) {
            boolean built = schematicManager.build(structureName, location);
            if (!built) {
                plugin.getLogger().warning(plugin.getMessages()
                        .getPlain("structure-missing-warning", "{name}", structureName));
            }
        }
        saveGenerator(gen);
        return gen;
    }

    /**
     * Var olan bir generator'un urettigi itemi degistirir (elde tutulan item ile).
     * Meta/durability (ornegin iksir turu) dahil tam olarak korunur.
     */
    public void setGeneratorItem(ItemGenerator gen, ItemStack item) {
        ItemStack representative = item.clone();
        representative.setAmount(1);
        gen.setProducedItem(representative);
        removeFloatingItem(gen);
        spawnFloatingItem(gen);
        saveGenerator(gen);
    }

    public void removeGenerator(ItemGenerator gen) {
        removeHologram(gen);
        removeFloatingItem(gen);
        if (gen.getStructureName() != null && !gen.getStructureName().equalsIgnoreCase("none")) {
            schematicManager.clear(gen.getStructureName(), gen.getLocation());
        }
        generators.remove(gen.getId());
        dataConfig.set(gen.getId().toString(), null);
        saveToDisk();
    }

    public ItemGenerator getGeneratorAt(Location loc) {
        for (ItemGenerator gen : generators.values()) {
            Location genLoc = gen.getLocation();
            if (genLoc.getWorld() == null || loc.getWorld() == null) continue;
            if (!genLoc.getWorld().equals(loc.getWorld())) continue;
            if (genLoc.getBlockX() == loc.getBlockX()
                    && genLoc.getBlockY() == loc.getBlockY()
                    && genLoc.getBlockZ() == loc.getBlockZ()) {
                return gen;
            }
        }
        return null;
    }

    public Collection<ItemGenerator> getAllGenerators() {
        return generators.values();
    }

    public List<ItemGenerator> getGeneratorsOf(UUID owner) {
        List<ItemGenerator> result = new ArrayList<>();
        for (ItemGenerator gen : generators.values()) {
            if (gen.getOwner().equals(owner)) result.add(gen);
        }
        return result;
    }

    // ----------------- Uretim Tick -----------------

    private void startProductionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (ItemGenerator gen : generators.values()) {
                    tickGenerator(gen, now);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // her saniye
    }

    private void tickGenerator(ItemGenerator gen, long now) {
        if (gen.getFuelSeconds() > 0) {
            gen.setFuelSeconds(gen.getFuelSeconds() - 1);
        }
        // Not: fuel = 0 iken uretimin durmasini istersen asagiya "if (!gen.hasFuel()) return;" ekleyebilirsin.

        if (gen.isFull()) return;
        if (now < gen.getNextProductionTime()) return;

        int amount = gen.getProduceAmount();
        if (gen.isBoostActive()) {
            amount = (int) Math.ceil(amount * gen.getBoostMultiplier());
        }

        gen.addStorage(amount);

        double effectiveInterval = gen.getIntervalSeconds();
        if (gen.isBoostActive()) {
            effectiveInterval = effectiveInterval / gen.getBoostMultiplier();
        }
        gen.setNextProductionTime(now + (long) (effectiveInterval * 1000L));
    }

    // ----------------- Yuzen Item (ArmorStand tabanli) -----------------

    private void spawnFloatingItem(ItemGenerator gen) {
        World world = gen.getLocation().getWorld();
        if (world == null) return;

        Location loc = gen.getLocation().clone().add(0.5, 1.3, 0.5);

        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setBasePlate(false);
        stand.setArms(true);
        stand.setCustomNameVisible(false);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        stand.setItemInHand(gen.getProducedItem().clone());

        gen.setFloatingItemStand(stand);
    }

    private void removeFloatingItem(ItemGenerator gen) {
        ArmorStand stand = gen.getFloatingItemStand();
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
        gen.setFloatingItemStand(null);
    }

    /**
     * Yuzen itemin hafifce yukari-asagi salinmasi (bobbing), yavas donmesi
     * ve yesil sparkle (HAPPY_VILLAGER) particle efekti uretmesini saglar.
     */
    private void startParticleTask() {
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                tick++;
                for (ItemGenerator gen : generators.values()) {
                    ArmorStand stand = gen.getFloatingItemStand();
                    if (stand == null || stand.isDead()) {
                        spawnFloatingItem(gen);
                        stand = gen.getFloatingItemStand();
                        if (stand == null) continue;
                    }

                    Location base = gen.getLocation().clone().add(0.5, 1.3, 0.5);
                    double bob = Math.sin((tick % 40) / 40.0 * Math.PI * 2) * 0.08;
                    Location current = base.clone().add(0, bob, 0);

                    // Yavas donus: yaw'i her tick'te biraz arttir
                    float yaw = (tick * 4f) % 360f;
                    current.setYaw(yaw);
                    stand.teleport(current);
                    stand.setVelocity(new Vector(0, 0, 0));
                    stand.setFallDistance(0f);

                    World world = current.getWorld();
                    if (world != null) {
                        Location particleLoc = current.clone().add(0, 0.15, 0);
                        world.playEffect(particleLoc, Effect.HAPPY_VILLAGER, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 4L); // ~0.2 saniyede bir
    }

    // ----------------- Hologram -----------------

    private void startHologramTask() {
        int interval = plugin.getConfig().getInt("hologram-update-interval", 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ItemGenerator gen : generators.values()) {
                    updateHologram(gen);
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void spawnHologram(ItemGenerator gen) {
        World world = gen.getLocation().getWorld();
        if (world == null) return;

        Location base = computeHologramBase(gen);
        String[] lines = buildHologramLines(gen);

        ArmorStand[] stands = new ArmorStand[lines.length];
        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, (lines.length - i) * 0.25, 0);
            ArmorStand stand = (ArmorStand) world.spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(lines[i]);
            stand.setMarker(true);
            stand.setSmall(true);
            stands[i] = stand;
        }
        gen.setHologramLines(stands);
    }

    /**
     * Hologram'in baslangic konumunu hesaplar: yerlestirme aninda bakilan yone gore
     * generator'un 4 blok ONUNDE ve item'in bulundugu yukseklikten biraz DAHA ASAGIDA.
     * Boylece oyuncunun kendi tasarladigi (schematic) yapiyla cakismaz.
     */
    private Location computeHologramBase(ItemGenerator gen) {
        double rad = Math.toRadians(gen.getFacingYaw());
        double dx = -Math.sin(rad) * 4.0;
        double dz = Math.cos(rad) * 4.0;
        return gen.getLocation().clone().add(0.5 + dx, 0.9, 0.5 + dz);
    }

    private void updateHologram(ItemGenerator gen) {
        ArmorStand[] stands = gen.getHologramLines();
        String[] lines = buildHologramLines(gen);

        if (stands == null || stands.length != lines.length) {
            removeHologram(gen);
            spawnHologram(gen);
            return;
        }

        for (int i = 0; i < stands.length; i++) {
            if (stands[i] == null || stands[i].isDead()) {
                removeHologram(gen);
                spawnHologram(gen);
                return;
            }
            stands[i].setCustomName(lines[i]);
        }
    }

    private void removeHologram(ItemGenerator gen) {
        ArmorStand[] stands = gen.getHologramLines();
        if (stands == null) return;
        for (ArmorStand stand : stands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        gen.setHologramLines(null);
    }

    private String[] buildHologramLines(ItemGenerator gen) {
        MessageManager msg = plugin.getMessages();

        long remainingMs = gen.getNextProductionTime() - System.currentTimeMillis();
        if (remainingMs < 0) remainingMs = 0;
        String timeLeft = formatDuration(remainingMs / 1000);

        int filled = (int) (((double) gen.getStorage() / gen.getMaxStorage()) * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? ChatColor.GREEN : ChatColor.RED).append("\u25A0");
        }

        String boostText = gen.isBoostActive()
                ? formatDuration((gen.getBoostExpireTime() - System.currentTimeMillis()) / 1000)
                : msg.getPlain("hologram-boost-none");

        String fuelText = gen.getFuelSeconds() > 0
                ? formatDuration(gen.getFuelSeconds())
                : "0s";

        return new String[]{
                ChatColor.WHITE + gen.getOwnerName(),
                msg.getPlain("hologram-farm-title", "{level}", String.valueOf(gen.getLevel())),
                bar.toString() + ChatColor.GRAY + " - " + timeLeft,
                msg.getPlain("hologram-boost-label", "{value}", boostText),
                msg.getPlain("hologram-fuel-label", "{value}", fuelText),
                msg.getPlain("hologram-chest-label",
                        "{storage}", String.valueOf(gen.getStorage()),
                        "{max}", String.valueOf(gen.getMaxStorage())),
                msg.getPlain("hologram-click-to-open")
        };
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    // ----------------- Persistence -----------------

    public void saveGenerator(ItemGenerator gen) {
        String path = gen.getId().toString();
        dataConfig.set(path + ".world", gen.getLocation().getWorld().getName());
        dataConfig.set(path + ".x", gen.getLocation().getBlockX());
        dataConfig.set(path + ".y", gen.getLocation().getBlockY());
        dataConfig.set(path + ".z", gen.getLocation().getBlockZ());
        dataConfig.set(path + ".owner", gen.getOwner().toString());
        dataConfig.set(path + ".owner-name", gen.getOwnerName());
        dataConfig.set(path + ".level", gen.getLevel());
        dataConfig.set(path + ".structure", gen.getStructureName());
        dataConfig.set(path + ".facing-yaw", gen.getFacingYaw());
        // Tam ItemStack (meta/durability dahil) - Bukkit'in ConfigurationSerializable altyapisi kullanilir.
        dataConfig.set(path + ".item", gen.getProducedItem());
        dataConfig.set(path + ".storage", gen.getStorage());
        dataConfig.set(path + ".max-storage", gen.getMaxStorage());
        dataConfig.set(path + ".produce-amount", gen.getProduceAmount());
        dataConfig.set(path + ".interval-seconds", gen.getIntervalSeconds());
        dataConfig.set(path + ".fuel-seconds", gen.getFuelSeconds());
        dataConfig.set(path + ".boost-expire", gen.getBoostExpireTime());
        saveToDisk();
    }

    public void saveAll() {
        for (ItemGenerator gen : generators.values()) {
            saveGenerator(gen);
        }
    }

    private void saveToDisk() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("generators.yml kaydedilemedi: " + e.getMessage());
        }
    }

    private void loadAll() {
        generators.clear();
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String worldName = dataConfig.getString(key + ".world");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) continue;

                int x = dataConfig.getInt(key + ".x");
                int y = dataConfig.getInt(key + ".y");
                int z = dataConfig.getInt(key + ".z");
                Location loc = new Location(world, x, y, z);

                UUID owner = UUID.fromString(dataConfig.getString(key + ".owner"));
                String ownerName = dataConfig.getString(key + ".owner-name", "Unknown");
                int level = dataConfig.getInt(key + ".level", 1);

                ItemStack item = dataConfig.getItemStack(key + ".item");
                if (item == null) {
                    // Eski (Material-only) formatla kaydedilmis veriler icin geriye donuk uyum
                    String materialName = dataConfig.getString(key + ".material", "DIAMOND");
                    item = new ItemStack(org.bukkit.Material.valueOf(materialName), 1);
                }

                int maxStorage = dataConfig.getInt(key + ".max-storage", 128);
                int produceAmount = dataConfig.getInt(key + ".produce-amount", 1);
                int interval = dataConfig.getInt(key + ".interval-seconds", 5);

                ItemGenerator gen = new ItemGenerator(id, loc, owner, ownerName, level, item,
                        maxStorage, produceAmount, interval);
                gen.setStructureName(dataConfig.getString(key + ".structure", "default"));
                gen.setFacingYaw((float) dataConfig.getDouble(key + ".facing-yaw", 0.0));
                gen.setStorage(dataConfig.getInt(key + ".storage", 0));
                gen.setFuelSeconds(dataConfig.getInt(key + ".fuel-seconds", 0));
                gen.setBoostExpireTime(dataConfig.getLong(key + ".boost-expire", 0L));

                generators.put(id, gen);
                spawnHologram(gen);
                spawnFloatingItem(gen);
            } catch (Exception e) {
                plugin.getLogger().warning("Generator yuklenemedi (" + key + "): " + e.getMessage());
            }
        }
        plugin.getLogger().info(generators.size() + " generator yuklendi.");
    }

    public void shutdown() {
        saveAll();
        for (ItemGenerator gen : generators.values()) {
            removeHologram(gen);
            removeFloatingItem(gen);
        }
    }
}
