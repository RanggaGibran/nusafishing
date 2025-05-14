package id.nusafishing.event;

import org.bukkit.Material;

import id.nusafishing.fishing.model.CustomFish;

public enum EventFish {
    // LEVIATHAN_HUNT event fish
    ANCIENT_LEVIATHAN(
        "event_ancient_leviathan", 
        "Leviathan Kuno", 
        "Predator tertua yang pernah berenang di lautan primordial", 
        Material.PUFFERFISH, 
        4001, 
        5000.0, 15000.0, 
        90, 100, 
        200000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    KRAKEN_KING(
        "event_kraken_king", 
        "Raja Kraken", 
        "Penguasa lautan dalam dengan tentakel yang dapat meremukkan kapal", 
        Material.PUFFERFISH, 
        4002, 
        4000.0, 12000.0, 
        85, 100, 
        180000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    // ANCIENT_GUARDIAN event fish
    POSEIDON_WRATH(
        "event_poseidon_wrath", 
        "Murka Poseidon", 
        "Ikan yang dikatakan sebagai utusan dewa laut", 
        Material.PUFFERFISH, 
        4003, 
        2000.0, 7000.0, 
        90, 100, 
        150000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    ATLANTEAN_GUARDIAN(
        "event_atlantean_guardian", 
        "Penjaga Atlantis", 
        "Makhluk mistis yang konon menjaga reruntuhan kota yang tenggelam", 
        Material.PUFFERFISH, 
        4004, 
        3000.0, 9000.0, 
        85, 100, 
        160000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    // ABYSSAL_HORROR event fish
    ABYSSAL_NIGHTMARE(
        "event_abyssal_nightmare", 
        "Mimpi Buruk Palung", 
        "Makhluk dari kedalaman tergelap yang tidak pernah melihat cahaya", 
        Material.PUFFERFISH, 
        4005, 
        1500.0, 5000.0, 
        85, 100, 
        140000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    DEEP_ONE(
        "event_deep_one", 
        "Penghuni Palung", 
        "Makhluk kuno yang hidup di tekanan air yang dapat meremukkan tulang", 
        Material.PUFFERFISH, 
        4006, 
        2000.0, 6000.0, 
        80, 100, 
        145000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    // RIVER_TITAN event fish
    RIVER_COLOSSUS(
        "event_river_colossus", 
        "Kolosal Sungai", 
        "Ikan air tawar terbesar yang pernah ditemukan", 
        Material.PUFFERFISH, 
        4007, 
        800.0, 3000.0, 
        85, 100, 
        130000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    FRESHWATER_BEHEMOTH(
        "event_freshwater_behemoth", 
        "Behemoth Air Tawar", 
        "Predator ganas yang telah memakan segala yang ada di sungai selama berabad-abad", 
        Material.PUFFERFISH, 
        4008, 
        1000.0, 3500.0, 
        85, 100, 
        135000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    // GOLDEN_CATCH event fish
    MIDAS_TOUCH(
        "event_midas_touch", 
        "Sentuhan Midas", 
        "Ikan dengan sisik murni terbuat dari emas", 
        Material.PUFFERFISH, 
        4009, 
        100.0, 500.0, 
        95, 100, 
        200000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    GOLDEN_EMPEROR(
        "event_golden_emperor", 
        "Kaisar Emas", 
        "Raja dari semua ikan bersisik emas", 
        Material.PUFFERFISH, 
        4010, 
        200.0, 800.0, 
        90, 100, 
        190000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    // COASTAL_TREASURE event fish
    SHORE_LEVIATHAN(
        "event_shore_leviathan", 
        "Leviathan Pantai", 
        "Predator pantai legendaris yang hanya muncul sekali dalam seabad", 
        Material.PUFFERFISH, 
        4011, 
        500.0, 2000.0, 
        85, 100, 
        170000.0, 
        CustomFish.FishTier.LEGENDARY
    ),
    
    COASTAL_MONARCH(
        "event_coastal_monarch", 
        "Penguasa Pantai", 
        "Makhluk megah yang menguasai perairan dangkal", 
        Material.PUFFERFISH, 
        4012, 
        400.0, 1500.0, 
        80, 100, 
        165000.0, 
        CustomFish.FishTier.LEGENDARY
    );
    
    private final String id;
    private final String name;
    private final String description;
    private final Material material;
    private final int modelData;
    private final double minWeight;
    private final double maxWeight;
    private final int minQuality;
    private final int maxQuality;
    private final double basePrice;
    private final CustomFish.FishTier tier;
    
    EventFish(String id, String name, String description, Material material, int modelData,
              double minWeight, double maxWeight, int minQuality, int maxQuality,
              double basePrice, CustomFish.FishTier tier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.material = material;
        this.modelData = modelData;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.minQuality = minQuality;
        this.maxQuality = maxQuality;
        this.basePrice = basePrice;
        this.tier = tier;
    }
    
    /**
     * Create a CustomFish instance from this event fish
     * @return A new CustomFish instance
     */
    public CustomFish createCustomFish() {
        // Create base fish
        CustomFish fish = new CustomFish(
            id, name, description, material, modelData,
            minWeight, maxWeight, minQuality, maxQuality,
            basePrice, tier
        );
        
        // Generate an actual caught instance with proper weight/quality
        // We need to apply tier boost, quality boost, etc. just like regular fish
        return fish.createCaughtInstance(2, 1.5, 1.5);
    }
}