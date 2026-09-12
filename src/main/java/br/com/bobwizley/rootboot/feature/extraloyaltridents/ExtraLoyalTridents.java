package br.com.bobwizley.rootboot.feature.extraloyaltridents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class ExtraLoyalTridents {

    /**
     * Where a held trident waits, measured under the world floor. One block is enough to keep it
     * clear of the height that ends the hold while leaving the 63 remaining blocks of the drop
     * vanilla allows before it destroys whatever fell into the void.
     */
    private static final double HOLD_DEPTH = 1.0;

    private static boolean enabled;

    private ExtraLoyalTridents() {
    }

    static void enable() {
        enabled = true;
    }

    static void disable() {
        enabled = false;
    }

    /**
     * Decides what an eligible trident does before its vanilla tick runs.
     *
     * <p>A trident already held keeps being held even when the feature is disabled: releasing it
     * would drop it back into the fall this feature saved it from.
     *
     * @return {@code true} when the trident must skip this tick entirely, which is how it waits in
     *     the void while its owner cannot receive it — the vanilla tick would otherwise either
     *     destroy it below the world or turn it into an item dropped where it hangs.
     */
    public static boolean holdInVoid(ThrownTrident trident) {
        if (!(trident.level() instanceof ServerLevel level)) {
            return false;
        }

        VoidHeldTrident held = (VoidHeldTrident) trident;
        double floor = level.getMinY();
        if (trident.getY() >= floor) {
            held.rootboot$setHeldInVoid(false);
            return false;
        }
        if (!held.rootboot$isHeldInVoid()) {
            if (!enabled || !isLoyal(trident, level)) {
                return false;
            }
            held.rootboot$setHeldInVoid(true);
            held.rootboot$armLoyaltyReturn();
        }
        if (trident.getY() < floor - HOLD_DEPTH) {
            trident.setPos(trident.getX(), floor - HOLD_DEPTH, trident.getZ());
            trident.setDeltaMovement(Vec3.ZERO);
        }
        return !held.rootboot$hasAcceptableReturnOwner();
    }

    private static boolean isLoyal(ThrownTrident trident, ServerLevel level) {
        return EnchantmentHelper.getTridentReturnToOwnerAcceleration(
                level, trident.getPickupItemStackOrigin(), trident) > 0;
    }
}
