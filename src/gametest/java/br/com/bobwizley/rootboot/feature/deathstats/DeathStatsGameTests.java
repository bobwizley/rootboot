package br.com.bobwizley.rootboot.feature.deathstats;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;

/**
 * The game test server boots RootBoot with Death Stats enabled, so every death here runs through
 * the registered handler rather than through a copy of it. A test body runs inside a single server
 * tick, which is what lets the clock be driven to an exact number of ticks: the handler that
 * advances it on every tick cannot interleave with the body.
 */
public final class DeathStatsGameTests {

    private static final int ONE_MINUTE_FIVE_SECONDS_IN_TICKS = 65 * 20;

    @GameTest
    public void theAnnouncementCarriesTheNameTheSurvivedTimeAndTheTotalCount(
            GameTestHelper helper) {
        ServerPlayer player = join(helper);

        Component message =
                DeathStatsMessage.of(player.getDisplayName(), ONE_MINUTE_FIVE_SECONDS_IN_TICKS, 4);

        TranslatableContents contents = (TranslatableContents) message.getContents();
        helper.assertValueEqual(contents.getKey(), DeathStatsMessage.KEY, "message key");
        helper.assertValueEqual(contents.getArgs().length, 3, "announced values");
        helper.assertValueEqual(
                contents.getArgs()[0], player.getDisplayName(), "announced player name");
        helper.assertValueEqual(
                ((Component) contents.getArgs()[1]).getString(), "0:01:05", "announced time");
        helper.assertValueEqual(
                ((Component) contents.getArgs()[2]).getString(), "4", "announced death count");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void aDeathAnnouncesTheVanillaTotalIncludingDeathsFromBeforeRootBoot(
            GameTestHelper helper) {
        ServerPlayer player = join(helper);
        setDeaths(player, 7);
        survive(helper, player, 100);

        die(helper, player);

        helper.assertValueEqual(
                DeathStatsFeature.deaths(player), 8, "total including pre-install deaths");
        helper.assertValueEqual(
                clock(helper).survivedTicks(player.getUUID()),
                0L,
                "ticks left on the clock by the announcement");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theClockPausesWhileThePlayerIsOffline(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        survive(helper, player, 40);

        leave(helper, player);
        DeathStatsFeature.advanceClocks(server(helper));
        DeathStatsFeature.advanceClocks(server(helper));

        helper.assertValueEqual(
                clock(helper).survivedTicks(player.getUUID()), 40L, "ticks survived while offline");
        helper.succeed();
    }

    @GameTest
    public void theClockPausesWhileThePlayerIsDead(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        survive(helper, player, 40);

        die(helper, player);
        DeathStatsFeature.advanceClocks(server(helper));

        helper.assertValueEqual(
                clock(helper).survivedTicks(player.getUUID()),
                0L,
                "ticks survived between the death and the respawn");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void aSurvivedTimeSurvivesARestart(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        survive(helper, player, 100);

        SurvivalClock restarted = afterARestart(clock(helper));

        helper.assertValueEqual(
                restarted.survivedTicks(player.getUUID()), 100L, "ticks survived after a restart");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void aRestartAfterADeathKeepsTheClockAtZero(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        survive(helper, player, 100);

        die(helper, player);
        SurvivalClock restarted = afterARestart(clock(helper));

        helper.assertValueEqual(
                restarted.survivedTicks(player.getUUID()), 0L, "ticks restored after a restart");
        leave(helper, player);
        helper.succeed();
    }

    // A real death empties the health bar before ServerPlayer#die runs, and that is what makes the
    // player stop counting as alive until they respawn.
    private static void die(GameTestHelper helper, ServerPlayer player) {
        player.setHealth(0.0F);
        player.die(helper.getLevel().damageSources().genericKill());
    }

    private static ServerPlayer join(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 4.0, 1.0));
        player.setPosRaw(position.x, position.y, position.z);
        return player;
    }

    private static void survive(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            DeathStatsFeature.advanceClocks(server(helper));
        }
        helper.assertValueEqual(
                clock(helper).survivedTicks(player.getUUID()), (long) ticks, "ticks survived");
    }

    private static SurvivalClock afterARestart(SurvivalClock clock) {
        Tag saved = SurvivalClock.TYPE.codec().encodeStart(NbtOps.INSTANCE, clock).getOrThrow();
        return SurvivalClock.TYPE.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
    }

    private static void setDeaths(ServerPlayer player, int deaths) {
        Stat<?> stat = Stats.CUSTOM.get(Stats.DEATHS);
        player.getStats().setValue(player, stat, deaths);
    }

    private static SurvivalClock clock(GameTestHelper helper) {
        return DeathStatsFeature.clock(server(helper));
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        server(helper).getPlayerList().remove(player);
    }
}
