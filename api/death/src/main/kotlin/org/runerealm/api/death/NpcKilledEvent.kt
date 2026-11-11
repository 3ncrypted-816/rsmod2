package org.runerealm.api.death

import org.runerealm.events.UnboundEvent
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player

/**
 * Published when [npc] starts its death sequence through [NpcDeath], with [killer] being the player
 * who dealt it the most damage.
 */
public data class NpcKilledEvent(val npc: Npc, val killer: Player) : UnboundEvent
