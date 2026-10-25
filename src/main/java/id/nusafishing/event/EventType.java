package id.nusafishing.event;

public enum EventType {
    LEVIATHAN_HUNT("Perburuan Leviathan", "Leviathan kuno telah terbangun dan muncul ke permukaan!"),
    ANCIENT_GUARDIAN("Penjaga Kuno", "Penjaga kuno dari kedalaman laut telah terlihat di dekat sini!"),
    ABYSSAL_HORROR("Horor Palung", "Makhluk mengerikan dari palung terdalam telah muncul!"),
    RIVER_TITAN("Titan Sungai", "Ikan raksasa legendaris telah terlihat di sungai!"),
    GOLDEN_CATCH("Tangkapan Emas", "Ikan-ikan langka dengan sisik emas telah bermigrasi ke perairan ini!"),
    COASTAL_TREASURE("Harta Pantai", "Makhluk laut legendaris telah terlihat di pantai!");

    private final String displayName;
    private final String description;

    EventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get a random event fish appropriate for this event type
     * @return An ConfigEventFish object or null if none available
     */
    public ConfigEventFish getRandomEventFish() {
        return ConfigEventFish.getRandomFishForEvent(this);
    }
}