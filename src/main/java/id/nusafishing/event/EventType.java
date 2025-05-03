package id.nusafishing.event;

import java.util.Random;

public enum EventType {
    LEVIATHAN_HUNT("Perburuan Leviathan", "Leviathan kuno telah terbangun dan muncul ke permukaan!"),
    ANCIENT_GUARDIAN("Penjaga Kuno", "Penjaga kuno dari kedalaman laut telah terlihat di dekat sini!"),
    ABYSSAL_HORROR("Horor Palung", "Makhluk mengerikan dari palung terdalam telah muncul!"),
    RIVER_TITAN("Titan Sungai", "Ikan raksasa legendaris telah terlihat di sungai!"),
    GOLDEN_CATCH("Tangkapan Emas", "Ikan-ikan langka dengan sisik emas telah bermigrasi ke perairan ini!"),
    COASTAL_TREASURE("Harta Pantai", "Makhluk laut legendaris telah terlihat di pantai!");

    private final String displayName;
    private final String description;
    private static final Random random = new Random();

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
     * @return An EventFish object
     */
    public EventFish getRandomEventFish() {
        switch (this) {
            case LEVIATHAN_HUNT:
                return EventFish.values()[random.nextInt(2)]; // First 2 fish
            case ANCIENT_GUARDIAN:
                return EventFish.values()[random.nextInt(2) + 2]; // Next 2 fish
            case ABYSSAL_HORROR:
                return EventFish.values()[random.nextInt(2) + 4]; // Next 2 fish
            case RIVER_TITAN:
                return EventFish.values()[random.nextInt(2) + 6]; // Next 2 fish
            case GOLDEN_CATCH:
                return EventFish.values()[random.nextInt(2) + 8]; // Next 2 fish
            case COASTAL_TREASURE:
                return EventFish.values()[random.nextInt(2) + 10]; // Next 2 fish
            default:
                return EventFish.ANCIENT_LEVIATHAN; // Default
        }
    }
}