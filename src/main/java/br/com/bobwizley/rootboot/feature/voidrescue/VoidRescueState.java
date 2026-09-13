package br.com.bobwizley.rootboot.feature.voidrescue;

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
 * How much of its deadline each running rescue has left, in ticks. The deadline is authoritative
 * on the server and saved with the world so that a restart resumes the rescue a totem has already
 * paid for instead of dropping it and letting the void charge for another one. A player who is
 * offline simply is not ticked, which pauses the deadline until they are back.
 */
public final class VoidRescueState extends SavedData {

    public static final SavedDataType<VoidRescueState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("rootboot", "void_rescue"),
            () -> new VoidRescueState(new HashMap<>()),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
                    .fieldOf("rescues")
                    .xmap(rescues -> new VoidRescueState(new HashMap<>(rescues)),
                            state -> state.rescues)
                    .codec(),
            DataFixTypes.LEVEL);

    private final Map<UUID, Integer> rescues;

    private VoidRescueState(Map<UUID, Integer> rescues) {
        this.rescues = rescues;
    }

    public int remaining(UUID playerId) {
        return rescues.getOrDefault(playerId, 0);
    }

    public void setRemaining(UUID playerId, int ticks) {
        Integer previous = ticks > 0 ? rescues.put(playerId, ticks) : rescues.remove(playerId);
        if (previous == null ? ticks > 0 : previous != ticks) {
            setDirty();
        }
    }
}
