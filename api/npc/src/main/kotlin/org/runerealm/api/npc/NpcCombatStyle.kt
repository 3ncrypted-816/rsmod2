package org.runerealm.api.npc

import org.runerealm.api.config.refs.params
import org.runerealm.game.entity.Npc
import org.runerealm.game.type.npc.UnpackedNpcType

/**
 * The style an npc attacks players with, read from the `npc_attack_style` param.
 *
 * Ranged and magic npcs attack from up to their npc type's `attackRange` tiles away (with line of
 * sight) instead of walking up to their target, so their combat interactions use "ap" (approach)
 * mode rather than "op" (adjacent) mode.
 */
public enum class NpcCombatStyle(public val id: Int) {
    Melee(0),
    Ranged(1),
    Magic(2);

    public val isDistanced: Boolean
        get() = this != Melee

    public companion object {
        public operator fun get(id: Int): NpcCombatStyle =
            entries.firstOrNull { it.id == id } ?: Melee
    }
}

public val UnpackedNpcType.combatStyle: NpcCombatStyle
    get() = NpcCombatStyle[param(params.npc_attack_style)]

public val Npc.combatStyle: NpcCombatStyle
    get() = visType.combatStyle

/** @return `true` if this npc attacks from a distance (ranged or magic). */
public fun Npc.usesDistancedCombat(): Boolean = combatStyle.isDistanced
