package org.runerealm.content.skills.runecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.runerealm.api.config.refs.stats
import org.runerealm.api.inv.HeldOpScript
import org.runerealm.api.inv.HeldUOpScript
import org.runerealm.api.inv.LocUOpScript
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.skills.runecraft.configs.RunecraftAltars
import org.runerealm.content.skills.runecraft.configs.runecraft_locs
import org.runerealm.content.skills.runecraft.configs.runecraft_objs
import org.runerealm.content.skills.runecraft.scripts.EssencePouches
import org.runerealm.content.skills.runecraft.scripts.Runecraft
import org.runerealm.content.skills.runecraft.scripts.updateRuinVarBits
import org.runerealm.game.inv.InvObj
import org.runerealm.game.loc.BoundLocInfo
import org.runerealm.game.type.loc.LocType
import org.runerealm.game.type.obj.Wearpos
import org.runerealm.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RunecraftScriptTest {
    private val objs = runecraft_objs
    private val locs = runecraft_locs

    @Test
    fun GameTestState.`talisman on mysterious ruins enters the air altar`() =
        runGameTest(Runecraft::class, LocUOpScript::class) {
            val ruins = place(locs.air_ruins, CoordGrid(2984, 3291))
            player.placeAt(RunecraftAltars.air.ruins!!.inside)
            player.placeAt(CoordGrid(2985, 3290))
            give(InvObj(objs.air_talisman))
            player.opLocU(ruins, 0)
            advance(ticks = 1)
            assertMessageSent("You hold the air talisman towards the mysterious ruins.")
            advance(ticks = 3)
            assertEquals(RunecraftAltars.air.ruins!!.inside, player.coords)
            assertEquals(1, player.count(objs.air_talisman))
        }

    @Test
    fun GameTestState.`worn tiara shows enter and enters the ruins`() =
        runGameTest(Runecraft::class) {
            val ruins = place(locs.fire_ruins, CoordGrid(3312, 3254))
            player.placeAt(RunecraftAltars.fire.ruins!!.inside)
            player.placeAt(CoordGrid(3313, 3253))
            player.worn[Wearpos.Hat.slot] = InvObj(objs.fire_tiara)
            player.updateRuinVarBits()
            assertEquals(1, player.vars[RunecraftAltars.fire.ruins!!.varbit])
            assertEquals(0, player.vars[RunecraftAltars.air.ruins!!.varbit])
            player.opLoc1(ruins)
            advance(ticks = 4)
            assertEquals(RunecraftAltars.fire.ruins!!.inside, player.coords)
        }

    @Test
    fun GameTestState.`craft multiple air runes per essence and leave by the portal`() =
        runGameTest(Runecraft::class) {
            val altar = place(locs.air_altar, CoordGrid(2843, 4833))
            give(InvObj(objs.rune_essence), *Array(9) { InvObj(objs.pure_essence) })
            player.stats[stats.runecrafting] = 22
            val xp0 = xp()
            player.opLoc1(altar)
            advance(ticks = 1)
            assertEquals(30, player.count(objs.air_rune))
            assertEquals(0, player.count(objs.pure_essence))
            assertEquals(50, xp() - xp0)
            assertMessageSent("You bind the temple's power into air runes.")

            player.placeAt(RunecraftAltars.air.ruins!!.outside)
            val portal = place(locs.air_portal, CoordGrid(2841, 4828))
            player.opLoc1(portal)
            advance(ticks = 2)
            assertEquals(RunecraftAltars.air.ruins!!.outside, player.coords)
        }

    @Test
    fun GameTestState.`nature altar needs pure essence and the level`() =
        runGameTest(Runecraft::class) {
            val altar = place(locs.nature_altar, CoordGrid(2399, 4840))
            give(InvObj(objs.rune_essence))
            player.stats[stats.runecrafting] = 44
            player.opLoc1(altar)
            advance(ticks = 1)
            assertMessageSent("This altar can only bind pure essence.")

            give(InvObj(objs.pure_essence), InvObj(objs.pure_essence))
            player.stats[stats.runecrafting] = 43
            player.opLoc1(altar)
            advance(ticks = 1)
            assertMessageSent("You need a Runecraft level of 44 to craft nature runes.")

            player.stats[stats.runecrafting] = 91
            player.opLoc1(altar)
            advance(ticks = 1)
            assertEquals(4, player.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`lava runes with a binding necklace always succeed`() =
        runGameTest(Runecraft::class, LocUOpScript::class) {
            val altar = place(locs.fire_altar, CoordGrid(2584, 4837))
            give(
                InvObj(objs.earth_talisman),
                InvObj(objs.earth_rune, 10),
                *Array(5) { InvObj(objs.pure_essence) },
            )
            player.worn[Wearpos.Front.slot] = InvObj(objs.binding_necklace)
            player.stats[stats.runecrafting] = 23
            val xp0 = xp()
            player.opLocU(altar, 1)
            advance(ticks = 1)
            assertEquals(5, player.count(objs.lava_rune))
            assertEquals(5, player.count(objs.earth_rune))
            assertEquals(0, player.count(objs.earth_talisman))
            assertEquals(52, xp() - xp0)
        }

    @Test
    fun GameTestState.`bind a tiara at its altar`() =
        runGameTest(Runecraft::class, LocUOpScript::class) {
            val altar = place(locs.air_altar, CoordGrid(2843, 4833))
            give(InvObj(objs.tiara), InvObj(objs.air_talisman))
            val xp0 = xp()
            player.opLocU(altar, 0)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.air_tiara))
            assertEquals(0, player.count(objs.air_talisman))
            assertEquals(25, xp() - xp0)
        }

    @Test
    fun GameTestState.`soul altar binds dark essence fragments`() =
        runGameTest(Runecraft::class) {
            val altar = place(locs.soul_altar, CoordGrid(1813, 3853))
            give(InvObj(objs.dark_essence_fragments, 10))
            player.stats[stats.runecrafting] = 90
            val xp0 = xp()
            player.opLoc1(altar)
            advance(ticks = 1)
            assertEquals(10, player.count(objs.soul_rune))
            assertEquals(297, xp() - xp0)
        }

    @Test
    fun GameTestState.`essence pouches fill check and empty`() =
        runGameTest(EssencePouches::class, HeldOpScript::class, HeldUOpScript::class) {
            give(InvObj(objs.small_pouch), *Array(5) { InvObj(objs.pure_essence) })
            player.opHeld(0, op = 1)
            advance(ticks = 1)
            assertEquals(2, player.count(objs.pure_essence))
            player.opHeld(0, op = 3)
            advance(ticks = 1)
            assertMessageSent("There are 3 essences in this pouch.")
            player.opHeld(0, op = 2)
            advance(ticks = 1)
            assertEquals(5, player.count(objs.pure_essence))

            give(InvObj(objs.giant_pouch), InvObj(objs.pure_essence))
            player.stats[stats.runecrafting] = 74
            player.opHeld(0, op = 1)
            advance(ticks = 1)
            assertMessageSent("You need level 75 Runecraft to use this pouch.")
        }

    private fun GameTestScope.xp(): Int = player.statMap.getXP(stats.runecrafting)

    private fun GameTestScope.place(type: LocType, coords: CoordGrid): BoundLocInfo {
        val loc = placeMapLoc(coords, type)
        player.teleport(coords.translateZ(-1))
        return loc
    }

    private fun GameTestScope.give(vararg objs: InvObj) {
        player.clearInv()
        objs.forEachIndexed { slot, obj -> player.inv[slot] = obj }
    }
}
