package id.nusafishing.integration.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Default fallback provider using vanilla Minecraft materials and CustomModelData.
 */
public class VanillaItemTextureProvider implements ItemTextureProvider {
    @Override
    public String getProviderName() {
        return "vanilla";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ItemStack buildItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        Material material = Material.getMaterial(itemId.toUpperCase());
        if (material != null && !material.isAir()) {
            return new ItemStack(material);
        }
        return null;
    }
}
