package id.nusafishing.fishing.model;

public class FishingRod {
    // Definisikan tier pancingan
    public enum RodTier {
        BASIC("§7", 1.0, 0, "Dasar"),
        INTERMEDIATE("§a", 1.2, 1, "Menengah"),
        ADVANCED("§9", 1.5, 2, "Mahir"),
        EXPERT("§5", 2.0, 3, "Expert"),
        MASTER("§6", 3.0, 4, "Master");
        
        private final String color;
        private final double catchRateMultiplier;
        private final int tierBoost;
        private final String displayName;
        
        RodTier(String color, double catchRateMultiplier, int tierBoost, String displayName) {
            this.color = color;
            this.catchRateMultiplier = catchRateMultiplier;
            this.tierBoost = tierBoost;
            this.displayName = displayName;
        }
        
        public String getColor() { return color; }
        public double getCatchRateMultiplier() { return catchRateMultiplier; }
        public int getTierBoost() { return tierBoost; }
        public String getDisplayName() { return displayName; }
    }
    
    private String id;
    private String name;
    private String description;
    private int customModelData;
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
        this.id = id;
        this.name = name;
        this.description = description;
        this.customModelData = customModelData;
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
    public int getCustomModelData() { return customModelData; }
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
}