package com.weark.itemgenerator.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;

import java.util.UUID;

/**
 * Tek bir Item Generator'un tum verisini tutan model.
 * Gercek bir sandik degil - sanal bir depo (storage) sayaci kullanir.
 */
public class ItemGenerator {

    private final UUID id;
    private Location location;
    private UUID owner;
    private String ownerName;

    private int level;
    private org.bukkit.inventory.ItemStack producedItem;
    private String structureName;
    private float facingYaw;

    private int storage;
    private int maxStorage;
    private int produceAmount;
    private int intervalSeconds;

    private long nextProductionTime;

    // Impulso (boost)
    private long boostExpireTime;
    private double boostMultiplier;

    // Combustivel (fuel) - saniye cinsinden kalan yakit suresi
    private int fuelSeconds;

    // Hologram referanslari (satir basina bir ArmorStand)
    private transient ArmorStand[] hologramLines;

    // Havada donen/parlayan item gorseli (particle efektli) - gorunmez ArmorStand'in elinde
    private transient ArmorStand floatingItemStand;

    public ItemGenerator(UUID id, Location location, UUID owner, String ownerName,
                          int level, org.bukkit.inventory.ItemStack producedItem,
                          int maxStorage, int produceAmount, int intervalSeconds) {
        this.id = id;
        this.location = location;
        this.owner = owner;
        this.ownerName = ownerName;
        this.level = level;
        this.producedItem = producedItem;
        this.maxStorage = maxStorage;
        this.produceAmount = produceAmount;
        this.intervalSeconds = intervalSeconds;
        this.storage = 0;
        this.boostMultiplier = 1.0;
        this.boostExpireTime = 0L;
        this.fuelSeconds = 0;
        this.nextProductionTime = System.currentTimeMillis() + (intervalSeconds * 1000L);
    }

    public boolean isBoostActive() {
        return System.currentTimeMillis() < boostExpireTime;
    }

    public boolean hasFuel() {
        return fuelSeconds > 0;
    }

    public boolean isFull() {
        return storage >= maxStorage;
    }

    public void addStorage(int amount) {
        storage = Math.min(maxStorage, storage + amount);
    }

    public int collect(int amount) {
        int taken = Math.min(amount, storage);
        storage -= taken;
        return taken;
    }

    // ----- Getters / Setters -----

    public UUID getId() { return id; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public org.bukkit.inventory.ItemStack getProducedItem() { return producedItem; }
    public void setProducedItem(org.bukkit.inventory.ItemStack producedItem) { this.producedItem = producedItem; }

    public String getStructureName() { return structureName; }
    public void setStructureName(String structureName) { this.structureName = structureName; }

    public float getFacingYaw() { return facingYaw; }
    public void setFacingYaw(float facingYaw) { this.facingYaw = facingYaw; }

    public int getStorage() { return storage; }
    public void setStorage(int storage) { this.storage = storage; }

    public int getMaxStorage() { return maxStorage; }
    public void setMaxStorage(int maxStorage) { this.maxStorage = maxStorage; }

    public int getProduceAmount() { return produceAmount; }
    public void setProduceAmount(int produceAmount) { this.produceAmount = produceAmount; }

    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public long getNextProductionTime() { return nextProductionTime; }
    public void setNextProductionTime(long nextProductionTime) { this.nextProductionTime = nextProductionTime; }

    public long getBoostExpireTime() { return boostExpireTime; }
    public void setBoostExpireTime(long boostExpireTime) { this.boostExpireTime = boostExpireTime; }

    public double getBoostMultiplier() { return boostMultiplier; }
    public void setBoostMultiplier(double boostMultiplier) { this.boostMultiplier = boostMultiplier; }

    public int getFuelSeconds() { return fuelSeconds; }
    public void setFuelSeconds(int fuelSeconds) { this.fuelSeconds = fuelSeconds; }

    public ArmorStand[] getHologramLines() { return hologramLines; }
    public void setHologramLines(ArmorStand[] hologramLines) { this.hologramLines = hologramLines; }

    public ArmorStand getFloatingItemStand() { return floatingItemStand; }
    public void setFloatingItemStand(ArmorStand floatingItemStand) { this.floatingItemStand = floatingItemStand; }
}
