package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Every case drives {@link MajorEventDiscoveryFeature#discover} directly instead of waiting for the
 * server tick, so the radius boundary and the "first check wins" rule are asserted deterministically.
 * The Wither is always spawned by the test and never by the player, which is what makes the
 * discovery independent of who summoned it.
 */
public final class MajorEventDiscoveryGameTests {

    // Joining discovers the current biome, and that title holds the player's single title slot. A
    // major event cannot be announced — and so is not recorded — until it has faded out.
    private static final int BIOME_TITLE_TICKS = 135;

    private static final int MAX_TICKS = 200;

    @GameTest(maxTicks = MAX_TICKS)
    public void aWitherAtSixtyFourBlocksIsDiscovered(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        withAFreeTitleSlot(helper, () -> {
            WitherBoss wither = witherAbove(helper, player, MajorEventDiscoveryFeature.WITHER_RADIUS);

            MajorEventDiscoveryFeature.discover(server(helper), player);

            helper.assertTrue(
                    discovered(helper, player, MajorEvent.WITHER),
                    "A Wither exactly 64 blocks away must be discovered");
            cleanUp(helper, player, wither);
        });
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void aWitherBeyondSixtyFourBlocksIsNotDiscovered(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        withAFreeTitleSlot(helper, () -> {
            WitherBoss wither =
                    witherAbove(helper, player, MajorEventDiscoveryFeature.WITHER_RADIUS + 1.0);

            MajorEventDiscoveryFeature.discover(server(helper), player);

            helper.assertFalse(
                    discovered(helper, player, MajorEvent.WITHER),
                    "A Wither past 64 blocks must leave the discovery unrecorded");
            cleanUp(helper, player, wither);
        });
    }

    // A Wither that outlives the test body would be picked up by the players of the tests running
    // beside this one, so this case checks the detection instead of the record: the slot the join
    // takes is beside the point, and the eligibility is what "already inside the radius" means.
    @GameTest
    public void aPlayerWhoJoinsInsideTheRadiusIsDetectedWithoutMoving(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 2.0, 1.0));
        WitherBoss wither = witherAt(helper, helper.getLevel(), position.add(0.0, 16.0, 0.0));
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));

        helper.assertTrue(
                MajorEventDiscoveryFeature.happening(MajorEvent.WITHER, player),
                "Joining already inside the radius must count as the first approach");
        cleanUp(helper, player, wither);
        helper.succeed();
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void theEndIsDiscoveredOnTheFirstCheckInsideTheDimension(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        withAFreeTitleSlot(helper, () -> {
            player.teleportTo(endOf(helper), 0.0, 96.0, 0.0, Set.of(), 0.0F, 0.0F, false);

            MajorEventDiscoveryFeature.discover(server(helper), player);

            helper.assertTrue(
                    discovered(helper, player, MajorEvent.END),
                    "Being in the End without an observable transition must count as the first entry");
            helper.assertFalse(
                    discovered(helper, player, MajorEvent.WITHER),
                    "The End discovery must not record the Wither discovery");
            cleanUp(helper, player, null);
        });
    }

    @GameTest(maxTicks = MAX_TICKS)
    public void twoEligibleEventsInTheSameTickRecordOnlyTheAnnouncedOne(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        withAFreeTitleSlot(helper, () -> {
            ServerLevel end = endOf(helper);
            player.teleportTo(end, 0.0, 96.0, 0.0, Set.of(), 0.0F, 0.0F, false);
            WitherBoss wither = witherAt(helper, end, player.position().add(0.0, 16.0, 0.0));

            MajorEventDiscoveryFeature.discover(server(helper), player);

            helper.assertValueEqual(recorded(helper, player), 1, "discoveries recorded in one tick");

            MajorEventDiscoveryFeature.discover(server(helper), player);

            helper.assertValueEqual(
                    recorded(helper, player), 1, "discoveries recorded while the title is on screen");
            cleanUp(helper, player, wither);
        });
    }

    private static void withAFreeTitleSlot(GameTestHelper helper, Runnable body) {
        helper.startSequence().thenIdle(BIOME_TITLE_TICKS).thenExecute(body).thenSucceed();
    }

    private static ServerLevel endOf(GameTestHelper helper) {
        ServerLevel end = server(helper).getLevel(Level.END);
        helper.assertTrue(end != null, "The game test server must provide the End");
        return end;
    }

    private static ServerPlayer joinAt(GameTestHelper helper, Vec3 relativePosition) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(relativePosition));
        return player;
    }

    private static WitherBoss witherAbove(
            GameTestHelper helper, ServerPlayer player, double distance) {
        return witherAt(helper, player.level(), player.position().add(0.0, distance, 0.0));
    }

    private static WitherBoss witherAt(GameTestHelper helper, ServerLevel level, Vec3 position) {
        WitherBoss wither = EntityTypes.WITHER.create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(wither != null, "The Wither must be creatable");
        wither.setPos(position);
        level.addFreshEntity(wither);
        return wither;
    }

    private static int recorded(GameTestHelper helper, ServerPlayer player) {
        int recorded = 0;
        for (MajorEvent event : MajorEvent.values()) {
            if (discovered(helper, player, event)) {
                recorded++;
            }
        }
        return recorded;
    }

    private static boolean discovered(GameTestHelper helper, ServerPlayer player, MajorEvent event) {
        return MajorEventDiscoveryFeature.state(server(helper)).discovered(player.getUUID(), event);
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void cleanUp(GameTestHelper helper, ServerPlayer player, WitherBoss wither) {
        if (wither != null) {
            wither.discard();
        }
        server(helper).getPlayerList().remove(player);
    }
}
