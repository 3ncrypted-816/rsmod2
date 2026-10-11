package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class AgilityShortcutsTest {
    @Test
    fun GameTestState.`varrock fence jumps to the other side`() =
        runGameTest(AgilityShortcuts::class) {
            val fence =
                place(
                    "lumbridge_sc_fencejump",
                    CoordGrid(3240, 3335, 0),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.stats[stats.agility] = 13
            player.teleport(CoordGrid(3240, 3334, 0))
            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(CoordGrid(3240, 3335, 0), player.coords)

            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(CoordGrid(3240, 3334, 0), player.coords)
        }

    @Test
    fun GameTestState.`shortcut requires the agility level`() =
        runGameTest(AgilityShortcuts::class) {
            val fence =
                place(
                    "lumbridge_sc_fencejump",
                    CoordGrid(3240, 3335, 0),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.stats[stats.agility] = 12
            player.teleport(CoordGrid(3240, 3334, 0))
            player.opLoc1(fence)
            advance(ticks = 1)
            assertMessageSent("You need an Agility level of 13 to negotiate this obstacle.")
            assertEquals(CoordGrid(3240, 3334, 0), player.coords)
        }

    @Test
    fun GameTestState.`falador underwall tunnel crosses the wall`() =
        runGameTest(AgilityShortcuts::class) {
            val south =
                place(
                    "falador_sc_castlewall_south",
                    CoordGrid(2948, 3310, 0),
                    LocShape.CentrepieceStraight,
                    LocAngle.North,
                )
            player.stats[stats.agility] = 26
            player.teleport(CoordGrid(2948, 3309, 0))
            player.opLoc1(south)
            advance(ticks = 6)
            assertEquals(CoordGrid(2948, 3313, 0), player.coords)
        }

    private fun GameTestScope.place(
        internal: String,
        coords: CoordGrid,
        shape: LocShape,
        angle: LocAngle,
    ): BoundLocInfo {
        allocZoneCollision(coords)
        val type = findLocTypes { it.internalName == internal }.single()
        return placeMapLoc(coords, type, shape, angle)
    }
}
