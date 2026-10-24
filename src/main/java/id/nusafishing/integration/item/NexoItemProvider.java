package id.nusafishing.integration.item;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Item texture provider for Nexo custom items.
 * Uses NexoItems.itemFromId(id).build() and listens for NexoItemsLoadedEvent.
 */
public class NexoItemProvider implements ItemTextureProvider, Listener {
    private final Plugin plugin;
    private volatile boolean itemsLoaded = false;

    public NexoItemProvider(Plugin plugin) {
        this.plugin = plugin;
        try {
            // Check if items are already loaded
            if (NexoItems.itemNames() != null && !NexoItems.itemNames().isEmpty()) {
                this.itemsLoaded = true;
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNexoItemsLoaded(NexoItemsLoadedEvent event) {
        this.itemsLoaded = true;
        plugin.getLogger().info("[NexoItemProvider] Nexo custom items loaded successfully.");
    }

    @Override
    public String getProviderName() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Nexo") && itemsLoaded;
    }

    @Override
    public ItemStack buildItem(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) {
            return null;
        }
        try {
            var builder = NexoItems.itemFromId(itemId);
            if (builder != null) {
                return builder.build();
            } else {
                plugin.getLogger().warning("[NexoItemProvider] Item ID not found in Nexo: " + itemId);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[NexoItemProvider] Error resolving Nexo item '" + itemId + "': " + t.getMessage());
        }
        return null;
    }
}
