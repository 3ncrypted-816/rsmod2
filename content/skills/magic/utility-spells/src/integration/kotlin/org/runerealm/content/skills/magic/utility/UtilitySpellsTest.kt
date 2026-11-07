package org.runerealm.content.skills.magic.utility

import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.stats
import org.runerealm.api.spells.charge.isChargeActive
import org.runerealm.api.spells.runes.MAGIC_RUNE_SCRIPTS
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.skills.smithing.configs.smithing_objs
import org.runerealm.game.entity.Player
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.obj.ObjType
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript

class UtilitySpellsTest {
    private val start = CoordGrid(3200, 3200, 0)

    @Test
    fun GameTestState.`high alchemy turns an item into 60 percent of its value`() =
        runGameTest(*spellTestScripts(Alchemy::class)) {
            setup(magic = 55)
            player.give(objs.nature_rune to 2, objs.fire_rune to 10)
            val target = utility_enchant_objs.ring_of_recoil
            player.inv[5] = InvObj(target, 1)
            val expected = objTypes[target].cost * 60 / 100

            player.opHeldT(utility_components.high_alchemy, invSlot = 5)
            advance(ticks = 1)
            assertEquals(0, player.inv.count(target))
            assertEquals(expected, player.inv.count(objs.coins))
            assertEquals(1, player.inv.count(objs.nature_rune))
            assertEquals(5, player.inv.count(objs.fire_rune))

            // The 5-tick recast delay ignores immediate re-casts.
            player.inv[6] = InvObj(target, 1)
            player.opHeldT(utility_components.high_alchemy, invSlot = 6)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(target))
            advance(ticks = 4)
            player.opHeldT(utility_components.high_alchemy, invSlot = 6)
            advance(ticks = 1)
            assertEquals(0, player.inv.count(target))
            assertEquals(expected * 2, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState.`low alchemy gives 40 percent and alchemises one of a stack`() =
        runGameTest(*spellTestScripts(Alchemy::class)) {
            setup(magic = 21)
            player.give(objs.nature_rune to 1, objs.fire_rune to 3)
            val target = utility_enchant_objs.ring_of_recoil
            val noted = objTypes.cert(objTypes[target])
            player.inv[5] = InvObj(noted, 3)

            player.opHeldT(utility_components.low_alchemy, invSlot = 5)
            advance(ticks = 1)
            assertEquals(2, player.inv[5]?.count)
            assertEquals(objTypes[target].cost * 40 / 100, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState.`alchemy warns before alchemising a valuable item`() =
        runGameTest(*spellTestScripts(Alchemy::class)) {
            setup(magic = 55)
            player.give(objs.nature_rune to 1, objs.fire_rune to 5)
            val target = utility_enchant_objs.amulet_of_glory
            assertTrue(objTypes[target].cost >= Alchemy.DEFAULT_WARNING_THRESHOLD)
            player.inv[5] = InvObj(target, 1)

            player.opHeldT(utility_components.high_alchemy, invSlot = 5)
            advance(ticks = 2)
            assertEquals(1, player.inv.count(target))
            assertEquals(1, player.inv.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`alchemy cannot be cast on coins`() =
        runGameTest(*spellTestScripts(Alchemy::class)) {
            setup(magic = 55)
            player.give(objs.nature_rune to 1, objs.fire_rune to 5)
            player.inv[5] = InvObj(objs.coins, 100)
            player.opHeldT(utility_components.high_alchemy, invSlot = 5)
            advance(ticks = 1)
            assertMessageSent(Alchemy.COINS_MESSAGE)
            assertEquals(100, player.inv.count(objs.coins))
        }

    @Test
    fun GameTestState.`superheat smelts iron with two coal into steel`() =
        runGameTest(*spellTestScripts(SuperheatItem::class)) {
            setup(magic = 43)
            player.setBaseLevel(stats.smithing, 30)
            player.setCurrentLevel(stats.smithing, 30)
            player.give(objs.nature_rune to 2, objs.fire_rune to 8)
            player.inv[5] = InvObj(smithing_objs.iron_ore, 1)
            player.inv[6] = InvObj(smithing_objs.coal, 1)
            player.inv[7] = InvObj(smithing_objs.coal, 1)
            val xp = player.statMap.getXP(stats.smithing)

            player.opHeldT(utility_components.superheat, invSlot = 5)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(smithing_objs.steel_bar))
            assertEquals(0, player.inv.count(smithing_objs.coal))
            assertEquals(xp + 17, player.statMap.getXP(stats.smithing))
        }

    @Test
    fun GameTestState.`superheat smelts iron without coal into iron and checks for ore`() =
        runGameTest(*spellTestScripts(SuperheatItem::class)) {
            setup(magic = 43)
            player.setBaseLevel(stats.smithing, 15)
            player.setCurrentLevel(stats.smithing, 15)
            player.give(objs.nature_rune to 2, objs.fire_rune to 8)
            player.inv[5] = InvObj(smithing_objs.iron_ore, 1)
            player.inv[6] = InvObj(objs.coins, 1)

            player.opHeldT(utility_components.superheat, invSlot = 6)
            advance(ticks = 1)
            assertMessageSent(SuperheatItem.NOT_ORE_MESSAGE)

            player.opHeldT(utility_components.superheat, invSlot = 5)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(smithing_objs.iron_bar))
        }

    @Test
    fun GameTestState.`superheat needs the smithing level and all ores`() =
        runGameTest(*spellTestScripts(SuperheatItem::class)) {
            setup(magic = 43)
            player.setBaseLevel(stats.smithing, 1)
            player.setCurrentLevel(stats.smithing, 1)
            player.give(objs.nature_rune to 2, objs.fire_rune to 8)
            player.inv[5] = InvObj(smithing_objs.copper_ore, 1)
            player.inv[6] = InvObj(smithing_objs.mithril_ore, 1)

            player.opHeldT(utility_components.superheat, invSlot = 5)
            advance(ticks = 1)
            assertMessageSent("You need one copper ore and one tin ore to make bronze.")

            player.opHeldT(utility_components.superheat, invSlot = 6)
            advance(ticks = 1)
            assertMessageSent("You need a Smithing level of 50 to smelt mithril.")
            assertEquals(2, player.inv.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`lvl-1 enchant turns a sapphire ring into a ring of recoil`() =
        runGameTest(*spellTestScripts(EnchantSpells::class)) {
            setup(magic = 7)
            player.give(objs.cosmic_rune to 1, objs.water_rune to 1)
            player.inv[5] = InvObj(utility_enchant_objs.sapphire_ring, 1)

            player.opHeldT(utility_components.enchant_1, invSlot = 5)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(utility_enchant_objs.ring_of_recoil))
            assertEquals(0, player.inv.count(objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`enchant spells only work on their own jewellery`() =
        runGameTest(*spellTestScripts(EnchantSpells::class)) {
            setup(magic = 93)
            player.give(objs.cosmic_rune to 1, objs.soul_rune to 20, objs.blood_rune to 20)
            player.inv[5] = InvObj(utility_enchant_objs.sapphire_ring, 1)
            player.opHeldT(utility_components.enchant_7, invSlot = 5)
            advance(ticks = 1)
            assertMessageSent(EnchantSpells.WRONG_ITEM_MESSAGE)

            player.inv[6] = InvObj(utility_enchant_objs.zenyte_amulet, 1)
            player.opHeldT(utility_components.enchant_7, invSlot = 6)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(utility_enchant_objs.amulet_of_torture))
        }

    @Test
    fun GameTestState.`enchant crossbow bolts enchants sets of ten`() =
        runGameTest(*spellTestScripts(EnchantSpells::class)) {
            setup(magic = 4)
            player.give(objs.cosmic_rune to 2, objs.air_rune to 4)
            player.inv[5] = InvObj(utility_enchant_objs.opal_bolts, 15)

            player.ifButton(utility_components.enchant_bolts)
            advance(ticks = 1)
            assertTrue(player.ui.containsModal(interfaces.skillmulti))
            player.resumePauseButton(components.skillmulti_a, sub = 2)
            advance(ticks = 1)
            assertEquals(10, player.inv.count(utility_enchant_objs.opal_bolts_e))
            advance(ticks = 4)
            assertEquals(15, player.inv.count(utility_enchant_objs.opal_bolts_e))
            assertEquals(0, player.inv.count(objs.cosmic_rune))
        }

    @Test
    fun GameTestState.`bones to bananas converts every bone`() =
        runGameTest(*spellTestScripts(BonesToFruit::class)) {
            setup(magic = 15)
            player.give(objs.nature_rune to 1, objs.water_rune to 2, objs.earth_rune to 2)
            player.inv[5] = InvObj(utility_objs.bones, 1)
            player.inv[6] = InvObj(utility_objs.bones, 1)
            player.inv[7] = InvObj(utility_objs.big_bones, 1)

            player.ifButton(utility_components.bones_bananas)
            advance(ticks = 1)
            assertEquals(2, player.inv.count(utility_objs.banana))
            assertEquals(1, player.inv.count(utility_objs.big_bones))
        }

    @Test
    fun GameTestState.`bones to peaches also converts big bones`() =
        runGameTest(*spellTestScripts(BonesToFruit::class)) {
            setup(magic = 60)
            player.give(objs.nature_rune to 2, objs.water_rune to 4, objs.earth_rune to 4)
            player.inv[5] = InvObj(utility_objs.bones, 1)
            player.inv[6] = InvObj(utility_objs.big_bones, 1)

            player.ifButton(utility_components.bones_peaches)
            advance(ticks = 1)
            assertEquals(2, player.inv.count(utility_objs.peach))
        }

    @Test
    fun GameTestState.`bones to bananas needs bones`() =
        runGameTest(*spellTestScripts(BonesToFruit::class)) {
            setup(magic = 15)
            player.give(objs.nature_rune to 1, objs.water_rune to 2, objs.earth_rune to 2)
            player.ifButton(utility_components.bones_bananas)
            advance(ticks = 1)
            assertMessageSent(BonesToFruit.NO_BONES_MESSAGE)
            assertEquals(1, player.inv.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`telekinetic grab picks up an item from a distance`() =
        runGameTest(*spellTestScripts(TelekineticGrab::class)) {
            setup(magic = 33)
            player.give(objs.law_rune to 1, objs.air_rune to 1)
            // Pick a free tile a few squares away that the player can see (the real map is loaded).
            val tile =
                (2..6)
                    .flatMap { d -> listOf(start.translate(d, 0), start.translate(0, d)) }
                    .first {
                        collision[it.x, it.z, it.level] == 0 &&
                            rayCastValidator.hasLineOfSight(start, it)
                    }
            val obj = spawnObj(tile, utility_objs.banana)
            advance(ticks = 1)

            player.opObjT(obj, utility_components.telegrab)
            advance(ticks = 1)
            assertEquals(0, player.inv.count(objs.law_rune))
            advance(ticks = 3)
            assertEquals(1, player.inv.count(utility_objs.banana))
            assertEquals(start, player.coords)
            assertEquals(0, player.inv.count(objs.law_rune))
        }

    @Test
    fun GameTestState.`charge orb spell turns an unpowered orb into an elemental orb`() =
        runGameTest(*spellTestScripts(ChargeOrbs::class)) {
            setup(magic = 56)
            player.give(objs.cosmic_rune to 3, objs.water_rune to 30)
            player.inv[5] = InvObj(utility_objs.unpowered_orb, 1)
            val obelisk = placeMapLoc(start.translateX(2), locTypes[utility_locs.obelisk_water])

            player.opLocT(obelisk, utility_components.charge_water_orb)
            advance(ticks = 5)
            assertEquals(1, player.inv.count(utility_objs.water_orb))
            assertEquals(0, player.inv.count(utility_objs.unpowered_orb))
        }

    @Test
    fun GameTestState.`charge lasts seven minutes and has a one minute recast delay`() =
        runGameTest(*spellTestScripts(ChargeSpell::class)) {
            setup(magic = 80)
            player.give(objs.blood_rune to 6, objs.air_rune to 6, objs.fire_rune to 6)
            player.ifButton(utility_components.charge)
            advance(ticks = 1)
            assertMessageSent(ChargeSpell.CHARGED_MESSAGE)
            assertTrue(player.isChargeActive())

            player.ifButton(utility_components.charge)
            advance(ticks = 1)
            assertMessageSent(ChargeSpell.COOLDOWN_MESSAGE)
            assertEquals(3, player.inv.count(objs.blood_rune))

            advanceUntil({ !player.isChargeActive() }, timeoutTicks = 701)
            advance(ticks = 1)
            assertFalse(player.isChargeActive())
        }

    private fun GameTestScope.setup(magic: Int) {
        player.teleport(start)
        player.setBaseLevel(stats.magic, magic)
        player.setCurrentLevel(stats.magic, magic)
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
    }

    private fun Player.give(vararg objs: Pair<ObjType, Int>) {
        objs.forEachIndexed { slot, (obj, count) -> inv[slot] = InvObj(obj, count) }
    }
}

private fun spellTestScripts(
    vararg scripts: KClass<out PluginScript>
): Array<KClass<out PluginScript>> = (MAGIC_RUNE_SCRIPTS + scripts).toTypedArray()
