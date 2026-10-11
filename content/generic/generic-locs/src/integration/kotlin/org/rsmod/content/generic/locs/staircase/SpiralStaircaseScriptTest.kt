package org.rsmod.content.generic.locs.staircase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

class SpiralStaircaseScriptTest {
    private val base = CoordGrid(0, 50, 50, 20, 20)

    @Test
    fun GameTestState.`top-floor and bottom-floor skip the middle floor`() =
        runGameTest(SpiralStaircaseScript::class) {
            val (bottom, middle, top) = placeStack(floors = 3)

            player.teleport(base.translateX(-1))
            player.opLoc2(bottom)
            advance(ticks = 3)
            assertEquals(2, player.level)

            player.teleport(base.copy(level = 2).translateX(-1))
            player.opLoc2(top)
            advance(ticks = 6) // Top staircases force their approach side; walk around first.
            assertEquals(0, player.level)

            player.teleport(base.copy(level = 1).translateX(-1))
            player.opLoc4(middle)
            advance(ticks = 3)
            assertEquals(2, player.level)

            player.teleport(base.copy(level = 1).translateX(-1))
            player.opLoc5(middle)
            advance(ticks = 3)
            assertEquals(0, player.level)
        }

    @Test
    fun GameTestState.`two-floor staircase top-floor acts like climb-up`() =
        runGameTest(SpiralStaircaseScript::class) {
            val (bottom) = placeStack(floors = 2)
            player.teleport(base.translateX(-1))
            player.opLoc2(bottom)
            advance(ticks = 3)
            assertEquals(1, player.level)
        }

    private fun GameTestScope.placeStack(floors: Int): List<BoundLocInfo> {
        val locs = mutableListOf<BoundLocInfo>()
        for (level in 0 until floors) {
            allocZoneCollision(base.copy(level = level))
            val type =
                when (level) {
                    0 -> staircase_locs.spiral_bottom
                    floors - 1 -> staircase_locs.spiral_top
                    else -> staircase_locs.spiral_middle
                }
            locs += placeMapLoc(base.copy(level = level), locTypes.getValue(type.id))
        }
        return locs
    }
}
