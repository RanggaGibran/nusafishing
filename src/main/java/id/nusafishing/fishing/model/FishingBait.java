package id.nusafishing.fishing.model;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import id.nusafishing.NusaFishing;

public class FishingBait {
    private String id;
    private String name;
    private String description;
    private Material material;
    private int customModelData;
    private List<String> validFish;
    private double attractionModifier;
    private int tierBoost; // Tambahan: Boost tier
    private int usesPerItem; // Tambahan: Jumlah penggunaan per item
    private double price; // Harga kustom
    private double qualityBoost;  // Multiplier untuk kualitas ikan
    private double weightBoost;   // Multiplier untuk berat ikan
    
    // Add field for tracking last user
    private Player lastUser;
    
    public FishingBait(String id, String name, String description, 
                     Material material, int customModelData,
                     List<String> validFish, double attractionModifier,
                     int tierBoost, int usesPerItem, double price,
                     double qualityBoost, double weightBoost) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.material = material;
        this.customModelData = customModelData;
        this.validFish = validFish;
        this.attractionModifier = attractionModifier;
        this.tierBoost = tierBoost;
        this.usesPerItem = usesPerItem;
        this.price = price;
        this.qualityBoost = qualityBoost;
        this.weightBoost = weightBoost;
        this.lastUser = null;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public List<String> getValidFish() { return validFish; }
    public double getAttractionModifier() { return attractionModifier; }
    public int getTierBoost() { return tierBoost; }
    public int getUsesPerItem() { return usesPerItem; }
    public double getPrice() { return price; }
    public double getQualityBoost() { return qualityBoost; }
    public double getWeightBoost() { return weightBoost; }
    
    // Add getLastUser method
    public Player getLastUser() { 
        return lastUser; 
    }
    
    // Add setLastUser method
    public void setLastUser(Player player) { 
        this.lastUser = player; 
    }
    
    public boolean canCatchFish(String fishId) {
        // If validFish is empty or null, it can catch any fish
        if (validFish == null || validFish.isEmpty()) {
            return true;
        }
        return validFish.contains(fishId);
    }
    
    /**
     * Use one charge of the bait in the player's off hand
     * @param player The player using the bait
     * @return true if successful, false if no more uses or no bait
     */
    public boolean consumeUse(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem == null || offHandItem.getType() != material) {
            return false;
        }
        
        // Check if this is the correct bait
        ItemMeta meta = offHandItem.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
            return false;
        }
        
        // Check/update remaining uses
        NamespacedKey usesKey = new NamespacedKey(NusaFishing.getInstance(), "bait_uses");
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        int remainingUses = container.getOrDefault(usesKey, PersistentDataType.INTEGER, usesPerItem);
        remainingUses--;
        
        if (remainingUses <= 0) {
            // Last use, reduce stack size
            int amount = offHandItem.getAmount();
            if (amount > 1) {
                // Reset uses for the next item in stack
                container.set(usesKey, PersistentDataType.INTEGER, usesPerItem);
                offHandItem.setItemMeta(meta);
                offHandItem.setAmount(amount - 1);
            } else {
                // Remove the last item
                player.getInventory().setItemInOffHand(null);
            }
            return true;
        } else {
            // Update uses left
            container.set(usesKey, PersistentDataType.INTEGER, remainingUses);
            
            // Update lore to show uses remaining
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    if (line.contains("Sisa penggunaan:")) {
                        lore.set(i, "§7Sisa penggunaan: §e" + remainingUses + "/" + usesPerItem);
                        break;
                    }
                }
                meta.setLore(lore);
            }
            
            offHandItem.setItemMeta(meta);
            return true;
        }
    }
}