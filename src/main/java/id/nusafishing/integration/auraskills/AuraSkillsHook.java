package id.nusafishing.integration.auraskills;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import id.nusafishing.NusaFishing;
import id.nusafishing.fishing.model.CustomFish;

public class AuraSkillsHook {
    private final NusaFishing plugin;
    private boolean available = false;

    public AuraSkillsHook(NusaFishing plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    public void checkAvailability() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) {
                Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                available = true;
                plugin.getLogger().info("AuraSkills terdeteksi dan berhasil dihubungkan!");
            } else {
                available = false;
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isEnabledInConfig() {
        return plugin.getConfig().getBoolean("auraskills.enabled", true);
    }

    /**
     * Add AuraSkills fishing experience to player upon catching a fish.
     *
     * @param player The player who caught the fish
     * @param fish   The fish that was caught
     */
    public void addFishingXp(Player player, CustomFish fish) {
        if (!available || !isEnabledInConfig() || player == null || fish == null) {
            return;
        }

        try {
            double baseXp = plugin.getConfig().getDouble("auraskills.base_xp", 15.0);
            
            // Tier multiplier
            String tierKey = fish.getActualTier() != null ? fish.getActualTier().name() : "COMMON";
            double tierMultiplier = plugin.getConfig().getDouble("auraskills.tier_xp_multipliers." + tierKey, 1.0);
            
            // Weight & quality scaling
            double qualityFactor = Math.max(0.5, fish.getQuality() / 50.0);
            double weightFactor = Math.min(3.0, 1.0 + Math.log10(Math.max(1.0, fish.getWeight())));
            
            double totalXp = (baseXp + (weightFactor * qualityFactor * 2.0)) * tierMultiplier;
            
            AuraSkillsApi api = AuraSkillsApi.get();
            if (api != null) {
                SkillsUser user = api.getUser(player.getUniqueId());
                if (user != null) {
                    user.addSkillXp(Skills.FISHING, totalXp);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Gagal menambahkan XP AuraSkills: " + t.getMessage());
        }
    }
}
