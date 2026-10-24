package id.nusafishing.fishing.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.model.FishingRod;
import id.nusafishing.fishing.model.CustomFish;

public class StaminaManager {
    private final NusaFishing plugin;
    private final Map<UUID, Double> playerStamina;
    private final Map<UUID, BukkitTask> regenerationTasks;
    
    private static final double MAX_STAMINA = 100.0;
    private static final double REGEN_AMOUNT = 1.0;
    private static final long REGEN_DELAY = 20L; // 1 second in ticks
    
    public StaminaManager(NusaFishing plugin) {
        this.plugin = plugin;
        this.playerStamina = new HashMap<>();
        this.regenerationTasks = new HashMap<>();
    }
    
    public void initializePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        playerStamina.put(playerId, MAX_STAMINA);
        startRegeneration(player);
    }
    
    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        playerStamina.remove(playerId);
        
        BukkitTask task = regenerationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    public boolean hasEnoughStamina(Player player, double amount) {
        return getStamina(player) >= amount;
    }
    
    public void consumeStamina(Player player, double amount, FishingRod rod) {
        UUID playerId = player.getUniqueId();
        double staminaCost = amount;
        
        // Apply rod's stamina modifier if present
        if (rod != null) {
            staminaCost *= rod.getStaminaModifier();
        }
        
        double currentStamina = playerStamina.getOrDefault(playerId, MAX_STAMINA);
        double newStamina = Math.max(0, currentStamina - staminaCost);
        
        playerStamina.put(playerId, newStamina);
        
        // Show action bar message only when stamina is low/depleted
        if (newStamina <= 20) {
            String staminaMessage;
            if (newStamina <= 0) {
                staminaMessage = "§c§lStamina habis! §cTunggu stamina regenerasi...";
            } else {
                staminaMessage = "§e§lStamina rendah! §e" + String.format("%.1f", newStamina) + "%";
            }
            sendActionBar(player, staminaMessage);
        }
    }
    
    public void restoreStamina(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        double currentStamina = playerStamina.getOrDefault(playerId, MAX_STAMINA);
        double newStamina = Math.min(MAX_STAMINA, currentStamina + amount);
        
        playerStamina.put(playerId, newStamina);
    }
    
    public double getStamina(Player player) {
        return playerStamina.getOrDefault(player.getUniqueId(), MAX_STAMINA);
    }
    
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    private void startRegeneration(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing task
        BukkitTask existingTask = regenerationTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start new regeneration task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                double stamina = playerStamina.getOrDefault(playerId, MAX_STAMINA);
                if (stamina < MAX_STAMINA) {
                    restoreStamina(player, REGEN_AMOUNT);
                    
                    // Show recovery message if stamina is still low
                    if (stamina < 20) {
                        sendActionBar(player, "§e§lStamina: §e" + String.format("%.1f", stamina + REGEN_AMOUNT) + "%");
                    }
                }
            }
        }.runTaskTimer(plugin, REGEN_DELAY, REGEN_DELAY);
        
        regenerationTasks.put(playerId, task);
    }
    
    /**
     * Konsumsi stamina berdasarkan berat ikan yang ditangkap
     * @param player Pemain yang menangkap ikan
     * @param fish Ikan yang ditangkap
     * @param rod Pancingan yang digunakan
     */
    public void consumeStaminaForFish(Player player, CustomFish fish, FishingRod rod) {
        // Base stamina cost is 10 + weight-based cost
        double fishWeight = fish.getWeight();
        double weightFactor;
        
        // Skala berdasarkan berat
        if (fishWeight < 10) {
            weightFactor = fishWeight / 2; // 0-5 stamina
        } else if (fishWeight < 100) {
            weightFactor = 5 + (fishWeight - 10) / 10; // 5-14 stamina
        } else if (fishWeight < 1000) {
            weightFactor = 15 + (fishWeight - 100) / 100; // 15-24 stamina
        } else {
            weightFactor = 25 + (fishWeight - 1000) / 1000; // 25+ stamina
        }
        
        // Cap the stamina cost at 50 to avoid excessive penalties
        double staminaCost = Math.min(50, 10 + weightFactor);
        
        // Apply rod stamina modifier
        if (rod != null) {
            staminaCost *= rod.getStaminaModifier();
        }
        
        // Consume the stamina
        consumeStamina(player, staminaCost, rod);
        
        // Notify player if significant stamina loss
        if (staminaCost > 20) {
            player.sendMessage("§e§lMelelahkan! §fMenangkap ikan seberat §e" + 
                fish.getFormattedWeight() + " §fmenguras §e" + 
                String.format("%.1f", staminaCost) + " §fstamina.");
        }
    }
}