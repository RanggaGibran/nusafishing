package id.nusafishing.integration.item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import id.nusafishing.compat.CustomModelDataHelper;

/**
 * Manages item/texture providers and resolves custom fish base items with safe fallbacks.
 */
public class ItemProviderManager {
    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, ItemTextureProvider> providers = new HashMap<>();

    public ItemProviderManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        registerProviders();
    }

    private void registerProviders() {
        // Always register vanilla provider
        registerProvider(new VanillaItemTextureProvider());

        // Register Nexo if present
        if (Bukkit.getPluginManager().getPlugin("Nexo") != null) {
            try {
                registerProvider(new NexoItemProvider(plugin));
                logger.info("[ItemProviderManager] Hooked into Nexo for custom fish textures.");
            } catch (Throwable t) {
                logger.warning("[ItemProviderManager] Failed to initialize Nexo provider: " + t.getMessage());
            }
        }

        // Register Oraxen if present
        if (Bukkit.getPluginManager().getPlugin("Oraxen") != null) {
            try {
                registerProvider(new OraxenItemProvider(plugin));
                logger.info("[ItemProviderManager] Hooked into Oraxen for custom fish textures.");
            } catch (Throwable t) {
                logger.warning("[ItemProviderManager] Failed to initialize Oraxen provider: " + t.getMessage());
            }
        }

        // Register ItemsAdder if present
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                registerProvider(new ItemsAdderItemProvider(plugin));
                logger.info("[ItemProviderManager] Hooked into ItemsAdder for custom fish textures.");
            } catch (Throwable t) {
                logger.warning("[ItemProviderManager] Failed to initialize ItemsAdder provider: " + t.getMessage());
            }
        }
    }

    public void registerProvider(ItemTextureProvider provider) {
        if (provider != null) {
            providers.put(provider.getProviderName().toLowerCase(), provider);
        }
    }

    public ItemTextureProvider getProvider(String providerName) {
        if (providerName == null) return null;
        return providers.get(providerName.trim().toLowerCase());
    }

    public Map<String, ItemTextureProvider> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    /**
     * Resolves an item using the requested provider and item ID.
     * If the provider is unavailable or fails, automatically falls back to vanilla behavior.
     *
     * @param textureProvider Provider name ("nexo", "oraxen", "itemsadder", "vanilla")
     * @param textureId Provider-specific item ID
     * @param fallbackMaterial Material to use if provider fails or vanilla is requested
     * @param customModelData Custom model data to apply for vanilla fallback
     * @return Resolved ItemStack ready for custom fish meta application
     */
    public ItemStack resolveBaseItem(String textureProvider, String textureId, Material fallbackMaterial, int customModelData) {
        String providerKey = (textureProvider != null && !textureProvider.isBlank())
                ? textureProvider.trim().toLowerCase()
                : "vanilla";

        if (!providerKey.equals("vanilla") && textureId != null && !textureId.isBlank()) {
            ItemTextureProvider provider = providers.get(providerKey);
            if (provider != null) {
                if (provider.isAvailable()) {
                    ItemStack customItem = provider.buildItem(textureId);
                    if (customItem != null) {
                        return customItem.clone();
                    } else {
                        logger.fine(() -> "[ItemProviderManager] Provider '" + providerKey + "' returned null for item '" + textureId + "', using vanilla fallback.");
                    }
                } else {
                    logger.fine(() -> "[ItemProviderManager] Provider '" + providerKey + "' requested for item '" + textureId + "' is not yet available, using vanilla fallback.");
                }
            } else {
                logger.fine(() -> "[ItemProviderManager] Provider '" + providerKey + "' not found on server, using vanilla fallback.");
            }
        }

        // Vanilla fallback
        Material mat = (fallbackMaterial != null && !fallbackMaterial.isAir()) ? fallbackMaterial : Material.COD;
        ItemStack vanillaItem = new ItemStack(mat);
        if (customModelData > 0) {
            ItemMeta meta = vanillaItem.getItemMeta();
            if (meta != null) {
                CustomModelDataHelper.applyCustomModelData(meta, customModelData);
                vanillaItem.setItemMeta(meta);
            }
        }
        return vanillaItem;
    }
}
