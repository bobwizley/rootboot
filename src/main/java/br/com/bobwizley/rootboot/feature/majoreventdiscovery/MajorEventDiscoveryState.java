package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.HashMap;
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
 * Per-world persistence of the major events each player has already discovered. Entries survive
 * reconnects and feature disable/re-enable cycles; disabling the feature merely prevents new
 * entries because no discovery handlers are registered.
 */
public final class MajorEventDiscoveryState extends SavedData {

    public static final SavedDataType<MajorEventDiscoveryState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("rootboot", "major_event_discovery"),
            MajorEventDiscoveryState::empty,
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, MajorEvent.CODEC.listOf())
                    .fieldOf("discoveries")
                    .xmap(MajorEventDiscoveryState::fromLists, MajorEventDiscoveryState::toLists)
                    .codec(),
            DataFixTypes.LEVEL);

    private final Map<UUID, Set<MajorEvent>> discoveries;

    private MajorEventDiscoveryState(Map<UUID, Set<MajorEvent>> discoveries) {
        this.discoveries = discoveries;
    }

    public static MajorEventDiscoveryState empty() {
        return new MajorEventDiscoveryState(new HashMap<>());
    }

    public boolean discovered(UUID playerId, MajorEvent event) {
        return discoveries.getOrDefault(playerId, Set.of()).contains(event);
    }

    public void discover(UUID playerId, MajorEvent event) {
        if (discoveries.computeIfAbsent(playerId, ignored -> EnumSet.noneOf(MajorEvent.class)).add(event)) {
            setDirty();
        }
    }

    private static MajorEventDiscoveryState fromLists(Map<UUID, List<MajorEvent>> discoveries) {
        Map<UUID, Set<MajorEvent>> sets = new HashMap<>();
        discoveries.forEach((playerId, events) -> {
            Set<MajorEvent> set = EnumSet.noneOf(MajorEvent.class);
            set.addAll(events);
            sets.put(playerId, set);
        });
        return new MajorEventDiscoveryState(sets);
    }

    private Map<UUID, List<MajorEvent>> toLists() {
        Map<UUID, List<MajorEvent>> lists = new HashMap<>();
        discoveries.forEach((playerId, events) -> lists.put(playerId, List.copyOf(events)));
        return lists;
    }
}
