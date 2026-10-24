package id.nusafishing.fishing.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import id.nusafishing.NusaFishing;
import id.nusafishing.compat.CompatibilityHelper;
import id.nusafishing.compat.CustomModelDataHelper;

public class FishingRod {
    // Definisikan tier pancingan
    public enum RodTier {
        BASIC("§7", 1.0, 0, "Dasar"),
        INTERMEDIATE("§a", 1.2, 1, "Menengah"),
        ADVANCED("§9", 1.5, 2, "Mahir"),
        EXPERT("§5", 2.0, 3, "Expert"),
        MASTER("§6", 3.0, 4, "Master"),
        MYTHIC("§c", 4.0, 5, "Mistik");
        
        private final String defaultColor;
        private final double catchRateMultiplier;
        private final int tierBoost;
        private final String displayName;
        
        RodTier(String defaultColor, double catchRateMultiplier, int tierBoost, String displayName) {
            this.defaultColor = defaultColor;
            this.catchRateMultiplier = catchRateMultiplier;
            this.tierBoost = tierBoost;
            this.displayName = displayName;
        }
        
        public String getDefaultColor() { return defaultColor; }

        public String getColor() {
            if (NusaFishing.getInstance() != null && NusaFishing.getInstance().getConfigManager() != null) {
                return NusaFishing.getInstance().getConfigManager().getRodTierColor(this);
            }
            return defaultColor;
        }

        public double getCatchRateMultiplier() { return catchRateMultiplier; }
        public int getTierBoost() { return tierBoost; }
        public String getDisplayName() { return displayName; }
    }
    
    private String id;
    private String name;
    private String description;
    private Material material;
    private int customModelData;
    private String textureProvider; // "vanilla", "nexo", "oraxen", "itemsadder"
    private String textureId;       // Item ID in texture provider
    private int durability;
    private double staminaModifier;
    private RodTier tier;
    private boolean hasSpecialAbility;
    private String specialAbilityDescription;
    private double price; // Harga kustom
    private int lureLevel; // Level enchant Lure
    private double maxWeightCapacity; // Kapasitas berat maksimal ikan yang bisa ditangkap
    
    public FishingRod(String id, String name, String description, int customModelData, 
                     int durability, double staminaModifier, RodTier tier, 
                     boolean hasSpecialAbility, String specialAbilityDescription,
                     double price, int lureLevel, double maxWeightCapacity) {
        this(id, name, description, Material.FISHING_ROD, customModelData, "vanilla", null,
             durability, staminaModifier, tier, hasSpecialAbility, specialAbilityDescription,
             price, lureLevel, maxWeightCapacity);
    }

    public FishingRod(String id, String name, String description, Material material,
                     int customModelData, String textureProvider, String textureId,
                     int durability, double staminaModifier, RodTier tier, 
                     boolean hasSpecialAbility, String specialAbilityDescription,
                     double price, int lureLevel, double maxWeightCapacity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.material = material != null ? material : Material.FISHING_ROD;
        this.customModelData = customModelData;
        this.textureProvider = (textureProvider == null || textureProvider.isBlank()) ? "vanilla" : textureProvider.trim().toLowerCase();
        this.textureId = textureId;
        this.durability = durability;
        this.staminaModifier = staminaModifier;
        this.tier = tier == null ? RodTier.BASIC : tier;
        this.hasSpecialAbility = hasSpecialAbility;
        this.specialAbilityDescription = specialAbilityDescription;
        this.price = price;
        this.lureLevel = lureLevel;
        this.maxWeightCapacity = maxWeightCapacity;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public String getTextureProvider() { return textureProvider; }
    public String getTextureId() { return textureId; }
    public int getDurability() { return durability; }
    public double getStaminaModifier() { return staminaModifier; }
    public RodTier getTier() { return tier; }
    public boolean hasSpecialAbility() { return hasSpecialAbility; }
    public String getSpecialAbilityDescription() { return specialAbilityDescription; }
    public double getPrice() { return price; }
    public int getLureLevel() { return lureLevel; }
    public double getMaxWeightCapacity() { return maxWeightCapacity; }
    
    // Quality boost based on rod tier (affects fish quality)
    public double getQualityBoost() {
        return 1.0 + ((tier.ordinal()) * 0.1);  // 0%, 10%, 20%, 30%, 40%
    }

    /**
     * Creates an active fishing rod item stack with full durability.
     *
     * @return Fully configured ItemStack
     */
    public ItemStack createItemStack() {
        return createItemStack(this.durability);
    }

    /**
     * Creates an active fishing rod item stack with specific durability.
     *
     * @param currentDurability Current durability value
     * @return Fully configured ItemStack
     */
    public ItemStack createItemStack(int currentDurability) {
        ItemStack rodItem;
        if (NusaFishing.getInstance() != null && NusaFishing.getInstance().getItemProviderManager() != null) {
            rodItem = NusaFishing.getInstance().getItemProviderManager()
                    .resolveBaseItem(textureProvider, textureId, material, customModelData);
        } else {
            rodItem = new ItemStack(material != null ? material : Material.FISHING_ROD);
            ItemMeta meta = rodItem.getItemMeta();
            if (meta != null && customModelData > 0) {
                CustomModelDataHelper.applyCustomModelData(meta, customModelData);
                rodItem.setItemMeta(meta);
            }
        }

        ItemMeta meta = rodItem.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(rodItem.getType());
        }

        meta.setDisplayName(tier.getColor() + name + " §7[" + tier.getDisplayName() + "]");
        
        List<String> lore = new ArrayList<>();
        lore.add("§8" + description);
        lore.add("");
        lore.add("§7Ketahanan: §f" + currentDurability + "/" + durability);
        lore.add("§7Stamina: §f" + (staminaModifier < 1.0 ? 
                "-" + (int)((1.0 - staminaModifier) * 100) + "%" : 
                "+" + (int)((staminaModifier - 1.0) * 100) + "%"));
        lore.add("§7Tier: " + tier.getColor() + tier.getDisplayName());
        lore.add("§7Quality Boost: §f+" + (int)((getQualityBoost() - 1.0) * 100) + "%");
        lore.add("§7Lure: §f" + lureLevel);
        lore.add("§7Kapasitas Berat: §f" + formatWeight(maxWeightCapacity));
        lore.add("");
        if (hasSpecialAbility && specialAbilityDescription != null && !specialAbilityDescription.isBlank()) {
            lore.add("§e✦ " + specialAbilityDescription);
            lore.add("");
        }
        lore.add("§8ID: " + id);
        meta.setLore(lore);

        if (CompatibilityHelper.getLuckEnchantment() != null) {
            meta.addEnchant(CompatibilityHelper.getLuckEnchantment(), 1, true);
        }
        if (lureLevel > 0) {
            meta.addEnchant(Enchantment.LURE, lureLevel, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        // Persistent data container
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(NamespacedKey.fromString("nusafishing:rod_id"), PersistentDataType.STRING, id);
        container.set(NamespacedKey.fromString("nusafishing:durability"), PersistentDataType.INTEGER, currentDurability);
        container.set(NamespacedKey.fromString("nusafishing:nusafishing.rod_id"), PersistentDataType.STRING, id);
        container.set(NamespacedKey.fromString("nusafishing:nusafishing.durability"), PersistentDataType.INTEGER, currentDurability);

        rodItem.setItemMeta(meta);
        return rodItem;
    }

    /**
     * Creates a display item for the Shop GUI.
     *
     * @param shopPrice Price displayed in lore
     * @param canAfford Whether player can afford the rod
     * @return Formatted ItemStack for GUI
     */
    public ItemStack createDisplayItem(double shopPrice, boolean canAfford) {
        ItemStack displayItem;
        if (NusaFishing.getInstance() != null && NusaFishing.getInstance().getItemProviderManager() != null) {
            displayItem = NusaFishing.getInstance().getItemProviderManager()
                    .resolveBaseItem(textureProvider, textureId, material, customModelData);
        } else {
            displayItem = new ItemStack(material != null ? material : Material.FISHING_ROD);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null && customModelData > 0) {
                CustomModelDataHelper.applyCustomModelData(meta, customModelData);
                displayItem.setItemMeta(meta);
            }
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
        }

        meta.setDisplayName("§b" + name);
        List<String> lore = new ArrayList<>();
        lore.add("§8" + description);
        lore.add("");
        lore.add("§7Ketahanan: §f" + durability);
        lore.add("§7Stamina: §f" + (staminaModifier < 1.0 ? 
                "-" + (int)((1.0 - staminaModifier) * 100) + "%" : 
                "+" + (int)((staminaModifier - 1.0) * 100) + "%"));
        lore.add("");
        lore.add("§7Harga: §f" + String.format("%.1f", shopPrice));
        lore.add("");
        lore.add(canAfford ? "§a► Klik untuk membeli" : "§c✘ Uang tidak cukup");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private String formatWeight(double weight) {
        if (weight < 1.0) {
            return String.format("%.0f g", weight * 1000);
        } else if (weight < 1000.0) {
            return String.format("%.1f kg", weight);
        } else {
            return String.format("%.1f ton", weight / 1000.0);
        }
    }
}
