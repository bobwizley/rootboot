package br.com.bobwizley.rootboot.feature.dropladder;

import br.com.bobwizley.rootboot.feature.Feature;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Drop Ladder: while a player stands in a ladder column holding a ladder, each new sneak press
 * hangs exactly one more ladder under the bottom end of the column.
 *
 * <p>What keeps a hanging ladder up is {@link DropLadder#hangsFromLadderAbove}, which is not part
 * of this registration: see the note there.
 */
public final class DropLadderFeature implements Feature {

    public static final String ID = "drop_ladder";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void register() {
        DropLadder.enable();
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> DropLadder.forget(handler.player));
    }
}
