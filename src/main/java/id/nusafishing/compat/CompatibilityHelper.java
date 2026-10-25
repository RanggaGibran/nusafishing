package id.nusafishing.compat;

import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;

/**
 * Cross-version compatibility helper for Bukkit/Spigot/Paper across 1.21 to 26.2+.
 */
public final class CompatibilityHelper {
    private CompatibilityHelper() {}

    public static final Particle BUBBLE = resolveParticle("BUBBLE", "WATER_BUBBLE");
    public static final Particle SPLASH = resolveParticle("SPLASH", "WATER_SPLASH");
    public static final Particle TOTEM = resolveParticle("TOTEM_OF_UNDYING", "TOTEM");
    public static final Particle WITCH = resolveParticle("WITCH", "SPELL_WITCH");

    private static Particle resolveParticle(String modern, String legacy) {
        try {
            return Particle.valueOf(modern);
        } catch (IllegalArgumentException e1) {
            try {
                return Particle.valueOf(legacy);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    public static Enchantment getLuckEnchantment() {
        try {
            return Enchantment.LUCK_OF_THE_SEA;
        } catch (Throwable t) {
            try {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
                if (ench != null) return ench;
            } catch (Throwable ignored) {}
            return null;
        }
    }

    public static void sendActionBar(org.bukkit.entity.Player player, String message) {
        if (player == null || message == null) return;
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Throwable t1) {
            try {
                player.sendTitle("", message, 0, 40, 10);
            } catch (Throwable ignored) {}
        }
    }
}
