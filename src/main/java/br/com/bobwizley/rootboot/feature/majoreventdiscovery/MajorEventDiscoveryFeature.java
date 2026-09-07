package br.com.bobwizley.rootboot.feature.majoreventdiscovery;

import br.com.bobwizley.rootboot.feature.Feature;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;

/**
 * Announces the End and the Wither the first time each player meets them. Both are polled while
 * still undiscovered instead of being caught on a transition, so joining or reconnecting already
 * inside the dimension or inside the radius counts as the first encounter.
 */
public final class MajorEventDiscoveryFeature implements Feature {

    public static final String ID = "major_event_discovery";

    static final double WITHER_RADIUS = 64.0;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                discover(server, player);
            }
        });
    }

    static void discover(MinecraftServer server, ServerPlayer player) {
        MajorEventDiscoveryState state = state(server);
        for (MajorEvent event : MajorEvent.values()) {
            if (state.discovered(player.getUUID(), event)) {
                continue;
            }
            if (happening(event, player)) {
                state.discover(player.getUUID(), event);
                present(player, event);
            }
        }
    }

    private static boolean happening(MajorEvent event, ServerPlayer player) {
        return switch (event) {
            case END -> Level.END.equals(player.level().dimension());
            case WITHER -> nearWither(player);
        };
    }

    // The inflated bounding box is only a coarse filter: it has to reach past the radius so that a
    // Wither standing exactly on the boundary is still handed to the exact distance check.
    private static boolean nearWither(ServerPlayer player) {
        return player.level().hasEntities(
                EntityTypes.WITHER,
                player.getBoundingBox().inflate(WITHER_RADIUS),
                wither -> player.distanceToSqr(wither) <= WITHER_RADIUS * WITHER_RADIUS);
    }

    private static void present(ServerPlayer player, MajorEvent event) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable(event.titleKey())
                        .withStyle(Style.EMPTY.withColor(event.color()).withBold(true))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable(event.subtitleKey())));

        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event.sound());
        player.connection.send(new ClientboundSoundEntityPacket(
                sound,
                SoundSource.MASTER,
                player,
                1.0F,
                1.0F,
                player.getRandom().nextLong()));
    }

    static MajorEventDiscoveryState state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MajorEventDiscoveryState.TYPE);
    }
}
