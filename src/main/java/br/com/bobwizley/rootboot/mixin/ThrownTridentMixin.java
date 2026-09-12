package br.com.bobwizley.rootboot.mixin;

import br.com.bobwizley.rootboot.feature.extraloyaltridents.ExtraLoyalTridents;
import br.com.bobwizley.rootboot.feature.extraloyaltridents.VoidHeldTrident;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The whole tick is what gets skipped, because every way a trident in the void is lost lives
 * inside it: the loyalty branch turns a trident whose owner cannot take it back into a dropped
 * item, and the entity tick that follows destroys anything 64 blocks under the world. A trident
 * that skips it keeps hanging exactly where it is, still saved with its chunk, until the owner is
 * available again and the vanilla return takes over untouched.
 *
 * <p>{@code dealtDamage} is what arms that return. A trident that never hit anything has it unset
 * and would only keep falling, so entering the void sets it — the same state a trident that landed
 * carries, and the one the return already expects.
 */
@Mixin(ThrownTrident.class)
abstract class ThrownTridentMixin implements VoidHeldTrident {

    @Unique
    private static final String ROOTBOOT_HELD_IN_VOID = "rootboot_held_in_void";

    @Shadow
    private boolean dealtDamage;

    @Unique
    private boolean rootboot$heldInVoid;

    @Shadow
    private boolean isAcceptibleReturnOwner() {
        throw new AssertionError("Replaced by the mixin processor");
    }

    @Override
    public boolean rootboot$isHeldInVoid() {
        return rootboot$heldInVoid;
    }

    @Override
    public void rootboot$setHeldInVoid(boolean heldInVoid) {
        rootboot$heldInVoid = heldInVoid;
    }

    @Override
    public void rootboot$armLoyaltyReturn() {
        dealtDamage = true;
    }

    @Override
    public boolean rootboot$hasAcceptableReturnOwner() {
        return isAcceptibleReturnOwner();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void rootboot$holdInVoid(CallbackInfo ci) {
        if (ExtraLoyalTridents.holdInVoid((ThrownTrident) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void rootboot$saveVoidHold(ValueOutput output, CallbackInfo ci) {
        if (rootboot$heldInVoid) {
            output.putBoolean(ROOTBOOT_HELD_IN_VOID, true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void rootboot$loadVoidHold(ValueInput input, CallbackInfo ci) {
        rootboot$heldInVoid = input.getBooleanOr(ROOTBOOT_HELD_IN_VOID, false);
    }
}
