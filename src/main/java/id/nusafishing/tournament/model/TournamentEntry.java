package id.nusafishing.tournament.model;

import java.util.UUID;

import id.nusafishing.fishing.model.CustomFish;

public class TournamentEntry {
    private final String playerName;
    private final UUID playerId;
    
    // Stat tracking
    private int fishCount = 0;
    private double totalWeight = 0.0;
    
    // Best fish tracking
    private double heaviestFishWeight = 0.0;
    private String heaviestFishName = "Tidak Ada";
    private int highestQualityFishQuality = 0;
    private String highestQualityFishName = "Tidak Ada";
    
    public TournamentEntry(String playerName, UUID playerId) {
        this.playerName = playerName;
        this.playerId = playerId;
    }
    
    // Getters
    public String getPlayerName() {
        return playerName;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getFishCount() {
        return fishCount;
    }
    
    public double getTotalWeight() {
        return totalWeight;
    }
    
    public double getHeaviestFishWeight() {
        return heaviestFishWeight;
    }
    
    public String getHeaviestFishName() {
        return heaviestFishName;
    }
    
    public int getHighestQualityFishQuality() {
        return highestQualityFishQuality;
    }
    
    public String getHighestQualityFishName() {
        return highestQualityFishName;
    }
    
    // Update methods
    public void incrementFishCount() {
        fishCount++;
    }
    
    public void addToTotalWeight(double weight) {
        totalWeight += weight;
    }
    
    public void setHeaviestFish(CustomFish fish) {
        if (fish != null) {
            heaviestFishWeight = fish.getWeight();
            heaviestFishName = fish.getName();
        }
    }
    
    public void setHighestQualityFish(CustomFish fish) {
        if (fish != null) {
            highestQualityFishQuality = fish.getQuality();
            highestQualityFishName = fish.getName();
        }
    }
}