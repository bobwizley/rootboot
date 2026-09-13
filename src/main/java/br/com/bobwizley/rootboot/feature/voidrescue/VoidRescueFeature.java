package br.com.bobwizley.rootboot.feature.voidrescue;

import br.com.bobwizley.rootboot.feature.Feature;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Void Rescue: an {@code out_of_world} kill activates and consumes a totem through the vanilla
 * flow, and the player it saves gets sixty seconds to climb out of the void.
 *
 * <p>The activation itself is decided at the vanilla call site, in the {@code LivingEntity} mixin;
 * what is registered here is the deadline that follows it.
 */
public final class VoidRescueFeature implements Feature {

    public static final String ID = "void_rescue";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        VoidRescue.enable();
        ServerTickEvents.END_SERVER_TICK.register(VoidRescue::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> !VoidRescue.shieldsFromVoid(entity, source));
    }
}
