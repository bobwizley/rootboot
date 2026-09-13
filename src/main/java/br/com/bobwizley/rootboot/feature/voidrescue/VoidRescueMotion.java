package br.com.bobwizley.rootboot.feature.voidrescue;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The two ways a rescue moves a player, each expressed as the vanilla effect that already means it.
 * The serialized name is the persisted identity of the effect a running rescue owns, so renaming one
 * orphans the effect a rescue saved before the rename.
 */
public enum VoidRescueMotion implements StringRepresentable {

    /**
     * Levitation VIII. The effect converges on {@code 0.05 * (amplifier + 1)} blocks per tick, which
     * drag settles at about 7.3 blocks per second.
     *
     * <p>The pace is what the geometry demands, not a taste: the totem only fires on the lethal hit,
     * and the void takes five hits of four points spread over forty ticks to get there, so the
     * player falls roughly 157 blocks at terminal velocity before the rescue even starts — about 220
     * blocks under the height where the void first touched them. Climbing back to an End island
     * inside the minute, with time left to sneak down onto it, is what sets this number.
     */
    RISING("rising", MobEffects.LEVITATION, 7),

    /** Slow Falling, whose whole effect is the slow descent the sneak key asks for. */
    SINKING("sinking", MobEffects.SLOW_FALLING, 0);

    public static final Codec<VoidRescueMotion> CODEC =
            StringRepresentable.fromEnum(VoidRescueMotion::values);

    private final String serializedName;
    private final Holder<MobEffect> effect;
    private final int amplifier;

    VoidRescueMotion(String serializedName, Holder<MobEffect> effect, int amplifier) {
        this.serializedName = serializedName;
        this.effect = effect;
        this.amplifier = amplifier;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public Holder<MobEffect> effect() {
        return effect;
    }

    /**
     * The remaining deadline is handed over as the duration, so the effect runs out with the rescue
     * rather than outliving it. It is applied without particles because the rescue is not a potion,
     * but keeps its icon: that is what reports the time left to a client with no RootBoot on it.
     */
    public MobEffectInstance instance(int remainingTicks) {
        return new MobEffectInstance(effect, remainingTicks, amplifier, false, false, true);
    }
}
