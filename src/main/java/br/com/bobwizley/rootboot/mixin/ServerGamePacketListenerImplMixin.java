package br.com.bobwizley.rootboot.mixin;

import br.com.bobwizley.rootboot.feature.dropladder.DropLadder;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client reports every change it makes to its own input, so this is where each press and
 * release of the sneak key arrives — several of them at once when the client is catching up. A
 * handler sampling the key once per tick would read a release and the press behind it as no change.
 *
 * <p>Reading the key after vanilla applied it keeps the gate vanilla puts on a client that has not
 * finished loading: until then the key vanilla exposes is the one the player kept.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At("TAIL"))
    private void rootboot$readSneakInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        DropLadder.readSneakInput(player);
    }
}
