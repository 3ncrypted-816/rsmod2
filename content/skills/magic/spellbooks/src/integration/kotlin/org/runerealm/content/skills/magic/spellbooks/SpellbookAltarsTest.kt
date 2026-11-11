package org.runerealm.content.skills.magic.spellbooks

import org.junit.jupiter.api.Test
import org.runerealm.api.combat.commons.magic.Spellbook
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.player.stat.stat
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.testing.GameTestState
import org.runerealm.map.CoordGrid

class SpellbookAltarsTest {
    private val altarCoords = CoordGrid(0, 50, 50, 22, 18)

    @Test
    fun GameTestState.`the ancient altar has a pray-at op`() = runGameTest {
        assertEquals("Pray-at", locTypes.getValue(spellbook_locs.ancient_altar.id).op[0])
    }

    @Test
    fun GameTestState.`praying at the ancient altar toggles the spellbook`() =
        runGameTest(SpellbookAltars::class) {
            player.stats[stats.prayer] = 43
            allocZoneCollision(altarCoords)
            val altar = placeMapLoc(altarCoords, locTypes.getValue(spellbook_locs.ancient_altar.id))
            player.teleport(altarCoords.translateX(-1))

            player.opLoc1(altar)
            advanceUntil(predicate = { player.vars[varbits.spellbook] == ANCIENTS })
            assertMessageSent(SpellbookAltars.TO_ANCIENTS_MESSAGE)
            assertEquals(0, player.stat(stats.prayer))

            advance(ticks = 2)
            player.opLoc1(altar)
            advanceUntil(predicate = { player.vars[varbits.spellbook] == STANDARD })
            assertMessageSent(SpellbookAltars.TO_STANDARD_MESSAGE)
        }

    @Test
    fun GameTestState.`switching spellbooks resets autocast`() =
        runGameTest(SpellbookAltars::class) {
            allocZoneCollision(altarCoords)
            val altar = placeMapLoc(altarCoords, locTypes.getValue(spellbook_locs.ancient_altar.id))
            player.teleport(altarCoords.translateX(-1))
            VarPlayerIntMapSetter.set(player, varbits.autocast_set, 1)
            VarPlayerIntMapSetter.set(player, varbits.autocast_spell, 3)
            VarPlayerIntMapSetter.set(player, varbits.saved_autocast_spell_staff, 3)

            player.opLoc1(altar)
            advanceUntil(predicate = { player.vars[varbits.spellbook] == ANCIENTS })
            assertEquals(0, player.vars[varbits.autocast_set])
            assertEquals(0, player.vars[varbits.autocast_spell])
            assertEquals(0, player.vars[varbits.saved_autocast_spell_staff])
        }

    private companion object {
        val ANCIENTS = Spellbook.Ancients.varValue
        val STANDARD = Spellbook.Standard.varValue
    }
}
