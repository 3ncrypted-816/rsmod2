package org.runerealm.content.generic.locs.staircase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.game.loc.BoundLocInfo
import org.runerealm.game.loc.LocAngle
import org.runerealm.map.CoordGrid

class SpiralStaircaseScriptTest {
    private val base = CoordGrid(0, 50, 50, 20, 20)

    @Test
    fun GameTestState.`staircases keep their regular climb options`() = runGameTest {
        fun ops(type: org.runerealm.game.type.loc.LocType) = locTypes[type].op.toList()
        val climbUp = listOf("Climb-up", "Top-floor", null, null, null)
        val climbDown = listOf("Climb-down", "Bottom-floor", null, null, null)
        val middle = listOf("Climb", "Climb-up", "Climb-down", "Top-floor", "Bottom-floor")
        assertEquals(climbUp, ops(staircase_locs.lumbridge_spiral_bottom))
        assertEquals(climbUp, ops(staircase_locs.spiral_bottom))
        assertEquals(middle, ops(staircase_locs.spiral_middle))
        assertEquals(middle, ops(staircase_locs.wooden_spiral_middle))
        assertEquals(climbDown, ops(staircase_locs.lumbridge_spiral_top))
        assertEquals(climbDown, ops(staircase_locs.spiral_top))
    }

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

    @Test
    fun GameTestState.`bottom-floor from lumbridge castle top floor reaches the ground floor`() =
        runGameTest(SpiralStaircaseScript::class) {
            // Lumbridge Castle's top staircase origin is one tile north-east of the others.
            for (level in 0..2) {
                allocZoneCollision(base.copy(level = level))
            }
            placeMapLoc(
                base,
                locTypes.getValue(staircase_locs.lumbridge_spiral_bottom.id),
                angle = LocAngle.South,
            )
            placeMapLoc(
                base.copy(level = 1),
                locTypes.getValue(staircase_locs.spiral_middle.id),
                angle = LocAngle.South,
            )
            val top =
                placeMapLoc(
                    base.copy(level = 2).translate(1, 1),
                    locTypes.getValue(staircase_locs.lumbridge_spiral_top.id),
                    angle = LocAngle.South,
                )
            player.teleport(base.copy(level = 2).translate(3, 1))
            player.opLoc2(top)
            advanceUntil({ player.level != 2 }, timeoutTicks = 20)
            assertEquals(0, player.level)
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
