package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bobwizley.rootboot.config.RootBootConfig;
import br.com.bobwizley.rootboot.feature.FeatureRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MajorEventDiscoveryFeatureTest {

    @Test
    void anEventLivedWhileDisabledRemainsEligibleAfterReEnable() {
        RootBootConfig config = new RootBootConfig();
        config.setEnabled(MajorEventDiscoveryFeature.ID, false);
        FeatureRegistry registry = new FeatureRegistry(List.of(new MajorEventDiscoveryFeature()));
        MajorEventDiscoveryState state = MajorEventDiscoveryState.empty();
        UUID playerId = UUID.randomUUID();

        assertTrue(registry.registerEnabled(config).isEmpty());
        assertFalse(state.discovered(playerId, MajorEvent.END));
    }
}
