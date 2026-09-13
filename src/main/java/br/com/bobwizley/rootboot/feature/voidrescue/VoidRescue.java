package br.com.bobwizley.rootboot.feature.voidrescue;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Void rescue: an {@code out_of_world} kill is handed to the vanilla totem flow, and the player it
 * brings back gets up to sixty seconds of controlled vertical movement to climb out.
 *
 * <p>Eligibility is the cause of the damage, never a dimension or a coordinate, so any dimension
 * that can drop a player out of the world — vanilla or modded — is covered by the same rule.
 */
public final class VoidRescue {

    /** The whole deadline a single activation grants, in ticks. */
    public static final int DEADLINE_TICKS = 20 * 60;

    /**
     * Levitation IV. The effect converges on {@code 0.05 * (amplifier + 1)} blocks per tick, so
     * this is four blocks per second — the pace the player walks with on the ground.
     */
    private static final int RISE_AMPLIFIER = 3;

    private static boolean enabled;

    private VoidRescue() {
    }

    static void enable() {
        enabled = true;
    }

    static void disable() {
        enabled = false;
    }

    /**
     * Whether an {@code out_of_world} kill is allowed to reach the totem this time. Vanilla stops
     * it because the damage type bypasses invulnerability; answering {@code true} here suspends
     * only that veto, leaving the hands it searches, the totem it consumes and every effect it
     * applies exactly as vanilla wrote them.
     */
    public static boolean reachesTotem(LivingEntity entity, DamageSource killingDamage) {
        return enabled && entity instanceof ServerPlayer && isVoid(killingDamage);
    }

    /** Starts a full deadline for a totem the void has just activated. */
    public static void begin(LivingEntity entity, DamageSource killingDamage) {
        if (reachesTotem(entity, killingDamage)) {
            ServerPlayer player = (ServerPlayer) entity;
            state(player).setRemaining(player.getUUID(), DEADLINE_TICKS);
        }
    }

    /**
     * The void keeps hurting every ten ticks for four points, and the totem hands the player back
     * with one point of health, so an unshielded rescue would be over in about a second: the
     * deadline only means something while the cause that opened it cannot reopen it. Nothing else
     * is shielded — the player stays as mortal as vanilla makes them for the whole minute.
     */
    public static boolean shieldsFromVoid(LivingEntity entity, DamageSource source) {
        return entity instanceof ServerPlayer player
                && isVoid(source)
                && state(player).remaining(player.getUUID()) > 0;
    }

    public static void tick(MinecraftServer server) {
        VoidRescueState state = state(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(state, player);
        }
    }

    public static void tick(ServerPlayer player) {
        tick(state(player), player);
    }

    private static void tick(VoidRescueState state, ServerPlayer player) {
        int remaining = state.remaining(player.getUUID());
        if (remaining <= 0) {
            return;
        }
        if (!player.isAlive() || player.onGround()) {
            end(state, player);
            return;
        }

        remaining--;
        if (remaining == 0) {
            end(state, player);
            return;
        }
        state.setRemaining(player.getUUID(), remaining);
        steer(player, remaining);
    }

    /**
     * Rising and sinking are expressed as the two vanilla effects that already mean them, so the
     * movement is predicted by the client that owns it instead of being corrected by the server,
     * and a client without RootBoot sees exactly what is happening. Both effects belong to the
     * rescue while it runs: the one that does not match the sneak key is cleared every tick, and
     * the deadline is what the applied one is given as its duration, so it runs out with the
     * rescue rather than outliving it.
     */
    private static void steer(ServerPlayer player, int remaining) {
        boolean sinking = player.isShiftKeyDown();
        Holder<MobEffect> wanted = sinking ? MobEffects.SLOW_FALLING : MobEffects.LEVITATION;
        player.removeEffect(sinking ? MobEffects.LEVITATION : MobEffects.SLOW_FALLING);
        if (!player.hasEffect(wanted)) {
            player.addEffect(new MobEffectInstance(
                    wanted, remaining, sinking ? 0 : RISE_AMPLIFIER, false, false, true));
        }
    }

    /** Ends only this activation: nothing here outlives it, and nothing blocks the next one. */
    private static void end(VoidRescueState state, ServerPlayer player) {
        state.setRemaining(player.getUUID(), 0);
        player.removeEffect(MobEffects.LEVITATION);
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    private static boolean isVoid(DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    private static VoidRescueState state(ServerPlayer player) {
        return state(player.level().getServer());
    }

    static VoidRescueState state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(VoidRescueState.TYPE);
    }
}
