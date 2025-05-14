package id.nusafishing.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.model.CustomFish;
import id.nusafishing.fishing.model.FishingBait;
import id.nusafishing.fishing.model.FishingRod;

public class ConfigManager {
    private final NusaFishing plugin;
    private FileConfiguration fishingConfig;
    private File fishingConfigFile;
    
    private Map<String, FishingRod> fishingRods;
    private Map<String, FishingBait> fishingBaits;
    private Map<String, CustomFish> customFishes;
    
    public ConfigManager(NusaFishing plugin) {
        this.plugin = plugin;
        this.fishingRods = new HashMap<>();
        this.fishingBaits = new HashMap<>();
        this.customFishes = new HashMap<>();
        
        // Initialize configs
        createFishingConfig();
        loadFishingConfig();
    }
    
    private void createFishingConfig() {
        fishingConfigFile = new File(plugin.getDataFolder(), "fishing.yml");
        if (!fishingConfigFile.exists()) {
            fishingConfigFile.getParentFile().mkdirs();
            plugin.saveResource("fishing.yml", false);
        }
        
        fishingConfig = YamlConfiguration.loadConfiguration(fishingConfigFile);
    }
    
    public void loadFishingConfig() {
        // Load fishing rods
        ConfigurationSection rodsSection = fishingConfig.getConfigurationSection("fishing_rods");
        if (rodsSection != null) {
            for (String key : rodsSection.getKeys(false)) {
                String name = rodsSection.getString(key + ".name", key);
                String description = rodsSection.getString(key + ".description", "");
                int customModelData = rodsSection.getInt(key + ".custom_model_data", 0);
                int durability = rodsSection.getInt(key + ".durability", 100);
                double staminaModifier = rodsSection.getDouble(key + ".stamina_modifier", 1.0);
                
                // Baca tier dan special ability
                String tierName = rodsSection.getString(key + ".tier", "BASIC");
                FishingRod.RodTier tier;
                try {
                    tier = FishingRod.RodTier.valueOf(tierName);
                } catch (IllegalArgumentException e) {
                    tier = FishingRod.RodTier.BASIC;
                }
                
                boolean hasSpecialAbility = rodsSection.getBoolean(key + ".special_ability", false);
                String abilityDescription = rodsSection.getString(key + ".ability_description", "");
                
                // Baca price dan lure_level
                double price = rodsSection.getDouble(key + ".price", -1); // Default -1 berarti gunakan kalkulasi
                int lureLevel = rodsSection.getInt(key + ".lure_level", tier.ordinal() + 1); // Default berdasarkan tier
                
                // Baca max_weight_capacity (kapasitas berat ikan maksimal)
                double maxWeightCapacity = rodsSection.getDouble(key + ".max_weight_capacity", 
                        switch(tier) {
                            case BASIC -> 25.0;
                            case INTERMEDIATE -> 100.0;
                            case ADVANCED -> 500.0;
                            case EXPERT -> 2500.0;
                            case MASTER -> 10000.0;
                            default -> 25.0;
                        });
                
                FishingRod rod = new FishingRod(key, name, description, customModelData, 
                                              durability, staminaModifier, tier,
                                              hasSpecialAbility, abilityDescription,
                                              price, lureLevel, maxWeightCapacity);
                fishingRods.put(key, rod);
            }
        }
        
        // Load fishing baits
        ConfigurationSection baitsSection = fishingConfig.getConfigurationSection("fishing_baits");
        if (baitsSection != null) {
            for (String key : baitsSection.getKeys(false)) {
                String name = baitsSection.getString(key + ".name", key);
                String description = baitsSection.getString(key + ".description", "");
                int customModelData = baitsSection.getInt(key + ".custom_model_data", 0);
                Material material = Material.getMaterial(baitsSection.getString(key + ".material", "COD"));
                List<String> validFish = baitsSection.getStringList(key + ".valid_fish");
                double attractionModifier = baitsSection.getDouble(key + ".attraction_modifier", 1.0);
                
                int tierBoost = baitsSection.getInt(key + ".tier_boost", 0);
                int usesPerItem = baitsSection.getInt(key + ".uses_per_item", 1);
                double price = baitsSection.getDouble(key + ".price", -1);
                
                // Baca nilai boosts baru dengan default 1.0 (tidak ada boost)
                double qualityBoost = baitsSection.getDouble(key + ".quality_boost", 1.0);
                double weightBoost = baitsSection.getDouble(key + ".weight_boost", 1.0);
                
                FishingBait bait = new FishingBait(key, name, description, material, 
                                                 customModelData, validFish, attractionModifier,
                                                 tierBoost, usesPerItem, price,
                                                 qualityBoost, weightBoost);
                fishingBaits.put(key, bait);
            }
        }
        
        // Load custom fish
        ConfigurationSection fishesSection = fishingConfig.getConfigurationSection("fishes");
        if (fishesSection != null) {
            for (String key : fishesSection.getKeys(false)) {
                String name = fishesSection.getString(key + ".name", key);
                String description = fishesSection.getString(key + ".description", "");
                Material material = Material.getMaterial(fishesSection.getString(key + ".material", "COD"));
                int customModelData = fishesSection.getInt(key + ".custom_model_data", 0);
                double minWeight = fishesSection.getDouble(key + ".min_weight", 0.1);
                double maxWeight = fishesSection.getDouble(key + ".max_weight", 1.0);
                int minQuality = fishesSection.getInt(key + ".min_quality", 1);
                int maxQuality = fishesSection.getInt(key + ".max_quality", 100);
                double basePrice = fishesSection.getDouble(key + ".base_price", 10.0);
                
                // Baca tier ikan
                String tierName = fishesSection.getString(key + ".tier", "COMMON");
                CustomFish.FishTier tier;
                try {
                    tier = CustomFish.FishTier.valueOf(tierName);
                } catch (IllegalArgumentException e) {
                    tier = CustomFish.FishTier.COMMON;
                }
                
                CustomFish fish = new CustomFish(key, name, description, material, customModelData,
                                                minWeight, maxWeight, minQuality, maxQuality, 
                                                basePrice, tier);
                customFishes.put(key, fish);
            }
        }
    }
    
    public void saveConfig() {
        try {
            fishingConfig.save(fishingConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save fishing.yml: " + e.getMessage());
        }
    }
    
    public void reloadConfigs() {
        // Reload plugin config
        plugin.reloadConfig();
        
        // Reload fishing config
        reloadFishingConfig();
        
        // Reload the rewards config
        plugin.getRewardsManager().reloadRewardsConfig();
        
        // Log message
        plugin.getLogger().info("Semua konfigurasi berhasil dimuat ulang!");
    }

    public void reloadFishingConfig() {
        createFishingConfig();
        loadFishingConfig();
    }
    
    public FileConfiguration getFishingConfig() {
        return fishingConfig;
    }
    
    public Map<String, FishingRod> getFishingRods() {
        return fishingRods;
    }
    
    public Map<String, FishingBait> getFishingBaits() {
        return fishingBaits;
    }
    
    public Map<String, CustomFish> getCustomFishes() {
        return customFishes;
    }
}