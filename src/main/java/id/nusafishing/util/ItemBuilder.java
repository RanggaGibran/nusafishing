package id.nusafishing.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final List<String> lore = new ArrayList<>();
    
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
        if (meta != null && meta.getLore() != null) {
            this.lore.addAll(meta.getLore());
        }
    }
    
    public ItemBuilder name(String name) {
        meta.setDisplayName(name);
        return this;
    }
    
    public ItemBuilder lore(String line) {
        lore.add(line);
        return this;
    }
    
    public ItemBuilder lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }
    
    public ItemBuilder lore(List<String> lines) {
        lore.addAll(lines);
        return this;
    }
    
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }
    
    public ItemBuilder flag(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }
    
    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }
    
    public ItemBuilder customModelData(int data) {
        if (data > 0 && meta != null) {
            id.nusafishing.compat.CustomModelDataHelper.applyCustomModelData(meta, data);
        }
        return this;
    }
    
    public ItemStack build() {
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
}