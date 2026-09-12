package br.com.bobwizley.rootboot.feature.deathstats;

import br.com.bobwizley.rootboot.feature.Feature;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * Announces every player death to everyone online with the time survived since the player's
 * previous death and their total death count.
 */
public final class DeathStatsFeature implements Feature {

    public static final String ID = "death_stats";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(DeathStatsFeature::advanceClocks);

        // AFTER_DEATH runs at the tail of ServerPlayer#die, past the point where vanilla awards
        // the deaths stat, so the count read here already includes the death being announced.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                announce(player);
            }
        });
    }

    // Accumulating a tick at a time, instead of deriving the elapsed time from a start timestamp,
    // is what pauses the clock while a player is offline or dead: a player the loop never sees
    // simply keeps the time they had.
    static void advanceClocks(MinecraftServer server) {
        SurvivalClock clock = clock(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isAlive()) {
                clock.advance(player.getUUID());
            }
        }
    }

    static void announce(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        SurvivalClock clock = clock(server);

        server.getPlayerList().broadcastSystemMessage(
                DeathStatsMessage.of(player, clock.survivedTicks(player.getUUID()), deaths(player)),
                false);
        clock.reset(player.getUUID());
    }

    // The vanilla statistic is the only count read, so deaths from before RootBoot was installed
    // are part of the total.
    static int deaths(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
    }

    public static SurvivalClock clock(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SurvivalClock.TYPE);
    }
}
