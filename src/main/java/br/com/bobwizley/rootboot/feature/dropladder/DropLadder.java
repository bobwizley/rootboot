package br.com.bobwizley.rootboot.feature.dropladder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

public final class DropLadder {

    private static final Set<UUID> sneaking = new HashSet<>();

    private static boolean enabled;

    private DropLadder() {
    }

    static void enable() {
        enabled = true;
    }

    static void disable() {
        enabled = false;
    }

    /**
     * Turns the sneak key into the gesture the feature reacts to: only the transition into a press
     * extends the column, so holding the key down leaves the column where the press left it.
     *
     * <p>Called once per input the client reports rather than once per tick, because the client
     * reports every change it makes and a lagging one reports several of them at once. Sampling the
     * key per tick instead would read a release and the press that followed it as no change at all.
     */
    public static void readSneakInput(ServerPlayer player) {
        if (!enabled || !player.isShiftKeyDown()) {
            sneaking.remove(player.getUUID());
            return;
        }
        if (sneaking.add(player.getUUID())) {
            extendColumn(player);
        }
    }

    public static void forget(ServerPlayer player) {
        sneaking.remove(player.getUUID());
    }

    /**
     * What holds a hanging ladder up. The rule is installed whatever the toggle says, because
     * disabling the feature must stop new placements without dropping the hanging ladders a world
     * already has.
     */
    public static boolean hangsFromLadderAbove(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(Blocks.LADDER)
                && above.getValue(LadderBlock.FACING) == state.getValue(LadderBlock.FACING);
    }

    private static void extendColumn(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos climbed = climbedLadder(level, player);
        if (climbed == null) {
            return;
        }
        InteractionHand hand = handHoldingLadder(player);
        if (hand == null) {
            return;
        }

        BlockPos bottom = bottomOfColumn(level, climbed);
        BlockPos target = bottom.below();
        if (!mayPlaceAt(level, player, target, player.getItemInHand(hand))) {
            return;
        }

        BlockState hanging = level.getBlockState(bottom)
                .setValue(LadderBlock.WATERLOGGED, level.getFluidState(target).is(Fluids.WATER));
        level.setBlockAndUpdate(target, hanging);
        announcePlacement(level, player, target, hanging);

        if (!player.hasInfiniteMaterials()) {
            player.getItemInHand(hand).shrink(1);
        }
    }

    /**
     * The block below the feet is read as a fallback because a player who climbed to the top of a
     * column stands on its last rung rather than inside it.
     *
     * @return the ladder the player is on, or {@code null} when there is none
     */
    private static BlockPos climbedLadder(ServerLevel level, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        if (level.getBlockState(feet).is(Blocks.LADDER)) {
            return feet;
        }
        return level.getBlockState(feet.below()).is(Blocks.LADDER) ? feet.below() : null;
    }

    private static BlockPos bottomOfColumn(ServerLevel level, BlockPos climbed) {
        BlockPos bottom = climbed;
        while (level.getBlockState(bottom.below()).is(Blocks.LADDER)) {
            bottom = bottom.below();
        }
        return bottom;
    }

    private static InteractionHand handHoldingLadder(ServerPlayer player) {
        if (player.getMainHandItem().is(Items.LADDER)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(Items.LADDER)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    /**
     * The ladder is placed directly instead of going through the player's use flow, so the rules
     * that flow would apply — spawn protection, the world border, spectator and adventure mode —
     * have to be checked here. Adventure mode asks about the ladder actually being spent and about
     * the ladder above as the surface it is put against, which is the question the use flow asks;
     * the permission to break the obstruction in the way is a different one and grants nothing here.
     */
    private static boolean mayPlaceAt(
            ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack ladder) {
        return level.isInWorldBounds(pos)
                && level.getBlockState(pos).canBeReplaced()
                && !player.isSpectator()
                && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.DOWN, ladder);
    }

    private static void announcePlacement(
            ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        SoundType sound = state.getSoundType();
        level.playSound(
                null,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
    }
}
