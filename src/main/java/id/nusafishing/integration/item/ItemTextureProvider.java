package id.nusafishing.integration.item;

import org.bukkit.inventory.ItemStack;

/**
 * Common abstraction for custom item/texture providers (Vanilla, Nexo, Oraxen, ItemsAdder).
 */
public interface ItemTextureProvider {
    /**
     * Name identifier of the provider ("vanilla", "nexo", "oraxen", "itemsadder").
     *
     * @return provider identifier string
     */
    String getProviderName();

    /**
     * Checks if the underlying plugin is present, enabled, and custom items are loaded.
     *
     * @return true if ready to create items
     */
    boolean isAvailable();

    /**
     * Resolves and builds an ItemStack for the given item ID.
     *
     * @param itemId The provider's custom item identifier
     * @return The created ItemStack, or null if item cannot be resolved
     */
    ItemStack buildItem(String itemId);
}
