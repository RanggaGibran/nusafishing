package id.nusafishing.fishing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.FishHook;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import id.nusafishing.NusaFishing;
import id.nusafishing.config.ConfigManager;
import id.nusafishing.fishing.data.PlayerSkillManager;
import id.nusafishing.fishing.model.CustomFish;
import id.nusafishing.fishing.model.FishingBait;
import id.nusafishing.fishing.model.FishingRod;
import id.nusafishing.event.FishingEventManager;
import id.nusafishing.event.EventFish;

public class FishingManager {
    private final NusaFishing plugin;
    private final ConfigManager configManager;
    private final Random random = new Random();
    
    // Data for special fishing locations
    private static class SpecialLocation {
        String id;
        String name;
        String description;
        double centerX;
        double centerZ;
        double radius;
        double fishMultiplier;
        double rareFishChance;
        List<String> specialFish;
        
        public boolean isInLocation(Location location) {
            double dx = location.getX() - centerX;
            double dz = location.getZ() - centerZ;
            return (dx * dx + dz * dz) <= (radius * radius);
        }
    }
    
    private final Map<String, SpecialLocation> specialLocations = new HashMap<>();
    
    public FishingManager(NusaFishing plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        
        // Load special locations from config
        loadSpecialLocations();
    }
    
    /**
     * Load special locations from the configuration
     */
    private void loadSpecialLocations() {
        specialLocations.clear();
        
        ConfigurationSection locationsSection = plugin.getConfigManager().getFishingConfig()
                .getConfigurationSection("special_locations");
        
        if (locationsSection == null) {
            return;
        }
        
        for (String key : locationsSection.getKeys(false)) {
            SpecialLocation location = new SpecialLocation();
            location.id = key;
            location.name = locationsSection.getString(key + ".name", key);
            location.description = locationsSection.getString(key + ".description", "");
            location.centerX = locationsSection.getDouble(key + ".center_x");
            location.centerZ = locationsSection.getDouble(key + ".center_z");
            location.radius = locationsSection.getDouble(key + ".radius", 50.0);
            location.fishMultiplier = locationsSection.getDouble(key + ".fish_multiplier", 1.0);
            location.rareFishChance = locationsSection.getDouble(key + ".rare_fish_chance", 0.1);
            location.specialFish = locationsSection.getStringList(key + ".special_fish");
            
            specialLocations.put(key, location);
        }
        
        plugin.getLogger().info("Loaded " + specialLocations.size() + " special fishing locations");
    }
    
    /**
     * Get a fishing rod by its custom model data
     * @param modelData The custom model data to look for
     * @return The matching FishingRod, or null if none found
     */
    public FishingRod getFishingRodByModelData(int modelData) {
        for (FishingRod rod : configManager.getFishingRods().values()) {
            if (rod.getCustomModelData() == modelData) {
                return rod;
            }
        }
        return null;
    }
    
    /**
     * Get a fishing rod by its ID
     * @param rodId The rod ID to look for
     * @return The matching FishingRod, or null if none found
     */
    public FishingRod getRodById(String rodId) {
        return configManager.getFishingRods().get(rodId);
    }
    
    /**
     * Get a bait by its custom model data
     * @param item The item to check for bait properties
     * @return The matching FishingBait, or null if not a bait
     */
    public FishingBait getBaitFromItem(ItemStack item) {
        if (item == null || item.getType() == null) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            return null;
        }
        
        int modelData = meta.getCustomModelData();
        
        for (FishingBait bait : configManager.getFishingBaits().values()) {
            if (bait.getMaterial() == item.getType() && 
                bait.getCustomModelData() == modelData) {
                return bait;
            }
        }
        
        return null;
    }
    
    /**
     * Find the special location a player is currently in, if any
     * @param player The player to check
     * @return The special location, or null if not in any
     */
    public SpecialLocation getPlayerSpecialLocation(Player player) {
        Location location = player.getLocation();
        
        for (SpecialLocation specialLocation : specialLocations.values()) {
            if (specialLocation.isInLocation(location)) {
                return specialLocation;
            }
        }
        
        return null;
    }
    
    /**
     * Check if the player is in a special fishing location
     * @param player The player to check
     * @return The name of the special location, or null if not in one
     */
    public String getSpecialLocationName(Player player) {
        SpecialLocation location = getPlayerSpecialLocation(player);
        return location != null ? location.name : null;
    }
    
    /**
     * Check if an event fish can be caught and return it
     * @param player The player fishing
     * @param location The fishing location
     * @return The event fish if one can be caught, otherwise null
     */
    public CustomFish checkForEventFish(Player player, Location location) {
        // Get the event manager
        FishingEventManager eventManager = plugin.getEventManager();
        
        // Check if an event is active and this location is valid
        EventFish eventFish = eventManager.canCatchEventFish(player, location);
        
        if (eventFish != null) {
            // Create the custom fish instance
            return eventFish.createCustomFish();
        }
        
        return null;
    }
    
    /**
     * Select a random fish based on player's location, time, weather, etc.
     * @param player The player fishing
     * @param rod The fishing rod being used, can be null for default rod
     * @return A randomly selected fish suitable for the conditions, or null if no fish can be caught
     */
    public CustomFish selectRandomFish(Player player, FishingRod rod) {
        // Check for event fish first (highest priority)
        CustomFish eventFish = checkForEventFish(player, player.getLocation());
        if (eventFish != null) {
            return eventFish;
        }
        
        Location location = player.getLocation();
        World world = location.getWorld();
        Biome biome = location.getBlock().getBiome();
        
        // Determine time of day and weather
        String timeOfDay = getTimeOfDay(world.getTime());
        String weather = getWeatherCondition(world);
        
        // Check for player's bait
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        FishingBait bait = getBaitFromItem(offHandItem);
        
        // Get player's skill level modifier
        PlayerSkillManager skillManager = plugin.getSkillManager();
        double skillQualityModifier = skillManager.getQualityModifier(player);
        double skillSuccessChance = skillManager.getFishingSuccessChance(player);
        
        // Get the special location if player is in one
        SpecialLocation specialLocation = getPlayerSpecialLocation(player);
        
        // Prepare weighted selection for fish
        List<CustomFish> availableFish = new ArrayList<>();
        List<Double> fishWeights = new ArrayList<>();
        double totalWeight = 0;
        
        Map<String, CustomFish> fishMap = configManager.getCustomFishes();
        
        // Calculate tier boost from rod and bait
        int totalTierBoost = 0;
        double qualityBoost = 1.0;
        double weightBoost = 1.0;  // Default: tidak ada boost berat

        if (rod != null) {
            totalTierBoost += rod.getTier().getTierBoost();
            qualityBoost *= rod.getQualityBoost();
        }

        if (bait != null) {
            totalTierBoost += bait.getTierBoost();
            qualityBoost *= bait.getQualityBoost();  // Terapkan boost kualitas dari umpan
            weightBoost *= bait.getWeightBoost();    // Terapkan boost berat dari umpan
        }

        // Apply skill quality boost
        qualityBoost *= skillQualityModifier;

        // Check for special fish from special locations first
        if (specialLocation != null && !specialLocation.specialFish.isEmpty() && 
            random.nextDouble() < specialLocation.rareFishChance) {
            
            // Try to catch a special fish
            String specialFishId = specialLocation.specialFish.get(
                random.nextInt(specialLocation.specialFish.size()));
            
            CustomFish specialFish = fishMap.get(specialFishId);
            if (specialFish != null) {
                // Fix: Pass the required parameters to createCaughtInstance
                CustomFish caughtFish = specialFish.createCaughtInstance(totalTierBoost, qualityBoost, weightBoost);
                
                // Apply additional bonuses for special fish
                // Location fish should be better quality due to rarity
                return caughtFish;
            }
        }
        
        // Filter and weigh normal fish based on conditions
        for (CustomFish fish : fishMap.values()) {
            // Skip if using bait that can't catch this fish
            if (bait != null && !bait.canCatchFish(fish.getId())) {
                continue;
            }
            
            // Calculate base weight for this fish
            double fishWeight = calculateFishWeight(fish, bait, specialLocation);
            
            // Add to selection pool
            availableFish.add(fish);
            fishWeights.add(fishWeight);
            totalWeight += fishWeight;
        }
        
        // Perform weighted random selection
        if (totalWeight > 0) {
            double randomValue = random.nextDouble() * totalWeight;
            double weightSum = 0;
            
            for (int i = 0; i < availableFish.size(); i++) {
                weightSum += fishWeights.get(i);
                if (randomValue < weightSum) {
                    // Consume one use of bait if available
                    if (bait != null) {
                        bait.consumeUse(player);
                        
                        // Show particle effect to indicate bait use
                        player.spawnParticle(Particle.WATER_BUBBLE, 
                                            player.getLocation().add(0, 0.5, 0), 
                                            10, 0.2, 0.2, 0.2, 0.1);
                    }
                    
                    // Fix the null check before accessing bait.getValidFish()
                    if (bait != null && !bait.getValidFish().isEmpty() && bait.getValidFish().contains(availableFish.get(i).getId())) {
                        qualityBoost *= 1.2;  // 20% bonus kualitas untuk ikan yang cocok
                        weightBoost *= 1.3;   // 30% bonus berat untuk ikan yang cocok
                    }

                    // Create instance of the selected fish with all boosts
                    double maxPossibleWeight = rod != null ? rod.getMaxWeightCapacity() : 100.0; // Default 100kg jika tidak ada rod
                    return availableFish.get(i).createCaughtInstance(totalTierBoost, qualityBoost, weightBoost, maxPossibleWeight);
                }
            }
        }
        
        // No fish available
        return null;
    }
    
    /**
     * Calculate weight (selection probability) for a fish based on various factors
     */
    private double calculateFishWeight(CustomFish fish, FishingBait bait, SpecialLocation location) {
        // Base weight calculation - make rarer fish MUCH harder to catch
        // Ikan dengan tier tinggi lebih sulit ditangkap secara eksponensial
        double baseWeight;
        switch (fish.getBaseTier()) {
            case LEGENDARY:
                baseWeight = 10.0 / (1 + fish.getBasePrice() * 2); // 10x harder than regular formula
                break;
            case EPIC:
                baseWeight = 25.0 / (1 + fish.getBasePrice() * 1.5); // 4x harder
                break; 
            case RARE:
                baseWeight = 50.0 / (1 + fish.getBasePrice() * 1.2); // 2x harder
                break;
            default:
                baseWeight = 100.0 / (1 + fish.getBasePrice()); // Original formula
        }
        
        // Apply bait attraction modifier with less impact for higher tiers
        double baitModifier = 1.0;
        if (bait != null) {
            // Apply attraction modifier based on tier
            switch (fish.getBaseTier()) {
                case LEGENDARY:
                    // Bait only 25% as effective for legendary
                    baitModifier = 1.0 + ((bait.getAttractionModifier() - 1.0) * 0.25);
                    break;
                case EPIC:
                    // Bait only 50% as effective for epic
                    baitModifier = 1.0 + ((bait.getAttractionModifier() - 1.0) * 0.5);
                    break;
                case RARE:
                    // Bait 75% as effective for rare
                    baitModifier = 1.0 + ((bait.getAttractionModifier() - 1.0) * 0.75);
                    break;
                default:
                    // Full effectiveness for common/uncommon
                    baitModifier = bait.getAttractionModifier();
            }
            
            // Apply bonus for matching fish, but with diminishing returns by tier
            if (!bait.getValidFish().isEmpty() && bait.getValidFish().contains(fish.getId())) {
                switch (fish.getBaseTier()) {
                    case LEGENDARY:
                        baitModifier *= 3.0;  // 3x bonus (instead of 10x)
                        break;
                    case EPIC:
                        baitModifier *= 5.0;  // 5x bonus
                        break;
                    case RARE:
                        baitModifier *= 7.0;  // 7x bonus
                        break;
                    default:
                        baitModifier *= 10.0; // Original 10x bonus
                }
            }
        }
        
        // Location modifier remains the same
        double locationModifier = (location != null) ? location.fishMultiplier : 1.0;
        
        // Add time-based factor - legendary fish more common at night/dawn
        // Tambahkan faktor ini jika ingin menambah variasi
        
        return baseWeight * baitModifier * locationModifier;
    }
    
    /**
     * Get the time of day as a string (DAY, SUNSET, NIGHT, SUNRISE)
     * @param time The tick time of the world
     * @return A string representing the time of day
     */
    private String getTimeOfDay(long time) {
        // Minecraft day is 24000 ticks long
        // Day: 0-12000, Sunset: 12000-14000, Night: 14000-22000, Sunrise: 22000-24000
        if (time < 12000) {
            return "DAY";
        } else if (time < 14000) {
            return "SUNSET";
        } else if (time < 22000) {
            return "NIGHT";
        } else {
            return "SUNRISE";
        }
    }
    
    /**
     * Get the current weather condition as a string (CLEAR, RAIN, THUNDER)
     * @param world The world to check weather in
     * @return A string representing the weather
     */
    private String getWeatherCondition(World world) {
        if (world.isThundering()) {
            return "THUNDER";
        } else if (world.hasStorm()) {
            return "RAIN";
        } else {
            return "CLEAR";
        }
    }
    
    /**
     * Reload all fishing-related configurations
     */
    public void reloadConfig() {
        loadSpecialLocations();
    }
    
    /**
     * Get access to the special locations map
     */
    public Map<String, Object> getSpecialLocations() {
        ConfigurationSection locationsSection = plugin.getConfigManager().getFishingConfig()
                .getConfigurationSection("special_locations");
        
        if (locationsSection == null) {
            return new HashMap<>();
        }
        
        return locationsSection.getValues(false);
    }
}