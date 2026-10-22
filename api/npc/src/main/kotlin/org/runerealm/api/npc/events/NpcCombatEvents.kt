package org.runerealm.api.npc.events

import org.runerealm.events.KeyedEvent
import org.runerealm.events.UnboundEvent
import org.runerealm.game.entity.Npc
import org.runerealm.game.hit.Hit
import org.runerealm.game.hit.HitBuilder

public class NpcHitEvents {
    public data class Modify(
        public val npc: Npc,
        public val hit: HitBuilder,
        override val id: Long = npc.visType.id.toLong(),
    ) : KeyedEvent

    public data class Impact(
        public val npc: Npc,
        public val hit: Hit,
        override val id: Long = npc.visType.id.toLong(),
    ) : KeyedEvent

    /**
     * Published for every hit that lands on any npc, after [Impact]. Unlike [Impact], which is
     * keyed by npc type and allows one handler per type, every subscriber receives this event. It
     * is the hook for generic "on hit dealt" effects such as poisoned weapons.
     */
    public data class AnyImpact(public val npc: Npc, public val hit: Hit) : UnboundEvent
}
