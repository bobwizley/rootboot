package br.com.bobwizley.rootboot.feature.dropladder;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class DropLadderGameTests {

    private static final BlockPos WALL = new BlockPos(3, 6, 4);
    private static final BlockPos TOP = new BlockPos(3, 6, 3);

    @GameTest
    public void aSneakPressHangsOneLadderUnderTheColumn(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));

        press(player);

        assertBlockIs(helper, TOP.below(), Blocks.LADDER, "A sneak press must hang one ladder");
        assertBlockIs(
                helper,
                TOP.below(2),
                Blocks.AIR,
                "A single sneak press must not hang more than one ladder");
        helper.assertTrue(
                player.getMainHandItem().getCount() == 3,
                "A hung ladder must cost exactly one ladder");
        finish(helper, player);
    }

    @GameTest
    public void aHeldSneakKeyNeverExtendsTheColumnAgain(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));

        player.setShiftKeyDown(true);
        for (int tick = 0; tick < 20; tick++) {
            DropLadder.tick(player);
        }

        assertBlockIs(helper, TOP.below(), Blocks.LADDER, "The press must hang one ladder");
        assertBlockIs(
                helper,
                TOP.below(2),
                Blocks.AIR,
                "Holding the sneak key must not extend the column every tick");

        press(player);

        assertBlockIs(
                helper, TOP.below(2), Blocks.LADDER, "A new press must hang one more ladder");
        finish(helper, player);
    }

    @GameTest
    public void anObstructedSpaceNeitherHangsNorCosts(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        helper.setBlock(TOP.below(), Blocks.STONE);

        press(player);

        assertBlockIs(
                helper, TOP.below(), Blocks.STONE, "An obstructed space must not be replaced");
        helper.assertTrue(
                player.getMainHandItem().getCount() == 4,
                "An obstructed space must not cost a ladder");
        finish(helper, player);
    }

    @GameTest
    public void theMainHandIsUsedBeforeTheOffHand(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LADDER, 4));

        press(player);

        assertBlockIs(helper, TOP.below(), Blocks.LADDER, "A hand holding a ladder must hang one");
        helper.assertTrue(
                player.getMainHandItem().getCount() == 3,
                "The main hand must pay for the ladder first");
        helper.assertTrue(
                player.getOffhandItem().getCount() == 4,
                "The off hand must not pay while the main hand can");
        finish(helper, player);
    }

    @GameTest
    public void theOffHandIsOnlyAFallback(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.STICK, 4));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.LADDER, 4));

        press(player);

        assertBlockIs(
                helper, TOP.below(), Blocks.LADDER, "The off hand must hang the ladder alone");
        helper.assertTrue(
                player.getOffhandItem().getCount() == 3,
                "The off hand must pay when the main hand holds no ladder");
        finish(helper, player);
    }

    @GameTest
    public void emptyHandsHangNothing(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, ItemStack.EMPTY);

        press(player);

        assertBlockIs(
                helper,
                TOP.below(),
                Blocks.AIR,
                "A player holding no ladder must not extend the column");
        finish(helper, player);
    }

    @GameTest
    public void aPlayerOffTheColumnHangsNothing(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        BlockPos aside = helper.absolutePos(TOP.east(2));
        player.setPosRaw(aside.getX() + 0.5, aside.getY(), aside.getZ() + 0.5);

        press(player);

        assertBlockIs(
                helper,
                TOP.below(),
                Blocks.AIR,
                "A player outside a ladder column must not extend it");
        finish(helper, player);
    }

    @GameTest
    public void creativeHangsWithoutCost(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        player.setGameMode(GameType.CREATIVE);

        press(player);

        assertBlockIs(helper, TOP.below(), Blocks.LADDER, "Creative must hang the ladder");
        helper.assertTrue(
                player.getMainHandItem().getCount() == 4, "Creative must not cost a ladder");
        finish(helper, player);
    }

    @GameTest
    public void aHangingLadderKeepsTheFacingOfTheColumn(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));

        press(player);

        BlockState hanging = helper.getBlockState(TOP.below());
        helper.assertTrue(
                hanging.getValue(LadderBlock.FACING) == Direction.NORTH,
                "A hung ladder must face the same way as the column it hangs from");
        finish(helper, player);
    }

    @GameTest
    public void aHangingLadderDependsOnTheChainAbove(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        press(player);
        press(player);
        assertBlockIs(helper, TOP.below(2), Blocks.LADDER, "Two presses must hang two ladders");

        helper.setBlock(TOP, Blocks.AIR);

        assertBlockIs(
                helper,
                TOP.below(),
                Blocks.AIR,
                "Losing the ladder above must drop the ladder hanging from it");
        assertBlockIs(
                helper, TOP.below(2), Blocks.AIR, "The whole hanging chain must drop together");
        finish(helper, player);
    }

    @GameTest
    public void aWallSupportedLadderIsLeftToVanilla(GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        helper.setBlock(WALL.below(), Blocks.STONE);
        helper.setBlock(TOP.below(), ladderState());

        helper.setBlock(TOP, Blocks.AIR);

        assertBlockIs(
                helper,
                TOP.below(),
                Blocks.LADDER,
                "A ladder with its own wall must not care about the ladder above it");
        finish(helper, player);
    }

    @GameTest
    public void theDisabledFeatureStopsNewPlacementsWithoutDroppingTheOldOnes(
            GameTestHelper helper) {
        ServerPlayer player = climbingPlayer(helper, new ItemStack(Items.LADDER, 4));
        press(player);

        DropLadder.disable();
        try {
            press(player);

            assertBlockIs(
                    helper,
                    TOP.below(2),
                    Blocks.AIR,
                    "A disabled feature must not hang new ladders");
            assertBlockIs(
                    helper,
                    TOP.below(),
                    Blocks.LADDER,
                    "A disabled feature must leave the hanging ladders a world already has");
            helper.assertTrue(
                    player.getMainHandItem().getCount() == 3,
                    "A disabled feature must not cost a ladder");
        } finally {
            DropLadder.enable();
        }
        finish(helper, player);
    }

    private static BlockState ladderState() {
        return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
    }

    /** A player standing in a single wall-supported ladder, holding the given stack. */
    private static ServerPlayer climbingPlayer(GameTestHelper helper, ItemStack mainHand) {
        DropLadder.enable();
        helper.setBlock(WALL, Blocks.STONE);
        helper.setBlock(TOP, ladderState());

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(TOP);
        player.setPosRaw(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        return player;
    }

    /** A fresh press of the sneak key, released first so the transition is always a new one. */
    private static void press(ServerPlayer player) {
        player.setShiftKeyDown(false);
        DropLadder.tick(player);
        player.setShiftKeyDown(true);
        DropLadder.tick(player);
    }

    private static void assertBlockIs(
            GameTestHelper helper, BlockPos pos, Block expected, String message) {
        helper.assertBlock(
                pos,
                block -> block == expected,
                found -> Component.literal(
                        message + " (expected " + expected + ", found " + found + ")"));
    }

    private static void finish(GameTestHelper helper, ServerPlayer player) {
        DropLadder.forget(player);
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeed();
    }
}
