package br.com.bobwizley.rootboot.feature.voidrescue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The rescues currently running, by player. The state is authoritative on the server and saved with
 * the world, so a restart resumes a rescue a totem has already paid for instead of dropping it and
 * letting the void charge for another one. A player who is offline is not ticked, which pauses their
 * deadline until they are back.
 */
public final class VoidRescueState extends SavedData {

    /**
     * @param remaining ticks left on the deadline
     * @param held which motion the rescue applied and therefore owns, empty while it owns neither
     */
    public record Rescue(int remaining, Optional<VoidRescueMotion> held) {

        public static final Codec<Rescue> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        Codec.INT.fieldOf("remaining").forGetter(Rescue::remaining),
                        VoidRescueMotion.CODEC.optionalFieldOf("held").forGetter(Rescue::held))
                .apply(instance, Rescue::new));
    }

    public static final SavedDataType<VoidRescueState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("rootboot", "void_rescue"),
            () -> new VoidRescueState(new HashMap<>()),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Rescue.CODEC)
                    .fieldOf("rescues")
                    .xmap(rescues -> new VoidRescueState(new HashMap<>(rescues)),
                            state -> state.rescues)
                    .codec(),
            DataFixTypes.LEVEL);

    private final Map<UUID, Rescue> rescues;

    private VoidRescueState(Map<UUID, Rescue> rescues) {
        this.rescues = rescues;
    }

    /** @return the rescue running for that player, or {@code null} when none is */
    public Rescue rescue(UUID playerId) {
        return rescues.get(playerId);
    }

    /** Records a rescue, or ends it when given {@code null}. */
    public void set(UUID playerId, Rescue rescue) {
        Rescue previous = rescue == null ? rescues.remove(playerId) : rescues.put(playerId, rescue);
        if (!Objects.equals(previous, rescue)) {
            setDirty();
        }
    }
}
