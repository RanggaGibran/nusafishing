package id.nusafishing;

import org.junit.Test;
import static org.junit.Assert.*;

import id.nusafishing.fishing.afk.AfkFishingManager;

public class AfkAndAuraSkillsTest {

    @Test
    public void testPercentParsing() {
        assertEquals(0.20, AfkFishingManager.parsePercent("20%", 0.10), 0.0001);
        assertEquals(0.35, AfkFishingManager.parsePercent("35%", 0.10), 0.0001);
        assertEquals(0.50, AfkFishingManager.parsePercent("50", 0.10), 0.0001);
        assertEquals(0.25, AfkFishingManager.parsePercent(0.25, 0.10), 0.0001);
        assertEquals(0.10, AfkFishingManager.parsePercent(null, 0.10), 0.0001);
        assertEquals(0.10, AfkFishingManager.parsePercent("invalid_value", 0.10), 0.0001);
    }
}
