package br.com.bobwizley.rootboot.feature.deathstats;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-world persistence of how long each player has survived since their previous death. The
 * elapsed time is accumulated tick by tick rather than derived from a start timestamp, which is
 * what makes it pause while the player is offline or dead and survive a restart without ever
 * counting the time the server was down.
 */
public final class SurvivalClock extends SavedData {

    public static final SavedDataType<SurvivalClock> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("rootboot", "survival_clock"),
            SurvivalClock::empty,
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG)
                    .fieldOf("survived_ticks")
                    .xmap(SurvivalClock::new, clock -> clock.survivedTicks)
                    .codec(),
            DataFixTypes.LEVEL);

    private final Map<UUID, Long> survivedTicks;

    private SurvivalClock(Map<UUID, Long> survivedTicks) {
        this.survivedTicks = new HashMap<>(survivedTicks);
    }

    public static SurvivalClock empty() {
        return new SurvivalClock(Map.of());
    }

    public long survivedTicks(UUID playerId) {
        return survivedTicks.getOrDefault(playerId, 0L);
    }

    public void advance(UUID playerId) {
        survivedTicks.merge(playerId, 1L, Long::sum);
        setDirty();
    }

    public void reset(UUID playerId) {
        survivedTicks.put(playerId, 0L);
        setDirty();
    }
}
