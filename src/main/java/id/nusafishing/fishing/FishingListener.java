package id.nusafishing.fishing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.FishHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
// text componennt
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.data.PlayerSkillManager;
import id.nusafishing.fishing.data.StaminaManager;
import id.nusafishing.fishing.minigame.StrikeMinigame;
import id.nusafishing.fishing.model.CustomFish;
import id.nusafishing.fishing.model.FishingRod;
import id.nusafishing.fishing.model.FishingBait;
import id.nusafishing.util.ItemBuilder;

public class FishingListener implements Listener {
    private final NusaFishing plugin;
    private final FishingManager fishingManager;
    private final StaminaManager staminaManager;
    private final PlayerSkillManager skillManager;
    private final StrikeMinigame strikeMinigame;
    private final Map<UUID, Long> lastCastTime;
    private final Map<UUID, FishingBait> activeBaits = new HashMap<>();
    
    // Keys for persistent data
    private static final String ROD_ID_KEY = "nusafishing.rod_id";
    private static final String ROD_DURABILITY_KEY = "nusafishing.durability";
    
    // Constants for fishing
    private static final int MIN_CAST_COOLDOWN_MS = 2000; // 2 seconds between casts
    private static final double CAST_STAMINA_COST = 10.0;
    private static final int DURABILITY_LOSS_ON_CAST = 1; // Durability loss when casting
    private static final int DURABILITY_LOSS_ON_CATCH = 2; // Durability loss when catching a fish

    public FishingListener(NusaFishing plugin, FishingManager fishingManager,
                         StaminaManager staminaManager, PlayerSkillManager skillManager) {
        this.plugin = plugin;
        this.fishingManager = fishingManager;
        this.staminaManager = staminaManager;
        this.skillManager = skillManager;
        this.strikeMinigame = new StrikeMinigame(plugin);
        this.lastCastTime = new HashMap<>();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        staminaManager.initializePlayer(player);
        skillManager.loadPlayerData(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        staminaManager.removePlayer(player);
        skillManager.savePlayerData(player);
        strikeMinigame.cleanupPlayer(player);
        activeBaits.remove(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Handle left-click during minigame
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || 
            event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            
            if (heldItem.getType() == Material.FISHING_ROD) {
                if (strikeMinigame.handleClick(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        
        // IMMEDIATELY cancel ANY fishing events if player is in a minigame - check first before anything else
        if (isPlayerInMinigame(player)) {
            event.setCancelled(true);
            return;
        }
        
        ItemStack fishingRod = player.getInventory().getItemInMainHand();
        
        if (fishingRod.getType() != Material.FISHING_ROD) {
            return;
        }
        
        // Check if using custom fishing rod
        FishingRod customRod = getCustomFishingRod(fishingRod);
        
        if (event.getState() == State.FISHING) {
            // Player cast their line
            handleFishingCast(event, player, fishingRod, customRod);
        } else if (event.getState() == State.BITE) {
            // Fish is biting - start minigame
            handleFishBite(event, player, fishingRod, customRod);
        } else if (event.getState() == State.CAUGHT_FISH) {
            // Default fish caught - replace with custom mechanics
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // If the player is in a minigame, end it (fish escapes)
        if (isPlayerInMinigame(player)) {
            player.sendMessage("§c§lIkan lolos! §fKamu membuka inventory saat memancing.");
            strikeMinigame.endMinigameAbort(player);
        }
    }
    
    private void handleFishingCast(PlayerFishEvent event, Player player, ItemStack fishingRod, FishingRod customRod) {
        UUID playerId = player.getUniqueId();
        
        // Check for cooldown
        long now = System.currentTimeMillis();
        long lastCast = lastCastTime.getOrDefault(playerId, 0L);
        if (now - lastCast < MIN_CAST_COOLDOWN_MS) {
            event.setCancelled(true);
            player.sendMessage("§cTunggu sebentar sebelum melempar kembali!");
            return;
        }
        
        // Check for stamina
        if (!staminaManager.hasEnoughStamina(player, CAST_STAMINA_COST)) {
            event.setCancelled(true);
            player.sendMessage("§cStamina tidak cukup untuk memancing!");
            return;
        }
        
        // Check rod durability
        if (customRod != null) {
            int currentDurability = getRodDurability(fishingRod);
            if (currentDurability <= 0) {
                event.setCancelled(true);
                player.sendMessage("§cPancingan kamu rusak! Perbaiki menggunakan /nusafishing repair");
                return;
            }
            
            // Reduce durability on cast
            reduceDurability(player, fishingRod, DURABILITY_LOSS_ON_CAST);
        }
        
        // Cek umpan di inventory dan pilih secara otomatis
        FishingBait selectedBait = selectBaitFromInventory(player);
        if (selectedBait != null) {
            activeBaits.put(playerId, selectedBait);
            
            // Terapkan boost lure ke fishing hook
            FishHook hook = event.getHook();
            
            // Hitung level lure dari rod + efek dari bait
            int baseLureLevel = customRod != null ? customRod.getLureLevel() : 0;
            
            // Konversi daya tarik umpan ke tambahan level lure (max +3)
            int extraLure = (int)Math.min(3, (selectedBait.getAttractionModifier() - 1.0) * 2);
            
            // Terapkan total lure level - using compatible methods
            try {
                // Try to use the modern method if available
                hook.getClass().getMethod("setLureTime", int.class)
                    .invoke(hook, (baseLureLevel + extraLure) * 20); // Multiply by ticks
            } catch (Exception e1) {
                try {
                    // If that doesn't work, try setting the wait time directly
                    // More wait time = less effective lure
                    int tickMultiplier = 100 - ((baseLureLevel + extraLure) * 10);
                    hook.getClass().getMethod("setMinWaitTime", int.class)
                        .invoke(hook, Math.max(10, tickMultiplier));
                } catch (Exception e2) {
                    // If all fails, just log but continue
                    plugin.getLogger().warning("Could not apply lure boost: " + e2.getMessage());
                }
            }
            
            // Beri tahu pemain umpan yang aktif dengan pesan yang SANGAT JELAS
            String attractionText = String.format("§e+%.0f%%", (selectedBait.getAttractionModifier() - 1.0) * 100);
            String lureBoost = extraLure > 0 ? " §7(+"+extraLure+" Lure)" : "";
            
            // Efek visual LEBIH DRAMATIS
            player.getWorld().spawnParticle(
                Particle.WATER_BUBBLE, 
                player.getLocation().add(0, 1.0, 0), 
                50, 0.7, 0.7, 0.7, 0.1
            );
            player.getWorld().spawnParticle(
                Particle.WATER_SPLASH,
                event.getHook().getLocation(),
                30, 0.3, 0.0, 0.3, 0.2
            );
            
            // Efek suara LEBIH JELAS
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.5f, 1.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        // Consume stamina and update last cast time
        staminaManager.consumeStamina(player, CAST_STAMINA_COST, customRod);
        lastCastTime.put(playerId, now);
    }
    
    private FishingBait selectBaitFromInventory(Player player) {
        // Prioritaskan umpan yang ada di off hand
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        FishingBait offHandBait = fishingManager.getBaitFromItem(offHandItem);
        if (offHandBait != null) {
            return offHandBait;
        }
        
        // Jika tidak ada di off hand, cek seluruh inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            FishingBait bait = fishingManager.getBaitFromItem(item);
            if (bait != null) {
                return bait;
            }
        }
        
        return null;
    }
    
    private void handleFishBite(PlayerFishEvent event, Player player, ItemStack fishingRod, FishingRod customRod) {
        // Cancel vanilla behavior
        event.setCancelled(true);
        
        // Don't start a new minigame if player is already in one
        if (isPlayerInMinigame(player)) {
            player.sendMessage("§cKamu sedang dalam permainan memancing!");
            return;
        }
        
        // Check rod durability again as a safety measure
        if (customRod != null) {
            int currentDurability = getRodDurability(fishingRod);
            if (currentDurability <= DURABILITY_LOSS_ON_CATCH) {
                player.sendMessage("§cPancingan kamu hampir rusak! Perbaiki segera dengan /nusafishing repair");
            }
        }
        
        // Get active bait instead of checking offhand
        UUID playerId = player.getUniqueId();
        FishingBait bait = activeBaits.get(playerId);
        
        // Display bait effect if using bait
        if (bait != null) {
            showBaitEffect(player, bait);
        }
        
        // Select a random fish based on player's location, rod, bait, etc.
        CustomFish fish = fishingManager.selectRandomFish(player, customRod);
        
        if (fish != null) {
            // Keep the huge fish notification but without record tracking
            if (fish.getWeight() >= 1000) {
                player.sendTitle(
                    "§d§l✦✦ IKAN RAKSASA! ✦✦", 
                    "§fKamu menangkap §b" + fish.getName() + " §e" + fish.getFormattedWeight() + "§f!",
                    10, 60, 20
                );
                
                player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation(),
                    100, 3, 3, 3, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            }
            
            // IMPORTANT: Cancel all active fishing hooks/bobbers for this player before starting minigame
            cancelAllFishingHooks(player);
            
            // Set metadata to indicate player is in minigame
            player.setMetadata("NF_InMinigame", new FixedMetadataValue(plugin, true));
            
            // Add visual effects based on fish tier
            int tierIndex = 0;
            if (fish.getActualTier() != null) {
                tierIndex = fish.getActualTier().ordinal();
            } else {
                // Fallback to base tier if actual tier is null
                tierIndex = fish.getBaseTier().ordinal();
                // Log the issue for debugging
                plugin.getLogger().warning("Null actualTier found for fish: " + fish.getId() + ", using baseTier instead");
            }
            
            switch (tierIndex) {
                case 4: // LEGENDARY
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
                    player.spawnParticle(Particle.TOTEM, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    break;
                case 3: // EPIC
                    player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                    player.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation(), 20, 0.5, 0.3, 0.5, 0.1);
                    break;
                case 2: // RARE
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.7f);
                    player.spawnParticle(Particle.END_ROD, player.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                    break;
                default:
                    // Normal sounds for common/uncommon
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
            }
            
            // Start the minigame
            strikeMinigame.startStrikeGame(player, fish, fishingRod, DURABILITY_LOSS_ON_CATCH);
            
            // Use bait (consume one use)
            if (bait != null) {
                consumeBaitFromInventory(player, bait);
            }
            
            // Tambahkan wear & tear yang lebih signifikan untuk ikan berat
            if (fishingRod != null && customRod != null) {
                // Skala kerusakan berdasarkan berat relatif terhadap kapasitas
                // 1. Dapatkan kapasitas maksimal rod
                double maxCapacity = customRod.getMaxWeightCapacity();
                
                // 2. Hitung persentase kapasitas yang digunakan
                double capacityUsedPercent = (fish.getWeight() / maxCapacity) * 100.0;
                
                // 3. Skala kerusakan berdasarkan persentase
                int baseDurabilityLoss = DURABILITY_LOSS_ON_CATCH; // Nilai dasar (2)
                
                int scaledDurabilityLoss;
                if (capacityUsedPercent < 20) {
                    // Ikan ringan - kerusakan normal
                    scaledDurabilityLoss = baseDurabilityLoss;
                } else if (capacityUsedPercent < 50) {
                    // Ikan medium - kerusakan sedang
                    scaledDurabilityLoss = baseDurabilityLoss * 2;
                } else if (capacityUsedPercent < 80) {
                    // Ikan berat - kerusakan tinggi
                    scaledDurabilityLoss = baseDurabilityLoss * 3;
                } else {
                    // Ikan sangat berat (mendekati kapasitas) - kerusakan ekstrem
                    scaledDurabilityLoss = baseDurabilityLoss * 5;
                }
                
                // 4. Terapkan kerusakan
                reduceDurability(player, fishingRod, scaledDurabilityLoss);
            }
            
            // Tambahkan informasi jika umpan membantu menangkap ikan
            if (bait != null) {
                String baitBonus = "";
                
                // Cek apakah ikan cocok dengan umpan
                if (bait.getValidFish().contains(fish.getId())) {
                    String qualityText = "+" + (int)((bait.getQualityBoost() * 1.2 - 1.0) * 100) + "% kualitas";
                    String weightText = "+" + (int)((bait.getWeightBoost() * 1.3 - 1.0) * 100) + "% berat";
                    baitBonus = "§a§l("+qualityText+", "+weightText+")";
                    player.sendMessage("§a§l[SEMPURNA!] §fUmpan §e" + bait.getName() + " §famat efektif untuk §b" + fish.getName() + "! " + baitBonus);
                } else {
                    String qualityText = "+" + (int)((bait.getQualityBoost() - 1.0) * 100) + "% kualitas";
                    String weightText = "+" + (int)((bait.getWeightBoost() - 1.0) * 100) + "% berat";
                    baitBonus = "§e("+qualityText+", "+weightText+")";
                    player.sendMessage("§a§l[SUKSES] §fUmpan §e" + bait.getName() + " §fmeningkatkan hasil tangkapan! " + baitBonus);
                }
            }
            
            // Tambahkan informasi jika umpan membantu menangkap ikan
            if (bait != null && bait.getValidFish().contains(fish.getId())) {
                // Efek spesial jika umpan cocok dengan ikan
                player.sendMessage("§a§o" + bait.getName() + " sangat efektif untuk menangkap " + fish.getName() + "!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                player.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 
                                    10, 0.5, 0.5, 0.5, 0.1);
            }
            
            // Kurangi stamina berdasarkan berat ikan
            plugin.getStaminaManager().consumeStaminaForFish(player, fish, customRod);
            
            // Check if this was an event fish
            if (fish != null && fish.getId().startsWith("event_")) {
                // Notify the event manager
                plugin.getEventManager().handleEventFishCaught(player, fish);
            }
            
            // Schedule the removal of metadata after max time
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("NF_InMinigame", plugin);
                    // Clear active bait when minigame ends
                    activeBaits.remove(playerId);
                }
            }.runTaskLater(plugin, 20 * 15); // 15 seconds max
        } else {
            // Efek suara saja tanpa pesan
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            // Clear active bait on failure
            activeBaits.remove(playerId);
        }
    }
    
    private boolean consumeBaitFromInventory(Player player, FishingBait bait) {
        // First, set this player as the last user
        bait.setLastUser(player);
        
        // Cek off-hand terlebih dahulu
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem != null && isSameBait(offHandItem, bait)) {
            return consumeBaitItem(player, offHandItem, player.getInventory().getItemInOffHand());
        }
        
        // Cek seluruh inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isSameBait(item, bait)) {
                return consumeBaitItem(player, item, i);
            }
        }
        
        return false;
    }
    
    private boolean isSameBait(ItemStack item, FishingBait bait) {
        if (item.getType() != bait.getMaterial()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        
        return meta.getCustomModelData() == bait.getCustomModelData();
    }
    
    private boolean consumeBaitItem(Player player, ItemStack item, int inventorySlot) {
        // Get bait data
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey(plugin, "bait_uses");
        NamespacedKey baitIdKey = new NamespacedKey(plugin, "nusafishing.bait_id");
        
        // Get bait ID to find the config
        if (!container.has(baitIdKey, PersistentDataType.STRING)) return false;
        String baitId = container.get(baitIdKey, PersistentDataType.STRING);
        FishingBait baitConfig = plugin.getConfigManager().getFishingBaits().get(baitId);
        if (baitConfig == null) return false;
        
        // Get remaining uses
        int remainingUses = container.getOrDefault(usesKey, PersistentDataType.INTEGER, baitConfig.getUsesPerItem());
        remainingUses--;
        
        if (remainingUses <= 0) {
            // Last use, reduce stack size
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                // Reset uses for next item
                container.set(usesKey, PersistentDataType.INTEGER, baitConfig.getUsesPerItem());
                item.setItemMeta(meta);
            } else {
                // Remove last item
                player.getInventory().setItem(inventorySlot, null);
            }
        } else {
            // Update uses count
            container.set(usesKey, PersistentDataType.INTEGER, remainingUses);
            
            // Update lore to reflect uses
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    if (line.contains("Sisa penggunaan:")) {
                        lore.set(i, "§7Sisa penggunaan: §e" + remainingUses + "/" + baitConfig.getUsesPerItem());
                        break;
                    }
                }
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return true;
    }
    
    private boolean consumeBaitItem(Player player, ItemStack item, ItemStack offHandItem) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey(plugin, "bait_uses");
        NamespacedKey baitIdKey = new NamespacedKey(plugin, "nusafishing.bait_id");
        
        // Get bait ID
        if (!container.has(baitIdKey, PersistentDataType.STRING)) return false;
        String baitId = container.get(baitIdKey, PersistentDataType.STRING);
        FishingBait baitConfig = plugin.getConfigManager().getFishingBaits().get(baitId);
        if (baitConfig == null) return false;
        
        // Get remaining uses
        int remainingUses = container.getOrDefault(usesKey, PersistentDataType.INTEGER, baitConfig.getUsesPerItem());
        remainingUses--;
        
        if (remainingUses <= 0) {
            // Last use, reduce stack size
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                // Reset uses
                container.set(usesKey, PersistentDataType.INTEGER, baitConfig.getUsesPerItem());
                item.setItemMeta(meta);
            } else {
                // Remove last item
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            // Update uses
            container.set(usesKey, PersistentDataType.INTEGER, remainingUses);
            
            // Update lore
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    if (line.contains("Sisa penggunaan:")) {
                        lore.set(i, "§7Sisa penggunaan: §e" + remainingUses + "/" + baitConfig.getUsesPerItem());
                        break;
                    }
                }
                meta.setLore(lore);
            }
            
            item.setItemMeta(meta);
        }
        
        return true;
    }
    
    /**
     * Cancel all active fishing hooks for a player
     * @param player The player whose hooks should be cancelled
     */
    private void cancelAllFishingHooks(Player player) {
        // Get all hooks in the world and cancel those belonging to this player
        player.getWorld().getEntitiesByClass(org.bukkit.entity.FishHook.class).stream()
                .filter(hook -> hook.getShooter() instanceof Player)
                .filter(hook -> ((Player)hook.getShooter()).getUniqueId().equals(player.getUniqueId()))
                .forEach(org.bukkit.entity.Entity::remove);
    }
    
    /**
     * Get the current durability of a fishing rod
     * @param fishingRod The fishing rod item
     * @return The current durability, or 0 if not found
     */
    public static int getRodDurability(ItemStack fishingRod) {
        if (fishingRod == null || fishingRod.getType() != Material.FISHING_ROD) {
            return 0;
        }
        
        ItemMeta meta = fishingRod.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(NusaFishing.getInstance(), ROD_DURABILITY_KEY);
        
        if (container.has(key, PersistentDataType.INTEGER)) {
            return container.get(key, PersistentDataType.INTEGER);
        }
        
        return 0;
    }
    
    /**
     * Reduce the durability of a fishing rod
     * @param player The player using the rod
     * @param fishingRod The fishing rod item
     * @param amount The amount to reduce
     * @return The new durability
     */
    public static int reduceDurability(Player player, ItemStack fishingRod, int amount) {
        if (fishingRod == null || fishingRod.getType() != Material.FISHING_ROD) {
            return 0;
        }
        
        ItemMeta meta = fishingRod.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey durabilityKey = new NamespacedKey(NusaFishing.getInstance(), ROD_DURABILITY_KEY);
        NamespacedKey rodIdKey = new NamespacedKey(NusaFishing.getInstance(), ROD_ID_KEY);
        
        if (!container.has(durabilityKey, PersistentDataType.INTEGER) || 
            !container.has(rodIdKey, PersistentDataType.STRING)) {
            return 0;
        }
        
        // Get current values
        int currentDurability = container.get(durabilityKey, PersistentDataType.INTEGER);
        String rodId = container.get(rodIdKey, PersistentDataType.STRING);
        
        // Get rod info for max durability
        FishingRod rod = NusaFishing.getInstance().getFishingManager().getRodById(rodId);
        if (rod == null) {
            return currentDurability;
        }
        
        // Calculate new durability
        int newDurability = Math.max(0, currentDurability - amount);
        
        // Update persistent data
        container.set(durabilityKey, PersistentDataType.INTEGER, newDurability);
        
        // Update lore
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("Ketahanan:")) {
                    lore.set(i, "§7Ketahanan: §f" + newDurability + "/" + rod.getDurability());
                    break;
                }
            }
            meta.setLore(lore);
        }
        
        // Apply changes
        fishingRod.setItemMeta(meta);
        
        // Notify player if durability is low
        if (newDurability <= 10 && newDurability > 0) {
            player.sendMessage("§c§lPeringatan! §fPancingan kamu sudah hampir rusak (" + newDurability + "/" + rod.getDurability() + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
        } else if (newDurability <= 0) {
            player.sendMessage("§c§lPancingan rusak! §fGunakan /nusafishing repair untuk memperbaikinya");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        }
        
        return newDurability;
    }
    
    /**
     * Repair a fishing rod to full durability
     * @param player The player repairing the rod
     * @param fishingRod The fishing rod item
     * @return true if repair successful
     */
    public static boolean repairRod(Player player, ItemStack fishingRod) {
        if (fishingRod == null || fishingRod.getType() != Material.FISHING_ROD) {
            return false;
        }
        
        ItemMeta meta = fishingRod.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey durabilityKey = new NamespacedKey(NusaFishing.getInstance(), ROD_DURABILITY_KEY);
        NamespacedKey rodIdKey = new NamespacedKey(NusaFishing.getInstance(), ROD_ID_KEY);
        
        if (!container.has(rodIdKey, PersistentDataType.STRING)) {
            return false;
        }
        
        // Get rod info for max durability
        String rodId = container.get(rodIdKey, PersistentDataType.STRING);
        FishingRod rod = NusaFishing.getInstance().getFishingManager().getRodById(rodId);
        if (rod == null) {
            return false;
        }
        
        // Reset durability to max
        container.set(durabilityKey, PersistentDataType.INTEGER, rod.getDurability());
        
        // Update lore
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("Ketahanan:")) {
                    lore.set(i, "§7Ketahanan: §f" + rod.getDurability() + "/" + rod.getDurability());
                    break;
                }
            }
            meta.setLore(lore);
        }
        
        // Apply changes
        fishingRod.setItemMeta(meta);
        return true;
    }
    
    private FishingRod getCustomFishingRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        // Check for custom model data
        if (meta.hasCustomModelData()) {
            int modelData = meta.getCustomModelData();
            // Get rod by custom model data
            return fishingManager.getFishingRodByModelData(modelData);
        }
        
        return null;
    }
    
    // Helper method to check if player is in minigame
    private boolean isPlayerInMinigame(Player player) {
        return player.getMetadata("NF_InMinigame").stream()
               .anyMatch(metaValue -> metaValue.asBoolean());
    }
    
    /**
     * Display visual and sound effects when a player is using bait
     * @param player The player
     * @param bait The bait being used
     */
    private void showBaitEffect(Player player, FishingBait bait) {
        // Schedule a repeating visual effect
        new BukkitRunnable() {
            int count = 0;
            ItemStack lastBaitItem = null;
            
            @Override
            public void run() {
                // Check if player still has bait in off hand
                ItemStack offHandItem = player.getInventory().getItemInOffHand();
                if (offHandItem == null || offHandItem.getType() != bait.getMaterial() || 
                    count >= 5) {
                    this.cancel();
                    return;
                }
                
                // Only update if the bait has changed
                if (!offHandItem.equals(lastBaitItem)) {
                    // Update reference
                    lastBaitItem = offHandItem.clone();
                    
                    // Show particles
                    player.getWorld().spawnParticle(
                        Particle.WATER_BUBBLE, 
                        player.getLocation().add(0, 0.5, 0), 
                        5, 0.2, 0.2, 0.2, 0
                    );
                    
                    // Send action bar message
                    String tierBoostText = bait.getTierBoost() > 0 ? 
                        " §7(+" + bait.getTierBoost() + " Tier)" : "";
                    player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR, 
                        TextComponent.fromLegacyText("§aUmpan aktif: §f" + bait.getName() + 
                                                  " §7- §f+" + 
                                                  (int)((bait.getAttractionModifier() - 1.0) * 100) + 
                                                  "% §7daya tarik" + tierBoostText)
                    );
                }
                count++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    // Tambahkan method baru untuk memformat nama-nama ikan
    private String formatFishNamesForBait(List<String> fishIds) {
        if (fishIds == null || fishIds.isEmpty()) {
            return "semua ikan";
        }
        
        List<String> names = new ArrayList<>();
        for (String fishId : fishIds) {
            CustomFish fish = plugin.getConfigManager().getCustomFishes().get(fishId);
            if (fish != null) {
                names.add(fish.getName());
            }
        }
        
        return String.join(", ", names);
    }
}