package org.runerealm.content.skills.magic.teleports

import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.spells.runes.MAGIC_RUNE_SCRIPTS
import org.runerealm.api.teleblock.TeleblockScript
import org.runerealm.api.teleblock.isTeleblocked
import org.runerealm.api.teleblock.teleblock
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.game.entity.Player
import org.runerealm.game.inv.InvObj
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript

class SpellbookTeleportsTest {
    private val start = CoordGrid(3200, 3200, 0)
    private val varrock = CoordGrid(3213, 3424, 0)

    @Test
    fun GameTestState.`varrock teleport consumes runes, gives xp and lands after 3 ticks`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            player.teleport(start)
            player.setBaseLevel(stats.magic, 25)
            player.setCurrentLevel(stats.magic, 25)
            player.giveVarrockRunes()
            val xp = player.statMap.getXP(stats.magic)

            openSpellbookFor(player)
            player.ifButton(spellbook_teleport_components.varrock)
            advance(ticks = 3)
            assertEquals(start, player.coords)
            advance(ticks = 1)
            assertEquals(varrock, player.coords)
            assertEquals(0, player.inv.count(objs.law_rune))
            assertEquals(0, player.inv.count(objs.air_rune))
            assertEquals(0, player.inv.count(objs.fire_rune))
            assertEquals(xp + 35, player.statMap.getXP(stats.magic))
        }

    @Test
    fun GameTestState.`cannot teleport above level 20 wilderness`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            val deepWilderness = CoordGrid(3100, 3700, 0) // Level 21+.
            player.teleport(deepWilderness)
            player.setBaseLevel(stats.magic, 25)
            player.setCurrentLevel(stats.magic, 25)
            player.giveVarrockRunes()

            openSpellbookFor(player)
            player.ifButton(spellbook_teleport_components.varrock)
            advance(ticks = 1)
            assertMessageSent("A mysterious force blocks your teleport spell!")
            advance(ticks = 5)
            assertEquals(deepWilderness, player.coords)
            assertEquals(1, player.inv.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`tele block prevents teleports`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class, TeleblockScript::class)) {
            val wilderness = CoordGrid(3100, 3540, 0) // Low-level wilderness.
            player.teleport(wilderness)
            player.setBaseLevel(stats.magic, 25)
            player.setCurrentLevel(stats.magic, 25)
            player.giveVarrockRunes()
            advance(ticks = 1)
            assertTrue(player.teleblock(cycles = 500))

            openSpellbookFor(player)
            player.ifButton(spellbook_teleport_components.varrock)
            advance(ticks = 1)
            assertMessageSent(
                "A Tele Block spell has been cast on you. It will expire in 5 minutes."
            )
            advance(ticks = 5)
            assertEquals(wilderness, player.coords)
            assertEquals(1, player.inv.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`leaving the wilderness removes tele block`() =
        runGameTest(*spellTestScripts(TeleblockScript::class)) {
            player.teleport(CoordGrid(3100, 3540, 0))
            advance(ticks = 1)
            assertTrue(player.teleblock(cycles = 500))
            assertFalse(player.teleblock(cycles = 500)) // Immune while blocked.
            player.teleport(start)
            advance(ticks = 3)
            assertFalse(player.isTeleblocked())
        }

    @Test
    fun GameTestState.`magic level is required`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            player.teleport(start)
            player.setBaseLevel(stats.magic, 24)
            player.setCurrentLevel(stats.magic, 24)
            player.giveVarrockRunes()
            openSpellbookFor(player)
            player.ifButton(spellbook_teleport_components.varrock)
            advance(ticks = 1)
            assertMessageSent("Your Magic level is not high enough for this spell.")
            advance(ticks = 4)
            assertEquals(start, player.coords)
        }

    @Test
    fun GameTestState.`house teleport only gives a message`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            player.teleport(start)
            player.setBaseLevel(stats.magic, 40)
            player.setCurrentLevel(stats.magic, 40)
            openSpellbookFor(player)
            player.ifButton(spellbook_teleport_components.house)
            advance(ticks = 1)
            assertMessageSent("You don't have a house to teleport to.")
        }

    @Test
    fun GameTestState.`tele other lumbridge teleports the target once accepted`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            val target = registerPlayer(CoordGrid(3202, 3200, 0))
            allocZoneCollision(target.coords)
            target.setVarBit(varbits.option_acceptaid, 1)
            player.teleport(start)
            player.setBaseLevel(stats.magic, 74)
            player.setCurrentLevel(stats.magic, 74)
            player.inv[0] = InvObj(objs.law_rune, 1)
            player.inv[1] = InvObj(objs.soul_rune, 1)
            player.inv[2] = InvObj(objs.earth_rune, 1)

            player.opPlayerT(target, spellbook_teleport_components.teleother_lumbridge)
            advance(ticks = 2)
            assertTrue(target.ui.containsModal(teleother_interfaces.teleport_other))
            assertEquals(0, player.inv.count(objs.soul_rune))

            target.ifButton(teleother_components.accept)
            advance(ticks = 5)
            assertEquals(CoordGrid(3221, 3218, 0), target.coords)
        }

    @Test
    fun GameTestState.`tele other needs the target to accept aid`() =
        runGameTest(*spellTestScripts(SpellbookTeleports::class)) {
            val target = registerPlayer(CoordGrid(3202, 3200, 0))
            allocZoneCollision(target.coords)
            target.setVarBit(varbits.option_acceptaid, 0)
            player.teleport(start)
            player.setBaseLevel(stats.magic, 74)
            player.setCurrentLevel(stats.magic, 74)
            player.inv[0] = InvObj(objs.law_rune, 1)
            player.inv[1] = InvObj(objs.soul_rune, 1)
            player.inv[2] = InvObj(objs.earth_rune, 1)

            player.opPlayerT(target, spellbook_teleport_components.teleother_lumbridge)
            advance(ticks = 3)
            assertFalse(target.ui.containsModal(teleother_interfaces.teleport_other))
            assertEquals(1, player.inv.count(objs.soul_rune))
        }

    private fun GameTestScope.openSpellbookFor(player: Player) {
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
    }

    private fun Player.giveVarrockRunes() {
        inv[0] = InvObj(objs.law_rune, 1)
        inv[1] = InvObj(objs.air_rune, 3)
        inv[2] = InvObj(objs.fire_rune, 1)
    }
}

internal object spellbook_teleport_components : ComponentReferences() {
    val varrock = find("magic_spellbook:varrock_teleport")
    val house = find("magic_spellbook:teleport_to_your_house")
    val teleother_lumbridge = find("magic_spellbook:teleother_lumbridge")
}

private fun spellTestScripts(
    vararg scripts: KClass<out PluginScript>
): Array<KClass<out PluginScript>> = (MAGIC_RUNE_SCRIPTS + scripts).toTypedArray()
