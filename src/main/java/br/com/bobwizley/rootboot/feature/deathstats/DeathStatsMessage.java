package br.com.bobwizley.rootboot.feature.deathstats;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The announcement published to everyone online after a death. It is built from translatable
 * components so that every client renders it in its own language from the same server message.
 *
 * <p>Each key carries its English text as a fallback because this is a server-side feature: a
 * client without RootBoot has no catalogue for these keys and would otherwise render the bare key,
 * dropping the name, the time and the count. The fallbacks are the same strings the mod's own
 * en_us catalogue holds, which {@code DeathStatsMessageTest} keeps them equal to.
 */
public final class DeathStatsMessage {

    public static final String KEY = "message.rootboot.death_stats";
    public static final String FIRST_KEY = KEY + ".first";

    static final String FALLBACK =
            "%s survived %s since their last death and now has a total of %s deaths";
    static final String FIRST_FALLBACK =
            "%s survived %s since spawning into the world and now has a total of %s death";

    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;

    private DeathStatsMessage() {
    }

    public static Component of(Component playerName, long survivedTicks, int deaths) {
        boolean first = deaths == 1;
        return Component.translatableWithFallback(
                first ? FIRST_KEY : KEY,
                first ? FIRST_FALLBACK : FALLBACK,
                playerName,
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
