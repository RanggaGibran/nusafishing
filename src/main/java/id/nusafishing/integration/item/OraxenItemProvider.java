package id.nusafishing.integration.item;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Item texture provider for Oraxen custom items.
 * Uses OraxenItems.getItemById(id).build() and listens for OraxenItemsLoadedEvent.
 */
public class OraxenItemProvider implements ItemTextureProvider, Listener {
    private final Plugin plugin;
    private volatile boolean itemsLoaded = false;

    public OraxenItemProvider(Plugin plugin) {
        this.plugin = plugin;
        try {
            // Check if items are already loaded
            if (OraxenItems.getNames() != null && !OraxenItems.getNames().isEmpty()) {
                this.itemsLoaded = true;
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onOraxenItemsLoaded(OraxenItemsLoadedEvent event) {
        this.itemsLoaded = true;
        plugin.getLogger().info("[OraxenItemProvider] Oraxen custom items loaded successfully.");
    }

    @Override
    public String getProviderName() {
        return "oraxen";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Oraxen") && itemsLoaded;
    }

    @Override
    public ItemStack buildItem(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) {
            return null;
        }
        try {
            var builder = OraxenItems.getItemById(itemId);
            if (builder != null) {
                return builder.build();
            } else {
                plugin.getLogger().warning("[OraxenItemProvider] Item ID not found in Oraxen: " + itemId);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[OraxenItemProvider] Error resolving Oraxen item '" + itemId + "': " + t.getMessage());
        }
        return null;
    }
}
