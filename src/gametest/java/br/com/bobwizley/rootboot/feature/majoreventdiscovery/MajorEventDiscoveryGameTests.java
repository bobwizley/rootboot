package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import java.util.Set;
import java.util.UUID;
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

    @GameTest
    public void aWitherAtSixtyFourBlocksIsDiscovered(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        WitherBoss wither = witherAbove(helper, player, MajorEventDiscoveryFeature.WITHER_RADIUS);

        MajorEventDiscoveryFeature.discover(server(helper), player);

        helper.assertTrue(
                discovered(helper, player, MajorEvent.WITHER),
                "A Wither exactly 64 blocks away must be discovered");
        cleanUp(helper, player, wither);
        helper.succeed();
    }

    @GameTest
    public void aWitherBeyondSixtyFourBlocksIsNotDiscovered(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        WitherBoss wither = witherAbove(helper, player, MajorEventDiscoveryFeature.WITHER_RADIUS + 1.0);

        MajorEventDiscoveryFeature.discover(server(helper), player);

        helper.assertFalse(
                discovered(helper, player, MajorEvent.WITHER),
                "A Wither past 64 blocks must leave the discovery unrecorded");
        cleanUp(helper, player, wither);
        helper.succeed();
    }

    @GameTest
    public void aPlayerWhoJoinsInsideTheRadiusDiscoversOnTheFirstCheck(GameTestHelper helper) {
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 2.0, 1.0));
        WitherBoss wither = witherAt(helper, position.add(0.0, 16.0, 0.0));
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));

        MajorEventDiscoveryFeature.discover(server(helper), player);

        helper.assertTrue(
                discovered(helper, player, MajorEvent.WITHER),
                "Joining already inside the radius must count as the first approach");
        cleanUp(helper, player, wither);
        helper.succeed();
    }

    @GameTest
    public void theEndIsDiscoveredOnTheFirstCheckInsideTheDimension(GameTestHelper helper) {
        ServerPlayer player = joinAt(helper, new Vec3(1.0, 2.0, 1.0));
        ServerLevel end = server(helper).getLevel(Level.END);
        helper.assertTrue(end != null, "The game test server must provide the End");
        player.teleportTo(end, 0.0, 96.0, 0.0, Set.of(), 0.0F, 0.0F, false);

        MajorEventDiscoveryFeature.discover(server(helper), player);

        helper.assertTrue(
                discovered(helper, player, MajorEvent.END),
                "Being in the End without an observable transition must count as the first entry");
        helper.assertFalse(
                discovered(helper, player, MajorEvent.WITHER),
                "The End discovery must not record the Wither discovery");
        cleanUp(helper, player, null);
        helper.succeed();
    }

    private static ServerPlayer joinAt(GameTestHelper helper, Vec3 relativePosition) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 position = helper.absoluteVec(relativePosition);
        player.snapTo(position);
        return player;
    }

    private static WitherBoss witherAbove(GameTestHelper helper, ServerPlayer player, double distance) {
        return witherAt(helper, player.position().add(0.0, distance, 0.0));
    }

    private static WitherBoss witherAt(GameTestHelper helper, Vec3 position) {
        WitherBoss wither = EntityTypes.WITHER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(wither != null, "The Wither must be creatable");
        wither.setPos(position);
        helper.getLevel().addFreshEntity(wither);
        return wither;
    }

    private static boolean discovered(GameTestHelper helper, ServerPlayer player, MajorEvent event) {
        UUID playerId = player.getUUID();
        return MajorEventDiscoveryFeature.state(server(helper)).discovered(playerId, event);
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
