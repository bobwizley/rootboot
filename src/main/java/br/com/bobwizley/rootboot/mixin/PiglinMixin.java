package br.com.bobwizley.rootboot.mixin;

import br.com.bobwizley.rootboot.feature.trimmedarmoredpiglins.TrimmedArmoredPiglins;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code finalizeSpawn} is where a newborn piglin's armor is already populated, and loading a
 * piglin from disk never reaches it. Piglin brutes are a sibling of {@link Piglin} rather than a
 * subclass, so targeting this class is what leaves them out.
 *
 * <p>A piglin created from authored NBT — {@code /summon}, a spawner carrying spawn data, a spawn
 * egg holding {@code entity_data} — is deliberately left alone, because vanilla skips or overwrites
 * everything {@code finalizeSpawn} decides on those paths. Its armor is the one whoever wrote that
 * NBT chose, and a rolled trim would overwrite an explicit choice rather than decorate a random one.
 */
@Mixin(Piglin.class)
abstract class PiglinMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void rootboot$trimSpawnedArmor(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {
        TrimmedArmoredPiglins.trimOnSpawn((Piglin) (Object) this, level);
    }
}
