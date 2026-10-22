package org.runerealm.content.skills.herblore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.config.refs.stats
import org.runerealm.api.inv.HeldOpScript
import org.runerealm.api.inv.HeldUOpScript
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.skills.herblore.configs.herblore_npcs
import org.runerealm.content.skills.herblore.configs.herblore_objs
import org.runerealm.content.skills.herblore.scripts.BobBarter
import org.runerealm.content.skills.herblore.scripts.Herblore
import org.runerealm.game.inv.InvObj
import org.runerealm.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HerbloreScriptTest {
    private val objs = herblore_objs

    @Test
    fun GameTestState.`clean a grimy herb`() =
        runGameTest(Herblore::class, HeldOpScript::class) {
            give(InvObj(objs.grimy_ranarr), InvObj(objs.grimy_torstol))
            player.stats[stats.herblore] = 25
            val xp0 = xp()
            player.opHeld(0)
            advance(ticks = 1)
            assertEquals(objs.ranarr.id, player.inv[0]?.id)
            assertEquals(7, xp() - xp0)

            player.opHeld(1)
            advance(ticks = 1)
            assertMessageSent("You need level 75 Herblore to clean the grimy torstol.")
            assertEquals(objs.grimy_torstol.id, player.inv[1]?.id)
        }

    @Test
    fun GameTestState.`herb into vial of water makes an unfinished potion`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(InvObj(objs.guam), InvObj(objs.vial_water))
            player.stats[stats.herblore] = 3
            player.opHeldU(0, 1)
            advance(ticks = 1)
            assertFalse(player.ui.containsModal(interfaces.skillmulti))
            assertEquals(1, player.count(objs.guam_unf))
            assertEquals(0, player.count(objs.vial_water))
            assertMessageSent("You put the guam leaf into the vial of water.")
        }

    @Test
    fun GameTestState.`mix attack potions with make-x`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(
                InvObj(objs.guam_unf),
                InvObj(objs.guam_unf),
                InvObj(objs.eye_of_newt),
                InvObj(objs.eye_of_newt),
            )
            player.stats[stats.herblore] = 3
            val xp0 = xp()
            player.opHeldU(2, 0)
            advance(ticks = 1)
            assertTrue(player.ui.containsModal(interfaces.skillmulti))
            player.resumePauseButton(components.skillmulti_a, sub = 2)
            advance(ticks = 2)
            assertEquals(1, player.count(objs.attack3))
            assertEquals(25, xp() - xp0)
            advance(ticks = 3)
            assertEquals(2, player.count(objs.attack3))
            assertEquals(0, player.count(objs.eye_of_newt))
            assertEquals(50, xp() - xp0)
        }

    @Test
    fun GameTestState.`potion level is required`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(InvObj(objs.ranarr_unf), InvObj(objs.snape_grass))
            player.stats[stats.herblore] = 37
            player.opHeldU(1, 0)
            advance(ticks = 1)
            assertMessageSent("You need a Herblore level of 38 to make a prayer potion(3).")
            assertEquals(1, player.count(objs.ranarr_unf))
        }

    @Test
    fun GameTestState.`pestle and mortar crushes every carried horn`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(
                InvObj(objs.pestle_and_mortar),
                InvObj(objs.unicorn_horn),
                InvObj(objs.unicorn_horn),
                InvObj(objs.unicorn_horn),
            )
            player.stats[stats.herblore] = 1
            player.opHeldU(0, 1)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.unicorn_horn_dust))
            advance(ticks = 8)
            assertEquals(3, player.count(objs.unicorn_horn_dust))
            assertEquals(0, player.count(objs.unicorn_horn))
            assertEquals(1, player.count(objs.pestle_and_mortar))
        }

    @Test
    fun GameTestState.`herb tar needs fifteen swamp tar and a pestle`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(InvObj(objs.guam), InvObj(objs.swamp_tar, 20))
            player.stats[stats.herblore] = 19
            player.opHeldU(0, 1)
            advance(ticks = 1)
            assertMessageSent("You need a pestle and mortar to do that.")

            give(InvObj(objs.guam), InvObj(objs.swamp_tar, 20), InvObj(objs.pestle_and_mortar))
            val xp0 = xp()
            player.opHeldU(0, 1)
            advance(ticks = 1)
            assertEquals(15, player.count(objs.guam_tar))
            assertEquals(5, player.count(objs.swamp_tar))
            assertEquals(30, xp() - xp0)
        }

    @Test
    fun GameTestState.`amylase upgrades super energy into stamina by dose`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(InvObj(objs.super_energy4), InvObj(objs.amylase_crystal, 10))
            player.stats[stats.herblore] = 77
            val xp0 = xp()
            player.opHeldU(1, 0)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.stamina4))
            assertEquals(6, player.count(objs.amylase_crystal))
            assertEquals(102, xp() - xp0)
        }

    @Test
    fun GameTestState.`super combat potion from torstol and three super potions`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(
                InvObj(objs.torstol),
                InvObj(objs.super_attack4),
                InvObj(objs.super_strength4),
                InvObj(objs.super_defence4),
            )
            player.stats[stats.herblore] = 90
            val xp0 = xp()
            player.opHeldU(0, 2)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.super_combat4))
            assertEquals(2, player.count(objs.vial))
            assertEquals(0, player.count(objs.super_attack4))
            assertEquals(150, xp() - xp0)
        }

    @Test
    fun GameTestState.`combine potion doses`() =
        runGameTest(Herblore::class, HeldUOpScript::class) {
            give(InvObj(objs.attack3), InvObj(objs.attack2))
            player.opHeldU(1, 0)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.attack4))
            assertEquals(1, player.count(objs.attack1))
            assertMessageSent("You have combined the liquid into 4 doses.")

            give(InvObj(objs.prayer1), InvObj(objs.prayer2))
            player.opHeldU(0, 1)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.prayer3))
            assertEquals(1, player.count(objs.vial))
        }

    @Test
    fun GameTestState.`bob barter decants potions into four doses`() =
        runGameTest(BobBarter::class) {
            give(
                InvObj(objs.prayer3),
                InvObj(objs.prayer3),
                InvObj(objs.prayer2),
                InvObj(objs.attack1),
            )
            val coords = CoordGrid(3155, 3480)
            player.teleport(coords.translateX(1))
            val bob = spawnNpc(coords, npcTypes[herblore_npcs.bob_barter])
            player.opNpc4(bob)
            advanceUntil({ player.ui.containsModal(interfaces.chatmenu) }, timeoutTicks = 5)
            player.resumePauseButton(components.chatmenu_pbutton, sub = 4)
            advance(ticks = 1)
            assertEquals(2, player.count(objs.prayer4))
            assertEquals(0, player.count(objs.prayer3))
            assertEquals(1, player.count(objs.vial))
            assertEquals(1, player.count(objs.attack1))
        }

    private fun GameTestScope.xp(): Int = player.statMap.getXP(stats.herblore)

    private fun GameTestScope.give(vararg objs: InvObj) {
        player.clearInv()
        objs.forEachIndexed { slot, obj -> player.inv[slot] = obj }
    }
}
