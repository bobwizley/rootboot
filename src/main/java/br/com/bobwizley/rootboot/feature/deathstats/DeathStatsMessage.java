package br.com.bobwizley.rootboot.feature.deathstats;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The announcement published to everyone online after a death. It is built from translatable
 * components so that every client renders it in its own language from the same server message.
 */
public final class DeathStatsMessage {

    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;

    private DeathStatsMessage() {
    }

    public static Component of(ServerPlayer player, long survivedTicks, int deaths) {
        return Component.translatable(
                deaths == 1 ? "message.rootboot.death_stats.first" : "message.rootboot.death_stats",
                player.getDisplayName(),
                Component.literal(duration(survivedTicks)).withStyle(ChatFormatting.WHITE),
                Component.literal(Integer.toString(deaths)).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY);
    }

    public static String duration(long survivedTicks) {
        long totalSeconds = survivedTicks / TICKS_PER_SECOND;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;
        long minutes = totalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR;
        long hours = totalSeconds / SECONDS_PER_MINUTE / MINUTES_PER_HOUR;
        return "%d:%02d:%02d".formatted(hours, minutes, seconds);
    }
}
