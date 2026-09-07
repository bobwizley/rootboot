package br.com.bobwizley.rootboot.feature.biomediscovery;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-world persistence for each player's discovered biome ids. Entries survive reconnects and
 * feature disable/re-enable cycles; disabling the feature merely prevents new entries because no
 * discovery handlers are registered.
 */
public final class BiomeDiscoveryState extends SavedData {

    public static final SavedDataType<BiomeDiscoveryState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("rootboot", "biome_discovery"),
            BiomeDiscoveryState::empty,
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Identifier.CODEC.listOf())
                    .fieldOf("discoveries")
                    .xmap(BiomeDiscoveryState::fromLists, BiomeDiscoveryState::toLists)
                    .codec(),
            DataFixTypes.LEVEL);

    private final Map<UUID, Set<Identifier>> discoveries;

    private BiomeDiscoveryState(Map<UUID, Set<Identifier>> discoveries) {
        this.discoveries = discoveries;
    }

    public static BiomeDiscoveryState empty() {
        return new BiomeDiscoveryState(new HashMap<>());
    }

    public boolean discovered(UUID playerId, Identifier biomeId) {
        return discoveries.getOrDefault(playerId, Set.of()).contains(biomeId);
    }

    public boolean discover(UUID playerId, Identifier biomeId) {
        boolean discovered = discoveries.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(biomeId);
        if (discovered) {
            setDirty();
        }
        return discovered;
    }

    private static BiomeDiscoveryState fromLists(Map<UUID, List<Identifier>> discoveries) {
        Map<UUID, Set<Identifier>> sets = new HashMap<>();
        discoveries.forEach((playerId, biomeIds) -> sets.put(playerId, new HashSet<>(biomeIds)));
        return new BiomeDiscoveryState(sets);
    }

    private Map<UUID, List<Identifier>> toLists() {
        Map<UUID, List<Identifier>> lists = new HashMap<>();
        discoveries.forEach((playerId, biomeIds) -> lists.put(playerId, List.copyOf(biomeIds)));
        return lists;
    }
}
