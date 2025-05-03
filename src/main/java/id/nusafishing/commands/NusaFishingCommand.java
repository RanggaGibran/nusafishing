package id.nusafishing.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;  // Add this import
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.FishingManager;
import id.nusafishing.fishing.FishingListener;
import id.nusafishing.fishing.model.CustomFish;
import id.nusafishing.fishing.model.FishingBait;
import id.nusafishing.fishing.model.FishingRod;
import id.nusafishing.util.ItemBuilder;
import id.nusafishing.economy.EconomyManager;
import id.nusafishing.gui.ShopGUI;
import id.nusafishing.event.EventFish;
import id.nusafishing.event.EventType;

public class NusaFishingCommand implements CommandExecutor, TabCompleter {
    private final NusaFishing plugin;
    private final FishingManager fishingManager;
    
    // Keys for persistent data
    private static final String ROD_ID_KEY = "nusafishing.rod_id";
    private static final String ROD_DURABILITY_KEY = "nusafishing.durability";
    private static final String BAIT_ID_KEY = "nusafishing.bait_id";
    
    public NusaFishingCommand(NusaFishing plugin, FishingManager fishingManager) {
        this.plugin = plugin;
        this.fishingManager = fishingManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§b=== NusaFishing Commands ===");
            sender.sendMessage("§f/nusafishing help - Tampilkan bantuan");
            sender.sendMessage("§f/nusafishing rod <id> [jumlah] - Dapatkan pancingan");
            sender.sendMessage("§f/nusafishing bait <id> [jumlah] - Dapatkan umpan");
            sender.sendMessage("§f/nusafishing reload - Muat ulang konfigurasi");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                // Tampilkan bantuan
                sender.sendMessage("§b=== NusaFishing Commands ===");
                sender.sendMessage("§f/nusafishing rod <id> [jumlah] - Dapatkan pancingan");
                sender.sendMessage("§f/nusafishing bait <id> [jumlah] - Dapatkan umpan");
                sender.sendMessage("§f/nusafishing repair - Perbaiki pancingan di tangan");
                sender.sendMessage("§f/nusafishing shop - Buka toko untuk beli dan jual");
                
                if (sender.hasPermission("nusafishing.sell")) {
                    sender.sendMessage("§f/nusafishing sell - Jual ikan di tangan");
                    sender.sendMessage("§f/nusafishing sell all - Jual semua ikan di inventory");
                }
                
                if (sender.hasPermission("nusafishing.admin")) {
                    sender.sendMessage("§f/nusafishing reload - Muat ulang konfigurasi");
                    sender.sendMessage("§f/nusafishing location list - Lihat lokasi spesial");
                    sender.sendMessage("§f/nusafishing location tp <id> - Teleport ke lokasi spesial");
                    sender.sendMessage("§f/nusafishing location create <id> <radius> - Buat lokasi spesial baru");
                    sender.sendMessage("§f/nusafishing location delete <id> - Hapus lokasi spesial");
                }
                return true;
                
            case "rod":
                // Berikan pancingan
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                    return true;
                }
                
                if (!sender.hasPermission("nusafishing.rod")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cPenggunaan: /nusafishing rod <id> [jumlah]");
                    return true;
                }
                
                String rodId = args[1].toLowerCase();
                int rodAmount = 1;
                
                if (args.length > 2) {
                    try {
                        rodAmount = Integer.parseInt(args[2]);
                        if (rodAmount < 1) rodAmount = 1;
                        if (rodAmount > 64) rodAmount = 64;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cJumlah harus berupa angka!");
                        return true;
                    }
                }
                
                return giveRod((Player) sender, rodId, rodAmount);
                
            case "bait":
                // Berikan umpan
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                    return true;
                }
                
                if (!sender.hasPermission("nusafishing.bait")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cPenggunaan: /nusafishing bait <id> [jumlah]");
                    return true;
                }
                
                String baitId = args[1].toLowerCase();
                int baitAmount = 1;
                
                if (args.length > 2) {
                    try {
                        baitAmount = Integer.parseInt(args[2]);
                        if (baitAmount < 1) baitAmount = 1;
                        if (baitAmount > 64) baitAmount = 64;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cJumlah harus berupa angka!");
                        return true;
                    }
                }
                
                return giveBait((Player) sender, baitId, baitAmount);
                
            case "reload":
                if (!sender.hasPermission("nusafishing.admin.reload")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk melakukan ini!");
                    return true;
                }
                
                sender.sendMessage("§aMereload konfigurasi plugin...");
                
                // Use the new method that properly reloads from disk
                plugin.getConfigManager().reloadConfigs();
                
                // Also reload other dependent systems
                fishingManager.reloadConfig();
                
                sender.sendMessage("§a§lBerhasil! §fKonfigurasi plugin telah dimuat ulang.");
                return true;
                
            case "location":
                // Handle location management commands
                if (!sender.hasPermission("nusafishing.admin")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cPenggunaan: /nusafishing location <list|tp|create|delete>");
                    return true;
                }
                
                String locationAction = args[1].toLowerCase();
                
                switch (locationAction) {
                    case "list":
                        // List all special fishing locations
                        Map<String, Object> locations = fishingManager.getSpecialLocations();
                        
                        if (locations.isEmpty()) {
                            sender.sendMessage("§cBelum ada lokasi memancing spesial yang terdaftar.");
                            return true;
                        }
                        
                        sender.sendMessage("§b=== Lokasi Memancing Spesial ===");
                        for (String locId : locations.keySet()) {
                            ConfigurationSection locSection = plugin.getConfigManager().getFishingConfig()
                                    .getConfigurationSection("special_locations." + locId);
                            String name = locSection.getString("name", locId);
                            double centerX = locSection.getDouble("center_x");
                            double centerZ = locSection.getDouble("center_z");
                            double radius = locSection.getDouble("radius");
                            
                            sender.sendMessage(String.format("§f- §b%s §f(§7%s§f): §7X: %.0f, Z: %.0f, Radius: %.0f", 
                                    name, locId, centerX, centerZ, radius));
                        }
                        return true;
                        
                    case "tp":
                        // Teleport to a special location
                        if (!(sender instanceof Player)) {
                            sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                            return true;
                        }
                        
                        if (args.length < 3) {
                            sender.sendMessage("§cPenggunaan: /nusafishing location tp <id>");
                            return true;
                        }
                        
                        return teleportToLocation((Player) sender, args[2]);
                        
                    case "create":
                        // Create a new special location
                        if (!(sender instanceof Player)) {
                            sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                            return true;
                        }
                        
                        if (args.length < 3) {
                            sender.sendMessage("§cPenggunaan: /nusafishing location create <id> [radius] [name]");
                            return true;
                        }
                        
                        String newLocationId = args[2].toLowerCase();
                        double radius = 50.0; // Default radius
                        String name = newLocationId;
                        
                        // Parse radius if provided
                        if (args.length > 3) {
                            try {
                                radius = Double.parseDouble(args[3]);
                                if (radius < 1) radius = 1;
                                if (radius > 500) radius = 500;
                            } catch (NumberFormatException e) {
                                sender.sendMessage("§cRadius harus berupa angka!");
                                return true;
                            }
                        }
                        
                        // Parse name if provided
                        if (args.length > 4) {
                            StringBuilder nameBuilder = new StringBuilder();
                            for (int i = 4; i < args.length; i++) {
                                if (i > 4) nameBuilder.append(" ");
                                nameBuilder.append(args[i]);
                            }
                            name = nameBuilder.toString();
                        }
                        
                        return createLocation((Player) sender, newLocationId, radius, name);
                        
                    case "delete":
                        // Delete a special location
                        if (args.length < 3) {
                            sender.sendMessage("§cPenggunaan: /nusafishing location delete <id>");
                            return true;
                        }
                        
                        return deleteLocation(sender, args[2]);
                        
                    default:
                        sender.sendMessage("§cSubcommand lokasi tidak valid! Gunakan list, tp, create, atau delete.");
                        return true;
                }
                
            case "repair":
                // Repair fishing rod
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                    return true;
                }
                
                // Check permission
                if (!sender.hasPermission("nusafishing.repair")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                return repairRod((Player) sender);
                
            case "sell":
                // Sell fish from inventory
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                    return true;
                }
                
                if (!sender.hasPermission("nusafishing.sell")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                if (!plugin.getEconomyManager().isEconomyEnabled()) {
                    sender.sendMessage("§cFitur ekonomi tidak aktif! Konfirmasi Vault terpasang dan ada plugin ekonomi.");
                    return true;
                }
                
                // Check for "all" parameter
                boolean sellAll = args.length > 1 && args[1].equalsIgnoreCase("all");
                
                return sellFish((Player) sender, sellAll);
                
            case "shop":
                // Open shop GUI
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cCommand ini hanya dapat digunakan oleh player!");
                    return true;
                }
                
                if (!sender.hasPermission("nusafishing.shop")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk menggunakan perintah ini!");
                    return true;
                }
                
                if (!plugin.getEconomyManager().isEconomyEnabled()) {
                    sender.sendMessage("§cFitur ekonomi tidak aktif! Konfirmasi Vault terpasang dan ada plugin ekonomi.");
                    return true;
                }
                
                new ShopGUI((Player) sender, plugin).open();
                return true;
                
            case "event":
                // Require admin permission
                if (!sender.hasPermission("nusafishing.admin.event")) {
                    sender.sendMessage("§cKamu tidak memiliki izin untuk melakukan itu!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§cPenggunaan: /nusafishing event <start|stop|info> [event_type]");
                    return true;
                }
                
                String eventSubCommand = args[1].toLowerCase();
                
                switch (eventSubCommand) {
                    case "start":
                        if (args.length < 3) {
                            sender.sendMessage("§cPenggunaan: /nusafishing event start <event_type>");
                            sender.sendMessage("§cJenis event: LEVIATHAN_HUNT, ANCIENT_GUARDIAN, ABYSSAL_HORROR, RIVER_TITAN, GOLDEN_CATCH, COASTAL_TREASURE");
                            return true;
                        }
                        
                        String eventType = args[2].toUpperCase();
                        if (plugin.getEventManager().forceStartEvent(eventType)) {
                            sender.sendMessage("§aMemulai event " + eventType + "!");
                        } else {
                            sender.sendMessage("§cJenis event tidak valid! Gunakan: LEVIATHAN_HUNT, ANCIENT_GUARDIAN, dll.");
                        }
                        break;
                        
                    case "stop":
                        plugin.getEventManager().forceEndEvent();
                        sender.sendMessage("§aMenghentikan event yang sedang berjalan.");
                        break;
                        
                    case "info":
                        if (plugin.getEventManager().isEventActive()) {
                            EventType activeEventType = plugin.getEventManager().getActiveEventType();
                            int timeRemaining = plugin.getEventManager().getEventTimeRemaining();
                            
                            sender.sendMessage("§6§l=== Info Event ===");
                            sender.sendMessage("§fEvent aktif: §e" + activeEventType.getDisplayName());
                            sender.sendMessage("§fDeskripsi: §7" + activeEventType.getDescription());
                            sender.sendMessage("§fWaktu tersisa: §e" + formatTime(timeRemaining));
                        } else {
                            sender.sendMessage("§cTidak ada event yang sedang berlangsung.");
                        }
                        break;
                        
                    default:
                        sender.sendMessage("§cCommand tidak dikenali. Gunakan: /nusafishing event <start|stop|info>");
                }
                
                return true;
                
            default:
                sender.sendMessage("§cCommand tidak valid! Gunakan /nusafishing help untuk bantuan.");
                return true;
        }
    }
    
    /**
     * Teleport a player to a special fishing location
     * @param player The player to teleport
     * @param locationId The ID of the location
     * @return true if successful
     */
    private boolean teleportToLocation(Player player, String locationId) {
        ConfigurationSection locSection = plugin.getConfigManager().getFishingConfig()
                .getConfigurationSection("special_locations." + locationId);
        
        if (locSection == null) {
            player.sendMessage("§cLokasi dengan ID '" + locationId + "' tidak ditemukan!");
            return false;
        }
        
        double centerX = locSection.getDouble("center_x");
        double centerZ = locSection.getDouble("center_z");
        String name = locSection.getString("name", locationId);
        
        // Get current world and find suitable Y coordinate (surface)
        World world = player.getWorld();
        int centerY = world.getHighestBlockYAt((int) centerX, (int) centerZ) + 1;
        
        // Create location and teleport
        Location location = new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
        player.teleport(location);
        
        player.sendMessage("§aTeleport ke lokasi memancing '§b" + name + "§a'!");
        return true;
    }
    
    /**
     * Create a new special fishing location
     * @param player The player creating the location
     * @param locationId The ID for the new location
     * @param radius The radius of the location
     * @param name The name of the location
     * @return true if successful
     */
    private boolean createLocation(Player player, String locationId, double radius, String name) {
        // Check if location ID already exists
        if (plugin.getConfigManager().getFishingConfig().contains("special_locations." + locationId)) {
            player.sendMessage("§cLokasi dengan ID '" + locationId + "' sudah ada!");
            return false;
        }
        
        // Get player's current position
        Location playerLocation = player.getLocation();
        double centerX = playerLocation.getBlockX();
        double centerZ = playerLocation.getBlockZ();
        
        // Add new location to config
        ConfigurationSection specialLocationsSection = plugin.getConfigManager().getFishingConfig()
                .getConfigurationSection("special_locations");
        
        if (specialLocationsSection == null) {
            specialLocationsSection = plugin.getConfigManager().getFishingConfig().createSection("special_locations");
        }
        
        ConfigurationSection newLocationSection = specialLocationsSection.createSection(locationId);
        newLocationSection.set("name", name);
        newLocationSection.set("description", "Lokasi memancing spesial");
        newLocationSection.set("center_x", centerX);
        newLocationSection.set("center_z", centerZ);
        newLocationSection.set("radius", radius);
        newLocationSection.set("fish_multiplier", 1.5);
        newLocationSection.set("rare_fish_chance", 0.2);
        newLocationSection.set("special_fish", new ArrayList<String>());
        
        // Save and reload
        plugin.getConfigManager().saveConfig();
        fishingManager.reloadConfig();
        
        player.sendMessage("§aLokasi memancing spesial '§b" + name + "§a' (ID: " + locationId + ") berhasil dibuat!");
        player.sendMessage("§7Pusat: X: " + centerX + ", Z: " + centerZ + ", Radius: " + radius);
        player.sendMessage("§7Gunakan command: /nusafishing location list untuk melihat semua lokasi");
        return true;
    }
    
    /**
     * Delete a special fishing location
     * @param sender The command sender
     * @param locationId The ID of the location
     * @return true if successful
     */
    private boolean deleteLocation(CommandSender sender, String locationId) {
        // Check if location exists
        if (!plugin.getConfigManager().getFishingConfig().contains("special_locations." + locationId)) {
            sender.sendMessage("§cLokasi dengan ID '" + locationId + "' tidak ditemukan!");
            return false;
        }
        
        // Get location name before deletion
        String name = plugin.getConfigManager().getFishingConfig()
                .getString("special_locations." + locationId + ".name", locationId);
        
        // Remove from config
        plugin.getConfigManager().getFishingConfig().set("special_locations." + locationId, null);
        
        // Save and reload
        plugin.getConfigManager().saveConfig();
        fishingManager.reloadConfig();
        
        sender.sendMessage("§aLokasi memancing spesial '§b" + name + "§a' (ID: " + locationId + ") berhasil dihapus!");
        return true;
    }
    
    /**
     * Give a player a custom fishing rod
     * @param player The player to give the rod to
     * @param rodId The ID of the rod in the configuration
     * @param amount The amount of rods to give
     * @return true if successful
     */
    private boolean giveRod(Player player, String rodId, int amount) {
        FishingRod rod = plugin.getConfigManager().getFishingRods().get(rodId);
        if (rod == null) {
            player.sendMessage("§cPancingan dengan ID '" + rodId + "' tidak ditemukan!");
            return false;
        }
        
        // Create the fishing rod item
        ItemStack rodItem = new ItemBuilder(Material.FISHING_ROD)
                .name(rod.getTier().getColor() + rod.getName() + " §7[" + rod.getTier().getDisplayName() + "]")
                .lore("§8" + rod.getDescription())
                .lore("")
                .lore("§7Ketahanan: §f" + rod.getDurability() + "/" + rod.getDurability())
                .lore("§7Stamina: §f" + (rod.getStaminaModifier() < 1.0 ? 
                        "-" + (int)((1.0 - rod.getStaminaModifier()) * 100) + "%" : 
                        "+" + (int)((rod.getStaminaModifier() - 1.0) * 100) + "%"))
                .lore("§7Tier: " + rod.getTier().getColor() + rod.getTier().getDisplayName())
                .lore("§7Quality Boost: §f+" + (int)((rod.getQualityBoost() - 1.0) * 100) + "%")
                .lore("§7Lure: §f" + rod.getLureLevel())
                .lore("")
                .lore(rod.hasSpecialAbility() ? "§e✦ " + rod.getSpecialAbilityDescription() : "")
                .lore("")
                .lore("§8ID: " + rodId)
                .customModelData(rod.getCustomModelData())
                .enchant(Enchantment.LUCK, 1)
                .enchant(Enchantment.LURE, rod.getLureLevel())
                .flag(ItemFlag.HIDE_ENCHANTS)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        
        // Store rod ID and durability in persistent data
        ItemMeta meta = rodItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, ROD_ID_KEY), PersistentDataType.STRING, rodId);
        container.set(new NamespacedKey(plugin, ROD_DURABILITY_KEY), PersistentDataType.INTEGER, rod.getDurability());
        rodItem.setItemMeta(meta);
        
        // Set amount and give to player
        rodItem.setAmount(amount);
        player.getInventory().addItem(rodItem);
        
        player.sendMessage("§aKamu mendapatkan §f" + amount + "x " + rod.getTier().getColor() + rod.getName() + "§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        return true;
    }
    
    /**
     * Give a player a custom bait
     * @param player The player to give the bait to
     * @param baitId The ID of the bait in the configuration
     * @param amount The amount of baits to give
     * @return true if successful
     */
    private boolean giveBait(Player player, String baitId, int amount) {
        FishingBait bait = plugin.getConfigManager().getFishingBaits().get(baitId);
        if (bait == null) {
            player.sendMessage("§cUmpan dengan ID '" + baitId + "' tidak ditemukan!");
            return false;
        }
        
        // Create the bait item
        ItemStack baitItem = new ItemBuilder(bait.getMaterial())
                .name("§a" + bait.getName())
                .lore("§8" + bait.getDescription())
                .lore("")
                .lore("§e§l✦ Efek Umpan:")
                .lore("  §7• Daya tarik: §a+" + (int)((bait.getAttractionModifier() - 1.0) * 100) + "%")
                .lore("  §7• Boost kualitas: §a+" + (int)((bait.getQualityBoost() - 1.0) * 100) + "%")
                .lore("  §7• Boost berat: §a+" + (int)((bait.getWeightBoost() - 1.0) * 100) + "%") 
                .lore("  §7• Tier boost: §a+" + (bait.getTierBoost() * 2) + " §7tingkat")
                .lore("")
                .lore("§b§l✦ Ikan Target:")
                .lore("  §7" + (bait.getValidFish().isEmpty() ? "Semua ikan" : formatFishNames(bait.getValidFish())))
                .lore("  §7(§dBonus 10x §7untuk ikan yang sesuai)")
                .lore("")
                .lore("§8Penggunaan: §f" + bait.getUsesPerItem() + "x")
                .lore("§8ID: " + baitId)
                .customModelData(bait.getCustomModelData())
                .enchant(Enchantment.LUCK, 1)
                .flag(ItemFlag.HIDE_ENCHANTS)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        
        // Store bait ID in persistent data
        ItemMeta meta = baitItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, BAIT_ID_KEY), PersistentDataType.STRING, baitId);
        baitItem.setItemMeta(meta);
        
        // Set amount and give to player
        baitItem.setAmount(amount);
        player.getInventory().addItem(baitItem);
        
        player.sendMessage("§aKamu mendapatkan §f" + amount + "x §a" + bait.getName() + "§a!");
        return true;
    }
    
    /**
     * Format fish IDs into display names
     * @param fishIds List of fish IDs
     * @return List of formatted fish names
     */
    private List<String> formatFishNames(List<String> fishIds) {
        List<String> formattedNames = new ArrayList<>();
        for (String fishId : fishIds) {
            if (plugin.getConfigManager().getCustomFishes().containsKey(fishId)) {
                formattedNames.add(plugin.getConfigManager().getCustomFishes().get(fishId).getName());
            } else {
                formattedNames.add(fishId);
            }
        }
        return formattedNames;
    }
    
    /**
     * Repair the fishing rod in player's hand
     * @param player The player repairing the rod
     * @return true if repair successful
     */
    private boolean repairRod(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        // Check if player is holding a fishing rod
        if (heldItem.getType() != Material.FISHING_ROD) {
            player.sendMessage("§cKamu harus memegang pancingan di tangan!");
            return false;
        }
        
        // Check if it's a custom rod
        ItemMeta meta = heldItem.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) {
            player.sendMessage("§cIni bukan pancingan custom!");
            return false;
        }
        
        // Get rod info from persistent data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey rodIdKey = new NamespacedKey(plugin, ROD_ID_KEY);
        NamespacedKey durabilityKey = new NamespacedKey(plugin, ROD_DURABILITY_KEY);
        
        if (!container.has(rodIdKey, PersistentDataType.STRING) ||
            !container.has(durabilityKey, PersistentDataType.INTEGER)) {
            player.sendMessage("§cIni bukan pancingan custom yang valid!");
            return false;
        }
        
        // Get rod data
        String rodId = container.get(rodIdKey, PersistentDataType.STRING);
        int currentDurability = container.get(durabilityKey, PersistentDataType.INTEGER);
        
        FishingRod rod = plugin.getFishingManager().getRodById(rodId);
        if (rod == null) {
            player.sendMessage("§cTidak dapat menemukan informasi pancingan!");
            return false;
        }
        
        // Calculate repair cost based on how damaged the rod is
        int maxDurability = rod.getDurability();
        int durabilityLost = maxDurability - currentDurability;
        
        if (durabilityLost <= 0) {
            player.sendMessage("§cPancingan ini tidak perlu diperbaiki!");
            return false;
        }
        
        // Calculate repair cost
        EconomyManager economy = plugin.getEconomyManager();
        double repairCost = economy.getRepairCost(durabilityLost);
        
        // Check if economy is enabled and player has enough money
        if (economy.isEconomyEnabled()) {
            if (!economy.hasMoney(player, repairCost)) {
                player.sendMessage("§cKamu tidak memiliki cukup uang untuk memperbaiki pancingan ini!");
                player.sendMessage("§cBiaya perbaikan: §f" + economy.formatMoney(repairCost));
                return false;
            }
            
            // Charge the player
            economy.withdrawMoney(player, repairCost);
            player.sendMessage("§aKamu membayar §f" + economy.formatMoney(repairCost) + 
                    " §auntuk memperbaiki pancingan.");
        }
        
        // Try to repair the rod
        if (FishingListener.repairRod(player, heldItem)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage("§a§lBerhasil! §fPancingan telah diperbaiki.");
            return true;
        } else {
            // Refund money if repair failed
            if (economy.isEconomyEnabled()) {
                economy.depositMoney(player, repairCost);
            }
            player.sendMessage("§cTidak dapat memperbaiki pancingan ini.");
            return false;
        }
    }
    
    /**
     * Sell fish from player's inventory
     * @param player The player selling fish
     * @param sellAll Whether to sell all fish or only the one in hand
     * @return true if successful
     */
    private boolean sellFish(Player player, boolean sellAll) {
        EconomyManager economy = plugin.getEconomyManager();
        
        if (!economy.isEconomyEnabled()) {
            player.sendMessage("§cFitur ekonomi tidak aktif!");
            return false;
        }
        
        double totalEarnings = 0;
        int totalFishSold = 0;
        
        if (sellAll) {
            // Sell all fish in inventory
            ItemStack[] contents = player.getInventory().getContents();
            Map<String, Integer> fishSold = new HashMap<>();
            
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null) continue;
                
                CustomFish fish = getFishFromItem(item);
                if (fish == null) continue;
                
                double price = economy.calculateFishPrice(fish);
                double earnings = price * item.getAmount();
                totalEarnings += earnings;
                totalFishSold += item.getAmount();
                
                // Keep track of fish types sold for price dynamics
                fishSold.put(fish.getId(), fishSold.getOrDefault(fish.getId(), 0) + item.getAmount());
                
                // Remove the fish from inventory
                player.getInventory().setItem(i, null);
            }
            
            // Update dynamic prices for all fish sold
            for (Map.Entry<String, Integer> entry : fishSold.entrySet()) {
                economy.updateDynamicPrice(entry.getKey(), entry.getValue());
            }
        } else {
            // Sell only the fish in hand
            ItemStack handItem = player.getInventory().getItemInMainHand();
            
            if (handItem.getType() == Material.AIR) {
                player.sendMessage("§cKamu tidak memegang ikan!");
                return false;
            }
            
            CustomFish fish = getFishFromItem(handItem);
            if (fish == null) {
                player.sendMessage("§cItem yang kamu pegang bukan ikan hasil pancingan!");
                return false;
            }
            
            double price = economy.calculateFishPrice(fish);
            totalEarnings = price * handItem.getAmount();
            totalFishSold = handItem.getAmount();
            
            // Update dynamic price for this fish
            economy.updateDynamicPrice(fish.getId(), handItem.getAmount());
            
            // Remove the fish from hand
            player.getInventory().setItemInMainHand(null);
        }
        
        if (totalFishSold == 0) {
            player.sendMessage("§cTidak ada ikan yang dapat dijual!");
            return false;
        }
        
        // Add money to player
        economy.depositMoney(player, totalEarnings);
        
        // Notify player
        player.sendMessage("§a§lBerhasil! §f" + 
                "Kamu menjual §b" + totalFishSold + " ikan§f dengan total " + 
                "§a" + economy.formatMoney(totalEarnings) + "§f.");
        
        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        return true;
    }
    
    /**
     * Get CustomFish information from an ItemStack
     * @param item The item to check
     * @return The CustomFish data, or null if not a fish
     */
    private CustomFish getFishFromItem(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey fishIdKey = new NamespacedKey(plugin, "fish_id");
        NamespacedKey weightKey = new NamespacedKey(plugin, "fish_weight");
        NamespacedKey qualityKey = new NamespacedKey(plugin, "fish_quality");

        if (!container.has(fishIdKey, PersistentDataType.STRING)) return null;

        String fishId = container.get(fishIdKey, PersistentDataType.STRING);
        CustomFish templateFish = plugin.getConfigManager().getCustomFishes().get(fishId);
        if (templateFish == null) return null;

        CustomFish fish = new CustomFish(
            templateFish.getId(),
            templateFish.getName(),
            templateFish.getDescription(),
            templateFish.getMaterial(),
            templateFish.getCustomModelData(),
            templateFish.getMinWeight(),
            templateFish.getMaxWeight(),
            templateFish.getMinQuality(),
            templateFish.getMaxQuality(),
            templateFish.getBasePrice(),
            templateFish.getBaseTier() // Pass the base tier from template fish
        );

        double weight = container.getOrDefault(weightKey, PersistentDataType.DOUBLE, templateFish.getMinWeight());
        int quality = container.getOrDefault(qualityKey, PersistentDataType.INTEGER, templateFish.getMinQuality());
        
        // Baca data tier yang tersimpan (atau gunakan base tier jika tidak ada)
        NamespacedKey tierKey = new NamespacedKey(plugin, "fish_tier");
        int tierOrdinal = container.getOrDefault(tierKey, PersistentDataType.INTEGER, templateFish.getBaseTier().ordinal());
        CustomFish.FishTier actualTier = CustomFish.FishTier.values()[tierOrdinal];

        try {
            java.lang.reflect.Field weightField = CustomFish.class.getDeclaredField("weight");
            java.lang.reflect.Field qualityField = CustomFish.class.getDeclaredField("quality");
            java.lang.reflect.Field tierField = CustomFish.class.getDeclaredField("actualTier");
            
            weightField.setAccessible(true);
            qualityField.setAccessible(true);
            tierField.setAccessible(true);
            
            weightField.set(fish, weight);
            qualityField.set(fish, quality);
            tierField.set(fish, actualTier); // Tambahkan ini untuk mengatur tier
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting fish properties: " + e.getMessage());
        }

        return fish;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "rod", "bait", "repair"));
            
            if (sender.hasPermission("nusafishing.sell")) {
                subCommands.add("sell");
            }
            
            if (sender.hasPermission("nusafishing.admin")) {
                subCommands.add("reload");
                subCommands.add("location");
            }
            
            if (sender.hasPermission("nusafishing.shop")) {
                subCommands.add("shop");
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("sell") && sender.hasPermission("nusafishing.sell")) {
                completions.add("all");
            } else if (args[0].equalsIgnoreCase("rod") && sender.hasPermission("nusafishing.rod")) {
                // Rod tab completion logic (existing code)
            } else if (args[0].equalsIgnoreCase("event")) {
                return Arrays.asList("start", "stop", "info").stream()
                       .filter(s -> s.startsWith(args[1].toLowerCase()))
                       .collect(Collectors.toList());
            }
            // Rest of the tab completion logic
        } else if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
            return Arrays.stream(EventType.values())
                   .map(Enum::name)
                   .filter(s -> s.startsWith(args[2].toUpperCase()))
                   .collect(Collectors.toList());
        }
        
        return completions;
    }
    
    /**
     * Helper method to format time in seconds to mm:ss
     * @param seconds The time in seconds
     * @return Formatted time string
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}