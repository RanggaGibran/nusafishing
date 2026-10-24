package id.nusafishing.integration.item;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Item texture provider for ItemsAdder custom items.
 * Uses CustomStack.getInstance("namespace:id").getItemStack() and listens for ItemsAdderLoadDataEvent.
 */
public class ItemsAdderItemProvider implements ItemTextureProvider, Listener {
    private final Plugin plugin;
    private volatile boolean itemsLoaded = false;

    public ItemsAdderItemProvider(Plugin plugin) {
        this.plugin = plugin;
        try {
            // Check if ItemsAdder items are already loaded
            if (ItemsAdder.areItemsLoaded()) {
                this.itemsLoaded = true;
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent event) {
        this.itemsLoaded = true;
        plugin.getLogger().info("[ItemsAdderItemProvider] ItemsAdder custom items loaded successfully.");
    }

    @Override
    public String getProviderName() {
        return "itemsadder";
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder") && itemsLoaded;
    }

    @Override
    public ItemStack buildItem(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) {
            return null;
        }
        try {
            CustomStack customStack = CustomStack.getInstance(itemId);
            if (customStack != null) {
                return customStack.getItemStack();
            } else {
                plugin.getLogger().warning("[ItemsAdderItemProvider] Item ID not found in ItemsAdder: " + itemId);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[ItemsAdderItemProvider] Error resolving ItemsAdder item '" + itemId + "': " + t.getMessage());
        }
        return null;
    }
}
