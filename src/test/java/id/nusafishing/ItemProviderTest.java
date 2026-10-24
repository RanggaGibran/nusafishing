package id.nusafishing;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import id.nusafishing.compat.CompatibilityHelper;
import id.nusafishing.compat.CustomModelDataHelper;
import id.nusafishing.compat.PlatformHelper;
import id.nusafishing.fishing.model.CustomFish;
import id.nusafishing.integration.item.ItemProviderManager;
import id.nusafishing.integration.item.ItemTextureProvider;
import id.nusafishing.integration.item.VanillaItemTextureProvider;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class ItemProviderTest {
    private ServerMock server;
    private org.bukkit.plugin.Plugin mockPlugin;
    private ItemProviderManager providerManager;

    @Before
    public void setUp() throws Exception {
        server = MockBukkit.mock();
        
        mockPlugin = (org.bukkit.plugin.Plugin) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.plugin.Plugin.class.getClassLoader(),
                new Class<?>[]{org.bukkit.plugin.Plugin.class},
                (proxy, method, args) -> {
                    if ("getLogger".equals(method.getName())) {
                        return java.util.logging.Logger.getLogger("nusafishing-test");
                    }
                    if ("getName".equals(method.getName())) {
                        return "nusafishing";
                    }
                    return null;
                }
        );
        providerManager = new ItemProviderManager(mockPlugin);

        // Inject instance and itemProviderManager into NusaFishing for static access in createItemStack()
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        NusaFishing nusaInstance = (NusaFishing) unsafe.allocateInstance(NusaFishing.class);
        Field instanceField = NusaFishing.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, nusaInstance);

        Field managerField = NusaFishing.class.getDeclaredField("itemProviderManager");
        managerField.setAccessible(true);
        managerField.set(nusaInstance, providerManager);
    }

    @After
    public void tearDown() throws Exception {
        Field instanceField = NusaFishing.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        MockBukkit.unmock();
    }

    @Test
    public void testVanillaProvider() {
        VanillaItemTextureProvider provider = new VanillaItemTextureProvider();
        assertEquals("vanilla", provider.getProviderName());
        assertTrue(provider.isAvailable());

        ItemStack item = provider.buildItem("SALMON");
        assertNotNull(item);
        assertEquals(Material.SALMON, item.getType());

        assertNull(provider.buildItem(null));
        assertNull(provider.buildItem("INVALID_MATERIAL_NAME_XYZ"));
    }

    @Test
    public void testFallbackWhenProviderAbsent() {
        // Nexo is not installed on this mock server
        ItemStack fallbackItem = providerManager.resolveBaseItem("nexo", "non_existent_fish", Material.PUFFERFISH, 1234);
        assertNotNull(fallbackItem);
        assertEquals(Material.PUFFERFISH, fallbackItem.getType());

        ItemMeta meta = fallbackItem.getItemMeta();
        assertNotNull(meta);
        if (meta.hasCustomModelData()) {
            assertEquals(1234, meta.getCustomModelData());
        }

        // Oraxen fallback
        ItemStack oraxenFallback = providerManager.resolveBaseItem("oraxen", "sample_bass", Material.SALMON, 4321);
        assertNotNull(oraxenFallback);
        assertEquals(Material.SALMON, oraxenFallback.getType());

        // ItemsAdder fallback
        ItemStack iaFallback = providerManager.resolveBaseItem("itemsadder", "pack:fish", Material.COD, 5678);
        assertNotNull(iaFallback);
        assertEquals(Material.COD, iaFallback.getType());
    }

    @Test
    public void testCustomProviderResolution() {
        // Register a mock custom provider
        providerManager.registerProvider(new ItemTextureProvider() {
            @Override
            public String getProviderName() {
                return "mock_provider";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public ItemStack buildItem(String itemId) {
                if ("custom_tuna".equals(itemId)) {
                    ItemStack mockStack = new ItemStack(Material.TROPICAL_FISH);
                    ItemMeta meta = mockStack.getItemMeta();
                    meta.setDisplayName("§eMock Tuna Base");
                    mockStack.setItemMeta(meta);
                    return mockStack;
                }
                return null;
            }
        });

        // Resolve item from the custom provider
        ItemStack resolved = providerManager.resolveBaseItem("mock_provider", "custom_tuna", Material.COD, 0);
        assertNotNull(resolved);
        assertEquals(Material.TROPICAL_FISH, resolved.getType());
    }

    @Test
    public void testCustomFishItemStackCreationWithPDC() {
        CustomFish fish = new CustomFish(
                "test_golden_fish",
                "Golden Fish",
                "A gleaming golden fish",
                Material.SALMON,
                9999,
                "nexo",
                "golden_fish_model",
                2.0,
                10.0,
                1,
                100,
                50.0,
                CustomFish.FishTier.RARE
        );

        CustomFish caught = fish.createCaughtInstance(1, 1.2, 1.1, 10.0);
        assertNotNull(caught);
        assertEquals("nexo", caught.getTextureProvider());
        assertEquals("golden_fish_model", caught.getTextureId());

        ItemStack fishItem = caught.createItemStack();
        assertNotNull(fishItem);
        // Because Nexo is not installed on mock server, it falls back to SALMON
        assertEquals(Material.SALMON, fishItem.getType());

        ItemMeta meta = fishItem.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.getDisplayName().contains("Golden Fish"));
        assertNotNull(meta.getLore());

        // Verify PDC persistence
        NamespacedKey fishIdKey = NamespacedKey.fromString("nusafishing:fish_id");
        NamespacedKey fishWeightKey = NamespacedKey.fromString("nusafishing:fish_weight");
        NamespacedKey fishQualityKey = NamespacedKey.fromString("nusafishing:fish_quality");
        NamespacedKey fishTierKey = NamespacedKey.fromString("nusafishing:fish_tier");

        assertEquals("test_golden_fish", meta.getPersistentDataContainer().get(fishIdKey, PersistentDataType.STRING));
        assertNotNull(meta.getPersistentDataContainer().get(fishWeightKey, PersistentDataType.DOUBLE));
        assertNotNull(meta.getPersistentDataContainer().get(fishQualityKey, PersistentDataType.INTEGER));
        assertNotNull(meta.getPersistentDataContainer().get(fishTierKey, PersistentDataType.INTEGER));
    }

    @Test
    public void testCustomModelDataHelper() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);

        CustomModelDataHelper.applyCustomModelData(meta, 4321);
        item.setItemMeta(meta);

        ItemMeta updatedMeta = item.getItemMeta();
        assertNotNull(updatedMeta);
        assertTrue(updatedMeta.hasCustomModelData());
        assertEquals(4321, updatedMeta.getCustomModelData());
    }

    @Test
    public void testCompatibilityHelper() {
        assertNotNull("Bubble particle should resolve", CompatibilityHelper.BUBBLE);
        assertNotNull("Splash particle should resolve", CompatibilityHelper.SPLASH);
        assertNotNull("Totem particle should resolve", CompatibilityHelper.TOTEM);
        assertNotNull("Witch particle should resolve", CompatibilityHelper.WITCH);
        assertNotNull("Luck enchantment should resolve", CompatibilityHelper.getLuckEnchantment());
    }

    @Test
    public void testPlatformHelper() {
        int release = PlatformHelper.getJavaRelease();
        assertTrue("Java release must be 21 or 25", release == 21 || release == 25);
        assertNotNull(PlatformHelper.getRuntimeStrategy());
    }

    @Test
    public void testFishingRodTextureSupport() {
        id.nusafishing.fishing.model.FishingRod rod = new id.nusafishing.fishing.model.FishingRod(
                "custom_nexo_rod",
                "Carbon Rod",
                "Advanced carbon fishing rod",
                Material.FISHING_ROD,
                2020,
                "nexo",
                "nexo_carbon_rod",
                300,
                0.5,
                id.nusafishing.fishing.model.FishingRod.RodTier.ADVANCED,
                true,
                "Double catch chance",
                15000.0,
                3,
                500.0
        );

        assertEquals("nexo", rod.getTextureProvider());
        assertEquals("nexo_carbon_rod", rod.getTextureId());

        // Test createItemStack with fallback to Material.FISHING_ROD when Nexo is absent
        ItemStack item = rod.createItemStack();
        assertNotNull(item);
        assertEquals(Material.FISHING_ROD, item.getType());

        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta.getDisplayName().contains("Carbon Rod"));

        // Verify PDC persistence
        NamespacedKey rodIdKey = NamespacedKey.fromString("nusafishing:rod_id");
        NamespacedKey durabilityKey = NamespacedKey.fromString("nusafishing:durability");
        assertEquals("custom_nexo_rod", meta.getPersistentDataContainer().get(rodIdKey, PersistentDataType.STRING));
        assertEquals(Integer.valueOf(300), meta.getPersistentDataContainer().get(durabilityKey, PersistentDataType.INTEGER));

        // Test display item for shop
        ItemStack displayItem = rod.createDisplayItem(15000.0, true);
        assertNotNull(displayItem);
        ItemMeta displayMeta = displayItem.getItemMeta();
        assertNotNull(displayMeta);
        assertTrue(displayMeta.getDisplayName().contains("Carbon Rod"));

        // Test custom provider resolution for rod
        providerManager.registerProvider(new ItemTextureProvider() {
            @Override
            public String getProviderName() {
                return "rod_mock_provider";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public ItemStack buildItem(String itemId) {
                if ("special_rod_id".equals(itemId)) {
                    ItemStack mock = new ItemStack(Material.FISHING_ROD);
                    ItemMeta m = mock.getItemMeta();
                    m.setDisplayName("§bMock Special Rod Texture");
                    mock.setItemMeta(m);
                    return mock;
                }
                return null;
            }
        });

        id.nusafishing.fishing.model.FishingRod mockRod = new id.nusafishing.fishing.model.FishingRod(
                "mock_special_rod",
                "Mock Special Rod",
                "Rod with mock texture provider",
                Material.FISHING_ROD,
                0,
                "rod_mock_provider",
                "special_rod_id",
                500,
                0.4,
                id.nusafishing.fishing.model.FishingRod.RodTier.EXPERT,
                false,
                "",
                25000.0,
                4,
                1000.0
        );

        ItemStack resolvedRod = mockRod.createItemStack();
        assertNotNull(resolvedRod);
        ItemMeta resolvedMeta = resolvedRod.getItemMeta();
        assertNotNull(resolvedMeta);
        assertTrue(resolvedMeta.getDisplayName().contains("Mock Special Rod"));
        assertEquals("mock_special_rod", resolvedMeta.getPersistentDataContainer().get(rodIdKey, PersistentDataType.STRING));
    }
}
