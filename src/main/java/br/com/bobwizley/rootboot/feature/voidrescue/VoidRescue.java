package br.com.bobwizley.rootboot.feature.voidrescue;

import br.com.bobwizley.rootboot.feature.voidrescue.VoidRescueState.Rescue;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
            state(player).set(player.getUUID(), new Rescue(DEADLINE_TICKS, Optional.empty()));
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
                && state(player).rescue(player.getUUID()) != null;
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
        Rescue rescue = state.rescue(player.getUUID());
        if (rescue == null) {
            return;
        }
        if (!player.isAlive() || player.onGround()) {
            end(state, player, rescue);
            return;
        }

        int remaining = rescue.remaining() - 1;
        if (remaining <= 0) {
            end(state, player, rescue);
            return;
        }
        state.set(player.getUUID(), steer(player, rescue, remaining));
    }

    /**
     * Moves the player with the vanilla effect that matches the sneak key, so the movement is
     * predicted by the client that owns it instead of being corrected by the server, and a client
     * without RootBoot sees exactly what is happening.
     *
     * <p>The rescue only ever touches what it applied itself. Rising does not clear Slow Falling,
     * because Levitation already replaces gravity outright and clearing it would only cost the
     * player a potion; sinking clears Levitation only when the rescue is the one holding it, which
     * leaves a shulker's levitation to the shulker. An effect the player already has from somewhere
     * else is left untouched rather than applied over: it moves them the way this tick wants
     * anyway, and {@code addEffect} would rewrite its flags and then report a success the rescue
     * would later honour by taking the player's own effect away.
     */
    private static Rescue steer(ServerPlayer player, Rescue rescue, int remaining) {
        VoidRescueMotion wanted =
                player.isShiftKeyDown() ? VoidRescueMotion.SINKING : VoidRescueMotion.RISING;
        Optional<VoidRescueMotion> held = rescue.held();
        if (held.filter(motion -> motion == wanted).isPresent()
                && player.hasEffect(wanted.effect())) {
            return new Rescue(remaining, held);
        }

        held.filter(motion -> motion != wanted)
                .ifPresent(motion -> player.removeEffect(motion.effect()));
        if (player.hasEffect(wanted.effect())) {
            return new Rescue(remaining, Optional.empty());
        }
        player.addEffect(wanted.instance(remaining));
        return new Rescue(remaining, Optional.of(wanted));
    }

    /** Ends only this activation: nothing here outlives it, and nothing blocks the next one. */
    private static void end(VoidRescueState state, ServerPlayer player, Rescue rescue) {
        rescue.held().ifPresent(motion -> player.removeEffect(motion.effect()));
        state.set(player.getUUID(), null);
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
