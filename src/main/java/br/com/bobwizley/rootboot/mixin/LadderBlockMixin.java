package br.com.bobwizley.rootboot.mixin;

import br.com.bobwizley.rootboot.feature.dropladder.DropLadder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a ladder hang from the one above it instead of from a wall, which is what a dropped ladder
 * stands on.
 *
 * <p>Vanilla only re-reads {@code canSurvive} when the update comes from the side the ladder is
 * attached to, so the second injection is what makes the chain a chain: losing the ladder above
 * now drops everything hanging under it, the same way losing the wall does.
 */
@Mixin(LadderBlock.class)
abstract class LadderBlockMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void rootboot$hangFromTheLadderAbove(
            BlockState state,
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && DropLadder.hangsFromLadderAbove(state, level, pos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void rootboot$dropWithTheLadderAbove(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random,
            CallbackInfoReturnable<BlockState> cir) {
        if (directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
