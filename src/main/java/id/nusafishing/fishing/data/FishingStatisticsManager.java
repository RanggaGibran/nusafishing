package id.nusafishing.fishing.data;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.model.CustomFish;

public class FishingStatisticsManager {
    private final NusaFishing plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;
    private org.bukkit.scheduler.BukkitTask autosaveTask;
    
    // Cache untuk statistik
    private final Map<UUID, Integer> fishCaughtCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> legendaryFishCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> eventFishCache = new ConcurrentHashMap<>();
    private final Map<UUID, Double> largestFishWeightCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> largestFishNameCache = new ConcurrentHashMap<>();
    
    // Data tersortir untuk leaderboard
    private Map<UUID, Integer> sortedFishCaught = new HashMap<>();
    private Map<UUID, Double> sortedLargestFish = new HashMap<>();
    private long lastLeaderboardUpdate = 0;
    private static final long LEADERBOARD_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 menit
    
    public FishingStatisticsManager(NusaFishing plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "fishing_stats.yml");
        
        // Buat file jika belum ada
        if (!statsFile.exists()) {
            statsFile.getParentFile().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Tidak bisa membuat fishing_stats.yml: " + e.getMessage());
            }
        }
        
        // Load konfigurasi
        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        // Load data ke cache
        loadCaches();

        // Autosave periodik setiap 5 menit
        this.autosaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAllStats();
        }, 20 * 60 * 5, 20 * 60 * 5);
    }
    
    private void loadCaches() {
        // Load data dari config ke cache
        for (String uuidString : statsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                
                fishCaughtCache.put(uuid, statsConfig.getInt(uuidString + ".fish_caught", 0));
                legendaryFishCache.put(uuid, statsConfig.getInt(uuidString + ".legendary_fish", 0));
                eventFishCache.put(uuid, statsConfig.getInt(uuidString + ".event_fish", 0));
                largestFishWeightCache.put(uuid, statsConfig.getDouble(uuidString + ".largest_fish_weight", 0.0));
                largestFishNameCache.put(uuid, statsConfig.getString(uuidString + ".largest_fish_name", "Tidak Ada"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID tidak valid di fishing_stats.yml: " + uuidString);
            }
        }
        
        // Sortir leaderboard pertama kali
        updateLeaderboards();
    }
    
    /**
     * Update leaderboard jika data sudah kadaluarsa
     */
    private void updateLeaderboards() {
        long now = System.currentTimeMillis();
        
        if (now - lastLeaderboardUpdate > LEADERBOARD_UPDATE_INTERVAL) {
            // Sortir berdasarkan jumlah ikan
            sortedFishCaught = fishCaughtCache.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
            
            // Sortir berdasarkan berat ikan terbesar
            sortedLargestFish = largestFishWeightCache.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
            
            lastLeaderboardUpdate = now;
        }
    }
    
    /**
     * Catat ikan yang tertangkap oleh pemain
     * @param playerId UUID pemain
     * @param fish Ikan yang tertangkap
     */
    public void recordFishCaught(UUID playerId, CustomFish fish) {
        // Tambah total ikan
        int currentCount = fishCaughtCache.getOrDefault(playerId, 0);
        fishCaughtCache.put(playerId, currentCount + 1);
        
        // Cek ikan legendary atau mythic
        if (fish.getActualTier() == CustomFish.FishTier.LEGENDARY || fish.getActualTier() == CustomFish.FishTier.MYTHIC) {
            int legendaryCount = legendaryFishCache.getOrDefault(playerId, 0);
            legendaryFishCache.put(playerId, legendaryCount + 1);
        }
        
        // Cek ikan event
        if (fish.getId().startsWith("event_")) {
            int eventCount = eventFishCache.getOrDefault(playerId, 0);
            eventFishCache.put(playerId, eventCount + 1);
        }
        
        // Cek ikan terbesar
        double fishWeight = fish.getWeight();
        double currentLargest = largestFishWeightCache.getOrDefault(playerId, 0.0);
        
        if (fishWeight > currentLargest) {
            largestFishWeightCache.put(playerId, fishWeight);
            largestFishNameCache.put(playerId, fish.getName());
        }
        
        // Stats cached in memory, periodically saved to disk
    }
    
    /**
     * Simpan statistik pemain ke file konfigurasi
     * @param playerId UUID pemain
     */
    private synchronized void savePlayerStats(UUID playerId) {
        String path = playerId.toString();
        
        statsConfig.set(path + ".fish_caught", fishCaughtCache.getOrDefault(playerId, 0));
        statsConfig.set(path + ".legendary_fish", legendaryFishCache.getOrDefault(playerId, 0));
        statsConfig.set(path + ".event_fish", eventFishCache.getOrDefault(playerId, 0));
        statsConfig.set(path + ".largest_fish_weight", largestFishWeightCache.getOrDefault(playerId, 0.0));
        statsConfig.set(path + ".largest_fish_name", largestFishNameCache.getOrDefault(playerId, "Tidak Ada"));
        
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Tidak bisa menyimpan fishing_stats.yml: " + e.getMessage());
        }
    }
    
    /**
     * Simpan semua statistik pemain sekaligus
     */
    public synchronized void saveAllStats() {
        for (UUID playerId : fishCaughtCache.keySet()) {
            String path = playerId.toString();
            statsConfig.set(path + ".fish_caught", fishCaughtCache.getOrDefault(playerId, 0));
            statsConfig.set(path + ".legendary_fish", legendaryFishCache.getOrDefault(playerId, 0));
            statsConfig.set(path + ".event_fish", eventFishCache.getOrDefault(playerId, 0));
            statsConfig.set(path + ".largest_fish_weight", largestFishWeightCache.getOrDefault(playerId, 0.0));
            statsConfig.set(path + ".largest_fish_name", largestFishNameCache.getOrDefault(playerId, "Tidak Ada"));
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Tidak bisa menyimpan fishing_stats.yml: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        saveAllStats();
    }
    
    // Getter untuk placeholder
    
    public int getTotalFishCaught(UUID playerId) {
        return fishCaughtCache.getOrDefault(playerId, 0);
    }
    
    public int getLegendaryFishCaught(UUID playerId) {
        return legendaryFishCache.getOrDefault(playerId, 0);
    }
    
    public int getEventFishCaught(UUID playerId) {
        return eventFishCache.getOrDefault(playerId, 0);
    }
    
    public double getLargestFishWeight(UUID playerId) {
        return largestFishWeightCache.getOrDefault(playerId, 0.0);
    }
    
    public String getLargestFishName(UUID playerId) {
        return largestFishNameCache.getOrDefault(playerId, "Tidak Ada");
    }
    
    // Method untuk leaderboard
    
    public Map.Entry<UUID, Integer> getTopFisher(int rank) {
        updateLeaderboards();
        
        // Konversi ke list untuk akses indeks
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>(sortedFishCaught.entrySet());
        
        if (rank > 0 && rank <= entries.size()) {
            return entries.get(rank - 1);
        }
        
        return null;
    }
    
    public Map.Entry<UUID, Double> getTopLargestFish(int rank) {
        updateLeaderboards();
        
        // Konversi ke list untuk akses indeks
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(sortedLargestFish.entrySet());
        
        if (rank > 0 && rank <= entries.size()) {
            return entries.get(rank - 1);
        }
        
        return null;
    }
}