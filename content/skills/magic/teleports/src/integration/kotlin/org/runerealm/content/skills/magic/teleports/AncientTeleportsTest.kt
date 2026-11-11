package org.runerealm.content.skills.magic.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.runerealm.api.combat.commons.magic.Spellbook
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.spells.runes.MAGIC_RUNE_SCRIPTS
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.game.inv.InvObj
import org.runerealm.map.CoordGrid

class AncientTeleportsTest {
    private val start = CoordGrid(3200, 3200, 0)

    @Test
    fun GameTestState.`paddewwa teleport lands in the edgeville dungeon after 4 ticks`() =
        runGameTest(*(MAGIC_RUNE_SCRIPTS + SpellbookTeleports::class).toTypedArray()) {
            setup(Spellbook.Ancients)
            val type = objTypes[objs.spell_paddewwa_teleport]
            val dest = type.param(params.spell_telecoord)
            assertEquals(PADDEWWA, dest)
            val xp = player.statMap.getXP(stats.magic)

            player.ifButton(type.param(params.spell_button))
            advance(ticks = 4)
            assertEquals(start, player.coords)
            advance(ticks = 1)
            assertEquals(PADDEWWA, player.coords)
            assertEquals(0, player.inv.count(objs.law_rune))
            assertEquals(0, player.inv.count(objs.air_rune))
            assertEquals(0, player.inv.count(objs.fire_rune))
            assertEquals(xp + 64, player.statMap.getXP(stats.magic))
        }

    @Test
    fun GameTestState.`ancient teleports need the ancient spellbook`() =
        runGameTest(*(MAGIC_RUNE_SCRIPTS + SpellbookTeleports::class).toTypedArray()) {
            setup(Spellbook.Standard)
            player.ifButton(objTypes[objs.spell_paddewwa_teleport].param(params.spell_button))
            advance(ticks = 6)
            assertEquals(start, player.coords)
            assertEquals(2, player.inv.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`every ancient teleport has a destination`() = runGameTest {
        for (spell in ANCIENT_TELEPORTS) {
            val dest = objTypes[spell].param(params.spell_telecoord)
            assert(dest != CoordGrid.ZERO) { "No destination for $spell" }
        }
    }

    @Test
    fun GameTestState.`edgeville home teleport works on the ancient spellbook only`() =
        runGameTest(HomeTeleport::class) {
            player.teleport(start)
            player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
            player.ifButton(home_teleport_components.ancient)
            advance(ticks = 30)
            assertEquals(start, player.coords)

            VarPlayerIntMapSetter.set(player, varbits.spellbook, Spellbook.Ancients.varValue)
            player.ifButton(home_teleport_components.ancient)
            advance(ticks = 25)
            assertEquals(EDGEVILLE_HOME, player.coords)
        }

    private fun GameTestScope.setup(book: Spellbook) {
        player.teleport(start)
        VarPlayerIntMapSetter.set(player, varbits.spellbook, book.varValue)
        player.setBaseLevel(stats.magic, 54)
        player.setCurrentLevel(stats.magic, 54)
        player.inv[0] = InvObj(objs.law_rune, 2)
        player.inv[1] = InvObj(objs.air_rune, 1)
        player.inv[2] = InvObj(objs.fire_rune, 1)
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
    }

    private companion object {
        /** Paddewwa Teleport: the Edgeville Dungeon (the cache coordinate). */
        val PADDEWWA = CoordGrid(3098, 9882, 0)
    }
}
