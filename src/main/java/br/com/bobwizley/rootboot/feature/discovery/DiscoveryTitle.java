package br.com.bobwizley.rootboot.feature.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * The single title slot every player's client owns. A discovery announced while the previous title
 * is still on screen replaces it, so a feature that cannot claim the slot must leave its discovery
 * unrecorded and announce it from a later check: what is recorded is exactly what was shown.
 */
public final class DiscoveryTitle {

    private static final Map<UUID, Long> occupiedUntil = new HashMap<>();

    private DiscoveryTitle() {
    }

    public static boolean claim(ServerPlayer player, int durationTicks) {
        return claim(player.getUUID(), player.level().getServer().getTickCount(), durationTicks);
    }

    static boolean claim(UUID playerId, long tick, int durationTicks) {
        occupiedUntil.values().removeIf(deadline -> deadline <= tick);
        if (occupiedUntil.containsKey(playerId)) {
            return false;
        }
        occupiedUntil.put(playerId, tick + durationTicks);
        return true;
    }
}
