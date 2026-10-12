package org.runerealm.content.skills.magic.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.components
import org.runerealm.api.inv.HeldOpScript
import org.runerealm.api.testing.GameTestState
import org.runerealm.game.inv.InvObj
import org.runerealm.map.CoordGrid

class ItemTeleportsTest {
    private val start = CoordGrid(3222, 3218, 0)

    @Test
    fun GameTestState.`rubbing a glory teleports and uses a charge`() =
        runGameTest(JewelleryTeleports::class, HeldOpScript::class) {
            player.teleport(start)
            allocZoneCollision(CoordGrid(3087, 3496, 0))
            val glory4 = objType("amulet_of_glory_4")
            val glory3 = objType("amulet_of_glory_3")
            player.inv[0] = InvObj(glory4)
            player.opHeld(0, glory4.iop.indexOfFirst { it == "Rub" } + 1)
            advance()
            player.resumePauseButton(components.chatmenu_pbutton, sub = 1) // Edgeville.
            advanceUntil({ player.coords != start }, timeoutTicks = 10)
            assertEquals(CoordGrid(3087, 3496, 0), player.coords)
            assertEquals(glory3.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState.`breaking a tablet teleports and uses it up`() =
        runGameTest(TeleportTablets::class, HeldOpScript::class) {
            player.teleport(start)
            allocZoneCollision(CoordGrid(3213, 3424, 0))
            val tablet = objType("poh_tablet_varrockteleport")
            player.inv[0] = InvObj(tablet)
            player.opHeld(0, tablet.iop.indexOfFirst { it == "Break" } + 1)
            advanceUntil({ player.coords != start }, timeoutTicks = 10)
            assertEquals(CoordGrid(3213, 3424, 0), player.coords)
            assertEquals(null, player.inv[0])
        }

    @Test
    fun GameTestState.`tablets do not work above level 20 wilderness`() =
        runGameTest(TeleportTablets::class, HeldOpScript::class) {
            val deep = CoordGrid(3100, 3700, 0)
            player.teleport(deep)
            val tablet = objType("poh_tablet_varrockteleport")
            player.inv[0] = InvObj(tablet)
            player.opHeld(0, tablet.iop.indexOfFirst { it == "Break" } + 1)
            advance(ticks = 5)
            assertEquals(deep, player.coords)
            assertEquals(tablet.id, player.inv[0]?.id)
        }

    private fun GameTestState.objType(name: String) =
        cacheTypes.objs.values.first { it.internalName == name }
}
