package br.com.bobwizley.rootboot.mixin;

import br.com.bobwizley.rootboot.feature.deathitemprotection.DeathItemProtection;
import br.com.bobwizley.rootboot.feature.deathitemprotection.DeathDroppingPlayer;
import br.com.bobwizley.rootboot.feature.halfhealthbabies.HalfHealthBabies;
import br.com.bobwizley.rootboot.feature.voidrescue.VoidRescue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void rootboot$updateBabyHealthReduction(CallbackInfo ci) {
        HalfHealthBabies.applyCurrentPolicy((LivingEntity) (Object) this);
    }

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void rootboot$protectDeathDrop(CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity item = cir.getReturnValue();
        if ((Object) this instanceof Player player
                && item != null
                && player.level() instanceof ServerLevel
                && ((DeathDroppingPlayer) player).rootboot$isDroppingDeathItems()) {
            DeathItemProtection.protect(item);
        }
    }

    /**
     * The only thing keeping a totem out of the void is this guard, which refuses every damage type
     * that bypasses invulnerability. Answering it for the void alone is what makes Void Rescue an
     * extension of the vanilla flow rather than a second one: the search through both hands, the
     * totem that is consumed, the statistic, the advancement, the health it restores, its effects
     * and the animation every client already knows are all vanilla's, untouched.
     */
    @ModifyExpressionValue(
            method = "checkTotemDeathProtection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/DamageSource;"
                            + "is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean rootboot$letTheVoidReachTheTotem(
            boolean bypassesInvulnerability, DamageSource killingDamage) {
        return bypassesInvulnerability
                && !VoidRescue.reachesTotem((LivingEntity) (Object) this, killingDamage);
    }

    /**
     * A rescue starts on the return rather than on the call above, because only the return knows
     * whether a totem was actually found and consumed.
     */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void rootboot$beginVoidRescue(
            DamageSource killingDamage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            VoidRescue.begin((LivingEntity) (Object) this, killingDamage);
        }
    }
}
