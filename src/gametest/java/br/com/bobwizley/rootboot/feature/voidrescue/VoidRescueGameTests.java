package br.com.bobwizley.rootboot.feature.voidrescue;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
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

    /** Levitation IV, which converges on the four blocks per second a player walks with. */
    private static final int EXPECTED_RISE_AMPLIFIER = 3;

    /** What the server waits for a joining client before it stops holding the player invulnerable. */
    private static final int CLIENT_LOAD_TIMEOUT_TICKS = 60;

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

        MobEffectInstance rise = player.getEffect(MobEffects.LEVITATION);
        helper.assertTrue(rise != null, "A rescued player must rise");
        helper.assertValueEqual(
                rise.getAmplifier(), EXPECTED_RISE_AMPLIFIER, "rise in blocks per second");
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

    private static ItemStack totem() {
        return new ItemStack(Items.TOTEM_OF_UNDYING);
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
        return VoidRescue.state(server(helper)).remaining(player.getUUID());
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static void finish(GameTestHelper helper, ServerPlayer player) {
        VoidRescue.state(server(helper)).setRemaining(player.getUUID(), 0);
        server(helper).getPlayerList().remove(player);
        helper.succeed();
    }
}
