package br.com.bobwizley.rootboot.feature.biomediscovery;

import br.com.bobwizley.rootboot.feature.Feature;
import br.com.bobwizley.rootboot.feature.discovery.DiscoveryTitle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Announces the first visit to each registered biome for each player. Join and reconnect are
 * checked immediately; subsequent checks happen only after the player changes block or dimension.
 * Discoveries are permanent for the world and are never rebased on reconnect or feature re-enable.
 * A biome is recorded only once its title can actually be shown, so one that comes up while
 * another discovery is still on screen waits for a later check.
 */
public final class BiomeDiscoveryFeature implements Feature {

    public static final String ID = "biome_discovery";

    private static final int FADE_IN = 20;
    private static final int STAY = 95;
    private static final int FADE_OUT = 20;

    private final Map<UUID, PlayerLocation> locations = new HashMap<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            locations.put(player.getUUID(), PlayerLocation.of(player));
            discover(server, player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                locations.remove(handler.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PlayerLocation location = PlayerLocation.of(player);
                if (!location.equals(locations.put(player.getUUID(), location))) {
                    discover(server, player);
                }
            }
        });
    }

    private static void discover(MinecraftServer server, ServerPlayer player) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        biome.unwrapKey().ifPresent(key -> discover(server, player, key.identifier()));
    }

    private static void discover(MinecraftServer server, ServerPlayer player, Identifier biomeId) {
        BiomeDiscoveryState state = state(server);
        if (state.discovered(player.getUUID(), biomeId)) {
            return;
        }
        if (DiscoveryTitle.claim(player, FADE_IN + STAY + FADE_OUT)) {
            state.discover(player.getUUID(), biomeId);
            present(player, biomeId);
        }
    }

    private static void present(ServerPlayer player, Identifier biomeId) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(FADE_IN, STAY, FADE_OUT));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                BiomeDisplayName.component(biomeId).withStyle(Style.EMPTY.withColor(0xFFFF55))));

        Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_IN);
        for (int repeat = 0; repeat < 3; repeat++) {
            player.connection.send(new ClientboundSoundEntityPacket(
                    sound,
                    SoundSource.MASTER,
                    player,
                    1.0F,
                    1.0F,
                    player.getRandom().nextLong()));
        }
    }

    private static BiomeDiscoveryState state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BiomeDiscoveryState.TYPE);
    }

    private record PlayerLocation(ResourceKey<Level> dimension, BlockPos block) {

        private static PlayerLocation of(ServerPlayer player) {
            return new PlayerLocation(player.level().dimension(), player.blockPosition());
        }
    }
}
