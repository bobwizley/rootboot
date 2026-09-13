package br.com.bobwizley.rootboot.feature.voidrescue;

import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The game test server boots RootBoot with Void Rescue enabled, so the totem activations here run
 * through the registered hooks rather than through a copy of them. A test body runs inside a single
 * server tick, which is what lets the deadline be driven to an exact number of ticks: the handler
 * that advances it on every tick cannot interleave with the body.
 */
public final class VoidRescueGameTests {

    /** Far more than a player can take, so the hit is lethal whatever the totem left behind. */
    private static final float LETHAL = 1000.0F;

    /** Where End terrain is: the rescue has to reach at least this high to hand over a landing. */
    private static final double END_ISLAND_HEIGHT = 60.0;

    /** What has to be left when the player gets there, so the sneak back down still fits. */
    private static final int TIME_TO_COME_DOWN_TICKS = 10 * 20;

    /** What the server waits for a joining client before it stops holding the player invulnerable. */
    private static final int CLIENT_LOAD_TIMEOUT_TICKS = 60;

    /** A potion far longer than any rescue, so the two can never be confused for each other. */
    private static final int POTION_TICKS = 4 * VoidRescue.DEADLINE_TICKS;

    /** Vanilla's terminal fall speed, which is what a player reaching the void is already moving at. */
    private static final double TERMINAL_FALL_SPEED = -3.92;

    /** Five hits of four points, ten ticks apart, plus room for the hit that lands on tick zero. */
    private static final int TICKS_TO_REACH_THE_LETHAL_HIT = 60;

    @GameTest
    public void theVoidSpendsATotemHeldInTheMainHand(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, totem());

        fallOutOfTheWorld(helper, player);

        helper.assertTrue(player.isAlive(), "The void must not kill a player holding a totem");
        helper.assertTrue(
                player.getMainHandItem().isEmpty(), "The void must spend the totem it used");
        helper.assertTrue(
                player.hasEffect(MobEffects.REGENERATION),
                "The totem must apply the effects it always applies");
        helper.assertValueEqual(
                remaining(helper, player), VoidRescue.DEADLINE_TICKS, "granted deadline");
        finish(helper, player);
    }

    @GameTest
    public void theVoidSpendsATotemHeldInTheOffHand(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, totem());

        fallOutOfTheWorld(helper, player);

        helper.assertTrue(player.isAlive(), "Either hand must be able to hold the totem");
        helper.assertTrue(
                player.getOffhandItem().isEmpty(), "The void must spend the totem it used");
        helper.assertValueEqual(
                remaining(helper, player), VoidRescue.DEADLINE_TICKS, "granted deadline");
        finish(helper, player);
    }

    @GameTest
    public void anotherCauseThatBypassesInvulnerabilityStillKills(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, totem());

        kill(helper, player, helper.getLevel().damageSources().genericKill());

        helper.assertFalse(
                player.isAlive(),
                "Only the void may reach the totem; every other bypassing cause stays vanilla");
        helper.assertValueEqual(
                totemsUsed(player), 0, "totems a cause that cannot reach them spent");
        helper.assertValueEqual(remaining(helper, player), 0, "granted deadline");
        finish(helper, player);
    }

    @GameTest
    public void theVoidStillKillsAPlayerWithoutATotem(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);

        fallOutOfTheWorld(helper, player);

        helper.assertFalse(player.isAlive(), "There is nothing to save a player with no totem");
        helper.assertValueEqual(remaining(helper, player), 0, "granted deadline");
        finish(helper, player);
    }

    @GameTest
    public void theRescueRisesAndSinksOnlyWhileSneakIsHeld(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);

        VoidRescue.tick(player);

        helper.assertTrue(player.hasEffect(MobEffects.LEVITATION), "A rescued player must rise");
        helper.assertFalse(
                player.hasEffect(MobEffects.SLOW_FALLING), "A rising player must not be sinking");

        player.setShiftKeyDown(true);
        VoidRescue.tick(player);

        helper.assertTrue(
                player.hasEffect(MobEffects.SLOW_FALLING), "Holding sneak must sink the player");
        helper.assertFalse(
                player.hasEffect(MobEffects.LEVITATION), "A sinking player must not be rising");

        player.setShiftKeyDown(false);
        VoidRescue.tick(player);

        helper.assertTrue(
                player.hasEffect(MobEffects.LEVITATION), "Releasing sneak must rise again");
        finish(helper, player);
    }

    @GameTest
    public void theRescueEndsTheMomentThePlayerLands(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);
        VoidRescue.tick(player);

        player.setOnGround(true);
        VoidRescue.tick(player);

        helper.assertValueEqual(remaining(helper, player), 0, "deadline left after landing");
        helper.assertFalse(
                player.hasEffect(MobEffects.LEVITATION), "Landing must stop the movement it gave");
        finish(helper, player);
    }

    @GameTest
    public void theRescueLastsExactlyOneMinuteAndThenEnds(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);

        for (int tick = 0; tick < VoidRescue.DEADLINE_TICKS - 1; tick++) {
            VoidRescue.tick(player);
        }

        helper.assertTrue(
                remaining(helper, player) > 0, "The rescue must last the whole minute");
        VoidRescue.tick(player);

        helper.assertValueEqual(remaining(helper, player), 0, "deadline left after the minute");
        helper.assertFalse(
                player.hasEffect(MobEffects.LEVITATION),
                "An expired rescue must stop the movement it gave");
        finish(helper, player);
    }

    @GameTest
    public void aRunningRescueIsNotChargedAnotherTotem(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, totem());
        VoidRescue.tick(player);
        int granted = remaining(helper, player);

        fallOutOfTheWorld(helper, player);

        helper.assertTrue(player.isAlive(), "The void must not reach a player it is rescuing");
        helper.assertFalse(
                player.getOffhandItem().isEmpty(),
                "A running rescue must not be charged a second totem");
        helper.assertValueEqual(
                remaining(helper, player), granted, "deadline left after a shielded hit");
        finish(helper, player);
    }

    @GameTest
    public void aLaterVoidDeathSpendsAnotherTotemAndGrantsAFullDeadline(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, totem());
        VoidRescue.tick(player);
        player.setOnGround(true);
        VoidRescue.tick(player);
        helper.assertValueEqual(remaining(helper, player), 0, "deadline left after landing");

        player.setOnGround(false);
        fallOutOfTheWorld(helper, player);

        helper.assertTrue(player.isAlive(), "A later fall must be able to spend another totem");
        helper.assertTrue(
                player.getOffhandItem().isEmpty(), "The later fall must spend the second totem");
        helper.assertValueEqual(
                remaining(helper, player), VoidRescue.DEADLINE_TICKS, "granted deadline");
        finish(helper, player);
    }

    @GameTest
    public void theDisabledFeatureLeavesTheVoidToVanilla(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, totem());

        VoidRescue.disable();
        try {
            fallOutOfTheWorld(helper, player);

            helper.assertFalse(
                    player.isAlive(), "A disabled feature must leave the void to vanilla");
            helper.assertValueEqual(
                    totemsUsed(player), 0, "totems a disabled feature spent");
            helper.assertValueEqual(remaining(helper, player), 0, "granted deadline");
        } finally {
            VoidRescue.enable();
        }
        finish(helper, player);
    }

    @GameTest
    public void risingLeavesAPlayersOwnSlowFallingAlone(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, POTION_TICKS));

        VoidRescue.tick(player);

        helper.assertTrue(
                player.hasEffect(MobEffects.LEVITATION), "A rescued player must rise");
        helper.assertTrue(
                potionIsIntact(player),
                "Rising must not spend a slow falling potion; levitation already replaces gravity");
        finish(helper, player);
    }

    @GameTest
    public void endingLeavesAPlayersOwnSlowFallingAlone(GameTestHelper helper) {
        ServerPlayer player = rescuedPlayer(helper);
        player.setShiftKeyDown(true);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, POTION_TICKS));
        VoidRescue.tick(player);

        player.setOnGround(true);
        VoidRescue.tick(player);

        helper.assertValueEqual(remaining(helper, player), 0, "deadline left after landing");
        helper.assertTrue(
                potionIsIntact(player),
                "Ending must clear only what the rescue itself applied");
        finish(helper, player);
    }

    /**
     * The whole point of the feature, measured against vanilla movement instead of against the
     * constant that produces it. The fall is the ordinary one: a player at full health drops into
     * the End void, and the void takes five hits over forty ticks to reach the lethal one, so the
     * totem fires a long way under the height where the void first touched them.
     */
    @GameTest
    public void aFullHealthFallIntoTheEndVoidGetsBackToTheIslands(GameTestHelper helper) {
        ServerPlayer player = fallingIntoTheEndVoid(helper);

        for (int tick = 0; tick < TICKS_TO_REACH_THE_LETHAL_HIT
                && remaining(helper, player) == 0; tick++) {
            player.invulnerableTime = Math.max(0, player.invulnerableTime - 1);
            player.checkBelowWorld();
            player.travel(Vec3.ZERO);
        }
        helper.assertTrue(remaining(helper, player) > 0, "The void must have activated the totem");

        int leftOnArrival = 0;
        for (int tick = 0; tick < VoidRescue.DEADLINE_TICKS && leftOnArrival == 0; tick++) {
            VoidRescue.tick(player);
            player.travel(Vec3.ZERO);
            if (reachedTheIslands(player)) {
                leftOnArrival = remaining(helper, player);
            }
        }

        helper.assertTrue(
                leftOnArrival >= TIME_TO_COME_DOWN_TICKS,
                "A rescue must reach the End islands with time left to get onto them (reached "
                        + player.getY() + " with " + leftOnArrival + " ticks left)");
        finish(helper, player);
    }

    /**
     * Up at island height, or stopped by the underside of one: both mean the climb got the player
     * back to where the End has ground again. Which of the two happens depends on where the fall
     * started, and steering out from under an island is ordinary movement the feature never touches.
     */
    private static boolean reachedTheIslands(ServerPlayer player) {
        return player.getY() >= END_ISLAND_HEIGHT
                || (player.verticalCollision && player.getDeltaMovement().y >= 0.0);
    }

    private static ItemStack totem() {
        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    /** Far longer than the rescue, so anything the rescue leaves behind is unmistakably the potion. */
    private static boolean potionIsIntact(ServerPlayer player) {
        MobEffectInstance potion = player.getEffect(MobEffects.SLOW_FALLING);
        return potion != null && potion.getDuration() > VoidRescue.DEADLINE_TICKS;
    }

    /**
     * A player at full health at terminal velocity just under the height where the End void starts
     * hurting. Vanilla holds a player invulnerable while it considers them mid-teleport, so the
     * dimension change has to be closed out before the void can touch them.
     */
    private static ServerPlayer fallingIntoTheEndVoid(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, totem());

        ServerLevel end = server(helper).getLevel(Level.END);
        player.teleportTo(end, 0.0, 0.0, 0.0, Set.of(), 0.0F, 0.0F, false);
        player.hasChangedDimension();
        letTheClientFinishLoading(helper, player);

        player.setPosRaw(0.0, end.getMinY() - 65.0, 0.0);
        player.setDeltaMovement(0.0, TERMINAL_FALL_SPEED, 0.0);
        return player;
    }

    /** A survival player in the air, which is where every rescue starts and ends. */
    private static ServerPlayer floatingPlayer(GameTestHelper helper) {
        VoidRescue.enable();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(new Vec3(1.0, 4.0, 1.0));
        player.setPosRaw(position.x, position.y, position.z);
        player.setOnGround(false);
        letTheClientFinishLoading(helper, player);
        return player;
    }

    /**
     * A mock player's embedded client never reports itself as loaded, and the server holds a player
     * whose client has not loaded invulnerable to everything, so the timeout that grants it is run
     * down by hand. Without this no damage of any kind reaches the player.
     */
    private static void letTheClientFinishLoading(GameTestHelper helper, ServerPlayer player) {
        for (int tick = 0; tick < CLIENT_LOAD_TIMEOUT_TICKS
                && !player.connection.hasClientLoaded(); tick++) {
            player.connection.tickClientLoadTimeout();
        }
        helper.assertTrue(
                player.connection.hasClientLoaded(),
                "A player the server still considers unloaded cannot be hurt at all");
    }

    /** A player the void has just saved, with the rescue granted and nothing ticked yet. */
    private static ServerPlayer rescuedPlayer(GameTestHelper helper) {
        ServerPlayer player = floatingPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, totem());
        fallOutOfTheWorld(helper, player);
        helper.assertValueEqual(
                remaining(helper, player), VoidRescue.DEADLINE_TICKS, "granted deadline");
        return player;
    }

    private static void fallOutOfTheWorld(GameTestHelper helper, ServerPlayer player) {
        kill(helper, player, helper.getLevel().damageSources().fellOutOfWorld());
    }

    /**
     * A test body runs inside a single server tick, so nothing ticks the damage cooldown down
     * between two hits and vanilla would refuse the second one. Clearing it here is what a tick
     * would have done and is what lets a body hit the same player more than once.
     */
    private static void kill(GameTestHelper helper, ServerPlayer player, DamageSource source) {
        player.invulnerableTime = 0;
        player.hurtServer(helper.getLevel(), source, LETHAL);
    }

    /** The vanilla statistic the totem flow awards, which survives the inventory a death drops. */
    private static int totemsUsed(ServerPlayer player) {
        return player.getStats().getValue(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
    }

    private static int remaining(GameTestHelper helper, ServerPlayer player) {
        VoidRescueState.Rescue rescue = VoidRescue.state(server(helper)).rescue(player.getUUID());
        return rescue == null ? 0 : rescue.remaining();
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void finish(GameTestHelper helper, ServerPlayer player) {
        VoidRescue.state(server(helper)).set(player.getUUID(), null);
        server(helper).getPlayerList().remove(player);
        helper.succeed();
    }
}
