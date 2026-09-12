package br.com.bobwizley.rootboot.feature.deathstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.bobwizley.rootboot.config.RootBootConfig;
import br.com.bobwizley.rootboot.feature.FeatureRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeathStatsFeatureTest {

    // Disabling the feature has to stop it at the registry, before any handler exists: nothing
    // announces a death and nothing writes the vanilla statistic. The game tests cover the enabled
    // side, since the game test server always boots with the toggle on.
    @Test
    void aDisabledToggleRegistersNoDeathHandlers() {
        FeatureRegistry registry = new FeatureRegistry(List.of(new DeathStatsFeature()));
        RootBootConfig config = new RootBootConfig();
        config.setEnabled(DeathStatsFeature.ID, false);

        assertTrue(registry.registerEnabled(config).isEmpty());
    }

    @Test
    void theFeatureIdIsStable() {
        assertEquals("death_stats", new DeathStatsFeature().id());
    }
}
