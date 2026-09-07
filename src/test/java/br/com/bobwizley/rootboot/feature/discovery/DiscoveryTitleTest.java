package br.com.bobwizley.rootboot.feature.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiscoveryTitleTest {

    private static final int DURATION = 160;

    @Test
    void asecondTitleIsRefusedWhileTheFirstIsOnScreen() {
        UUID playerId = UUID.randomUUID();

        assertTrue(DiscoveryTitle.claim(playerId, 100L, DURATION));
        assertFalse(DiscoveryTitle.claim(playerId, 100L, DURATION));
        assertFalse(DiscoveryTitle.claim(playerId, 259L, DURATION));
    }

    @Test
    void theSlotIsFreeAgainWhenTheTitleHasFadedOut() {
        UUID playerId = UUID.randomUUID();
        DiscoveryTitle.claim(playerId, 100L, DURATION);

        assertTrue(DiscoveryTitle.claim(playerId, 260L, DURATION));
    }

    @Test
    void oneBusyPlayerDoesNotBlockAnother() {
        UUID discoverer = UUID.randomUUID();
        DiscoveryTitle.claim(discoverer, 100L, DURATION);

        assertTrue(DiscoveryTitle.claim(UUID.randomUUID(), 100L, DURATION));
    }
}
