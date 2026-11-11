package org.runerealm.content.skills.magic.spellbooks

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.Spellbook
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.stat
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.player.vars.enumVarBit
import org.runerealm.api.script.onOpLoc1
import org.runerealm.api.spells.autocast.AutocastWeapons
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.WeaponCategory
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Spellbook switching at the
 * [Ancient Pyramid altar](https://oldschool.runescape.wiki/w/Altar_(Ancient_Pyramid)):
 * - Praying at the altar switches to the Ancient Magicks ("You feel a strange wisdom fill your
 *   mind....") and, on the Ancient Magicks, back to the standard spellbook ("You feel a strange
 *   drain upon your memory...").
 * - It drains all of the player's Prayer points, but can be used with none.
 * - Switching spellbooks clears the autocast spell, both the active one and the ones saved for
 *   staves and bladed staves, since they belong to the old book.
 *
 * Desert Treasure I is not required, since quests are not tracked by this server. Players on the
 * Lunar or Arceuus spellbook are switched to the Ancient Magicks.
 */
class SpellbookAltars @Inject constructor(private val autocast: AutocastWeapons) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(spellbook_locs.ancient_altar) { prayAtAncientAltar() }
    }

    private fun ProtectedAccess.prayAtAncientAltar() {
        anim(spellbook_seqs.pray)
        val prayer = player.stat(stats.prayer)
        if (prayer > 0) {
            statSub(stats.prayer, prayer, 0)
        }
        if (player.spellbook == Spellbook.Ancients) {
            switchSpellbook(player, Spellbook.Standard)
            mes(TO_STANDARD_MESSAGE)
        } else {
            switchSpellbook(player, Spellbook.Ancients)
            mes(TO_ANCIENTS_MESSAGE)
        }
    }

    /** Changes [player]'s spellbook and resets their autocast settings. */
    fun switchSpellbook(player: Player, book: Spellbook) {
        player.spellbook = book
        VarPlayerIntMapSetter.set(player, varbits.autocast_set, 0)
        VarPlayerIntMapSetter.set(player, varbits.autocast_spell, 0)
        VarPlayerIntMapSetter.set(player, varbits.autocast_defmode, 0)
        for (category in AUTOCAST_CATEGORIES) {
            val staffVarBits = autocast.getVarBits(category) ?: continue
            autocast.reset(player, staffVarBits)
        }
    }

    companion object {
        const val TO_ANCIENTS_MESSAGE: String = "You feel a strange wisdom fill your mind...."
        const val TO_STANDARD_MESSAGE: String = "You feel a strange drain upon your memory..."

        private val AUTOCAST_CATEGORIES = listOf(WeaponCategory.Staff, WeaponCategory.BladedStaff)
    }
}

private var Player.spellbook by enumVarBit<Spellbook>(varbits.spellbook)

typealias spellbook_locs = SpellbookLocs

internal typealias spellbook_seqs = SpellbookSeqs

object SpellbookLocs : LocReferences() {
    /** The altar in the Ancient Pyramid (loc 6552). */
    val ancient_altar = find("dt_zaros_altar")
}

object SpellbookSeqs : SeqReferences() {
    val pray = find("human_pray")
}
