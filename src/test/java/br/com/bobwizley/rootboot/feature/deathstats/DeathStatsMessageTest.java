package br.com.bobwizley.rootboot.feature.deathstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class DeathStatsMessageTest {

    @Test
    void anUnstartedClockReadsAsZero() {
        assertEquals("0:00:00", DeathStatsMessage.duration(0L));
    }

    @Test
    void ticksBelowASecondAreNotCounted() {
        assertEquals("0:00:00", DeathStatsMessage.duration(19L));
    }

    @Test
    void minutesAndSecondsArePaddedToTwoDigits() {
        assertEquals("0:01:05", DeathStatsMessage.duration(65L * 20L));
    }

    @Test
    void hoursAreNotWrappedIntoDays() {
        assertEquals("30:00:00", DeathStatsMessage.duration(30L * 3_600L * 20L));
    }

    // This is a server-side feature, so the announcement has to arrive complete at a client that
    // has no RootBoot catalogue. This JVM holds only the vanilla one, which is exactly that client.
    @Test
    void aClientWithoutTheRootBootCatalogueStillReadsTheThreeStats() {
        assertFalse(Language.getInstance().has(DeathStatsMessage.KEY));
        assertFalse(Language.getInstance().has(DeathStatsMessage.FIRST_KEY));

        String announcement =
                DeathStatsMessage.of(Component.literal("Alex"), 65L * 20L, 4).getString();

        assertFalse(announcement.contains(DeathStatsMessage.KEY));
        assertTrue(announcement.contains("Alex"), announcement);
        assertTrue(announcement.contains("0:01:05"), announcement);
        assertTrue(announcement.contains("4"), announcement);
    }

    @Test
    void theFirstDeathReadsInTheSingular() {
        String announcement =
                DeathStatsMessage.of(Component.literal("Alex"), 0L, 1).getString();

        assertTrue(announcement.endsWith("1 death"), announcement);
    }

    // The fallbacks only help while they say what the catalogue says; nothing else keeps the two
    // copies of these sentences equal.
    @Test
    void theFallbacksMatchTheModCatalogue() throws IOException {
        JsonObject catalogue = englishCatalogue();

        assertEquals(
                DeathStatsMessage.FALLBACK, catalogue.get(DeathStatsMessage.KEY).getAsString());
        assertEquals(
                DeathStatsMessage.FIRST_FALLBACK,
                catalogue.get(DeathStatsMessage.FIRST_KEY).getAsString());
    }

    private static JsonObject englishCatalogue() throws IOException {
        try (InputStream lang = DeathStatsMessageTest.class.getResourceAsStream(
                "/assets/rootboot/lang/en_us.json")) {
            assertTrue(lang != null, "The mod must package an en_us catalogue");
            return new Gson().fromJson(
                    new InputStreamReader(lang, StandardCharsets.UTF_8), JsonObject.class);
        }
    }
}
