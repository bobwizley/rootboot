package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MajorEventDiscoveryStateTest {

    @Test
    void anEventIsUndiscoveredUntilItIsRecorded() {
        MajorEventDiscoveryState state = MajorEventDiscoveryState.empty();
        UUID playerId = UUID.randomUUID();

        assertFalse(state.discovered(playerId, MajorEvent.END));
        state.discover(playerId, MajorEvent.END);
        assertTrue(state.discovered(playerId, MajorEvent.END));
    }

    @Test
    void theTwoDiscoveriesAreRecordedSeparately() {
        MajorEventDiscoveryState state = MajorEventDiscoveryState.empty();
        UUID playerId = UUID.randomUUID();

        state.discover(playerId, MajorEvent.END);

        assertFalse(state.discovered(playerId, MajorEvent.WITHER));
    }

    @Test
    void discoveriesAreIndependentBetweenPlayers() {
        MajorEventDiscoveryState state = MajorEventDiscoveryState.empty();
        UUID discoverer = UUID.randomUUID();
        UUID newcomer = UUID.randomUUID();

        state.discover(discoverer, MajorEvent.WITHER);

        assertFalse(state.discovered(newcomer, MajorEvent.WITHER));
    }

    @Test
    void discoveriesSurvivePersistenceAndReconnect() {
        MajorEventDiscoveryState original = MajorEventDiscoveryState.empty();
        UUID playerId = UUID.randomUUID();
        original.discover(playerId, MajorEvent.END);
        original.discover(playerId, MajorEvent.WITHER);

        JsonElement encoded = MajorEventDiscoveryState.TYPE.codec()
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        MajorEventDiscoveryState restored = MajorEventDiscoveryState.TYPE.codec()
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertTrue(restored.discovered(playerId, MajorEvent.END));
        assertTrue(restored.discovered(playerId, MajorEvent.WITHER));
    }
}
