package org.runerealm.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.testing.GameTestState
import org.runerealm.content.skills.agility.courses.AGILITY_COURSES
import org.runerealm.game.map.collision.isWalkBlocked
import org.runerealm.map.CoordGrid

class AgilityCourseDataTest {
    @Test
    fun GameTestState.`every course loc exists and has a first op`() {
        val locs = cacheTypes.locs.values.associateBy { it.internalName }
        for (course in AGILITY_COURSES) {
            for (obstacle in course.obstacles + course.extras) {
                for (name in obstacle.locs) {
                    val type = locs[name]
                    assertTrue(type != null, "${course.name} ${obstacle.name}: unknown loc `$name`")
                    assertTrue(type!!.op[0] != null, "${course.name} ${obstacle.name}: no op1")
                }
            }
        }
    }

    @Test
    fun GameTestState.`obstacle destinations, fall landings and mark tiles are walkable`() {
        for (course in AGILITY_COURSES) {
            for (obstacle in course.obstacles + course.extras) {
                val dest = obstacle.dest
                val x = dest.x
                val z = dest.z
                if (x != null && z != null) {
                    val coords = CoordGrid(x, z, dest.level)
                    assertFalse(collision.isWalkBlocked(coords), "${course.name} ${obstacle.name}")
                }
                val landing = obstacle.fail?.landing ?: continue
                assertFalse(
                    collision.isWalkBlocked(landing),
                    "${course.name} ${obstacle.name} fall",
                )
            }
            for (tile in course.markSpawns) {
                assertFalse(collision.isWalkBlocked(tile), "${course.name} mark $tile")
            }
        }
    }

    @Test
    fun GameTestState.`rooftop experience matches the wiki lap totals`() {
        val totals = AGILITY_COURSES.associate { it.name to it.obstacles.sumOf { o -> o.xp } }
        assertEquals(120.0, totals.getValue("Draynor Village Rooftop"), 0.01)
        assertEquals(216.0, totals.getValue("Al Kharid Rooftop"), 0.01)
        assertEquals(269.7, totals.getValue("Varrock Rooftop"), 0.01)
        assertEquals(240.0, totals.getValue("Canifis Rooftop"), 0.01)
        assertEquals(586.0, totals.getValue("Falador Rooftop"), 0.01)
        assertEquals(570.0, totals.getValue("Seers' Village Rooftop"), 0.01)
        assertEquals(890.0, totals.getValue("Pollnivneach Rooftop"), 0.01)
        assertEquals(780.0, totals.getValue("Rellekka Rooftop"), 0.01)
        assertEquals(889.0, totals.getValue("Ardougne Rooftop"), 0.01)
    }
}
