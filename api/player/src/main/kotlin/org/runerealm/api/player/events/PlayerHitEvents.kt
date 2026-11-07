package org.runerealm.api.player.events

import org.runerealm.events.UnboundEvent
import org.runerealm.game.entity.Player
import org.runerealm.game.hit.Hit

public class PlayerHitEvents {
    /**
     * Published once a queued hit lands on [player] and its [damage] has been taken from their
     * hitpoints. [damage] is the damage actually dealt (capped to the player's hitpoints).
     *
     * Every subscriber receives this event, which makes it the hook for "on hit taken" effects such
     * as the ring of recoil or poisoned weapons used by other players.
     */
    public data class Impact(
        public val player: Player,
        public val hit: Hit,
        public val damage: Int,
    ) : UnboundEvent
}
