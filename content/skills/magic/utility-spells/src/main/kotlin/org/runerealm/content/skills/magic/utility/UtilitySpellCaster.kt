package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.combat.manager.MagicRuneManager
import org.runerealm.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varps
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.ObjType

/** Shared casting helpers for the standard spellbook's non-combat spells. */
class UtilitySpellCaster
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    fun spell(obj: ObjType): MagicSpell =
        checkNotNull(spells.getObjSpell(obj)) { "Spell not found: $obj" }

    /**
     * Checks the spell's level, spellbook and rune requirements without consuming anything. The
     * relevant failure message is sent to the player.
     */
    fun canCast(player: Player, spell: MagicSpell): Boolean = runes.canCastSpell(player, spell)

    /**
     * Consumes the spell's runes and gives its cast experience.
     *
     * @return `false` if the requirements are not met (a message is sent to the player).
     */
    fun cast(access: ProtectedAccess, spell: MagicSpell, xp: Double = spell.castXp): Boolean {
        val result = runes.attemptCast(access.player, spell)
        if (result.isFailure()) {
            return false
        }
        access.statAdvance(stats.magic, xp)
        return true
    }
}

private var Player.spellDelayEnd by intVarp(varps.spell_delay_end)

/**
 * @return `true` while a previous utility spell (such as High Level Alchemy) is still being cast.
 *   The client ignores further casts until the delay runs out.
 */
fun Player.isUtilitySpellDelayed(): Boolean {
    val end = spellDelayEnd
    return end != 0 && currentMapClock < end
}

/** Blocks further utility spells for [cycles] game cycles, e.g. 5 for High Level Alchemy. */
fun Player.setUtilitySpellDelay(cycles: Int) {
    spellDelayEnd = currentMapClock + cycles
}
