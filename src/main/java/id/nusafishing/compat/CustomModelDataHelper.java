package id.nusafishing.compat;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles custom model data across Minecraft versions:
 * - On 1.21.4+ and 26.2+: Uses CustomModelDataComponent
 * - On older versions (1.20 - 1.21.3): Uses legacy integer CustomModelData
 */
public final class CustomModelDataHelper {
    private static final boolean HAS_COMPONENT_API;
    private static Method GET_COMPONENT_METHOD;
    private static Method SET_COMPONENT_METHOD;
    private static Method SET_FLOATS_METHOD;

    static {
        boolean componentApiAvailable = false;
        try {
            Class<?> metaClass = ItemMeta.class;
            Class<?> componentClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            GET_COMPONENT_METHOD = metaClass.getMethod("getCustomModelDataComponent");
            SET_COMPONENT_METHOD = metaClass.getMethod("setCustomModelDataComponent", componentClass);
            SET_FLOATS_METHOD = componentClass.getMethod("setFloats", List.class);
            componentApiAvailable = true;
        } catch (Throwable ignored) {
            componentApiAvailable = false;
        }
        HAS_COMPONENT_API = componentApiAvailable;
    }

    private CustomModelDataHelper() {}

    /**
     * Checks if the modern CustomModelDataComponent API is active on this server.
     */
    public static boolean hasComponentApi() {
        return HAS_COMPONENT_API;
    }

    /**
     * Applies custom model data to an ItemMeta instance safely.
     *
     * @param meta The item meta to modify
     * @param customModelData The model data integer
     */
    public static void applyCustomModelData(ItemMeta meta, int customModelData) {
        if (meta == null || customModelData <= 0) {
            return;
        }

        // On 1.21.4+ / 26.x, use CustomModelDataComponent
        if (HAS_COMPONENT_API) {
            try {
                Object component = GET_COMPONENT_METHOD.invoke(meta);
                if (component != null) {
                    SET_FLOATS_METHOD.invoke(component, Collections.singletonList((float) customModelData));
                    SET_COMPONENT_METHOD.invoke(meta, component);
                    return;
                }
            } catch (Throwable ignored) {
                // Fall back to integer method if component invocation encounters any issue
            }
        }

        // Fallback for pre-1.21.4 versions or fallback on error
        try {
            meta.setCustomModelData(customModelData);
        } catch (Throwable ignored) {
        }
    }
}
