package org.runerealm.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.stats
import org.runerealm.api.registry.obj.ObjRegistry
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.skills.agility.courses.AgilityCourses
import org.runerealm.content.skills.agility.courses.courseProgress
import org.runerealm.content.skills.agility.courses.course_objs
import org.runerealm.content.skills.agility.courses.currentCourse
import org.runerealm.game.loc.BoundLocInfo
import org.runerealm.game.loc.LocAngle
import org.runerealm.game.loc.LocShape
import org.runerealm.map.CoordGrid

class AgilityCoursesTest {
    @Test
    fun GameTestState.`gnome log balance crosses and gives experience`() =
        runGameTest(AgilityCourses::class) {
            val log = place("gnome_log_balance1", CoordGrid(2474, 3435, 0), LocShape.GroundDecor)
            player.stats[stats.agility] = 1
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(2474, 3436, 0))
            player.opLoc1(log)
            advance(ticks = 12)
            assertEquals(CoordGrid(2474, 3429, 0), player.coords)
            assertEquals(10, player.statMap.getXP(stats.agility) - startXp)
            assertEquals(1, player.currentCourse)
            assertEquals(1, player.courseProgress)
        }

    @Test
    fun GameTestState.`rooftop obstacle requires the course level`() =
        runGameTest(AgilityCourses::class) {
            val wall = place("rooftops_kharid_wallclimb", CoordGrid(3273, 3195, 0))
            player.stats[stats.agility] = 19
            player.teleport(CoordGrid(3273, 3196, 0))
            player.opLoc1(wall)
            advance(ticks = 1)
            assertMessageSent("You need an Agility level of 20 to attempt this obstacle.")
            assertEquals(CoordGrid(3273, 3196, 0), player.coords)
        }

    @Test
    fun GameTestState.`canifis gap can be failed`() =
        runGameTest(AgilityCourses::class) {
            val gap = place("rooftops_canifis_jump_5", CoordGrid(3485, 3499, 3))
            player.stats[stats.agility] = 40
            player.stats[stats.hitpoints] = 10
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(3486, 3499, 3))
            random.next = 100 // Success roll fails.
            random.then = 2 // Damage.
            player.opLoc1(gap)
            advance(ticks = 6)
            assertEquals(CoordGrid(3482, 3498, 0), player.coords)
            assertEquals(0, player.statMap.getXP(stats.agility) - startXp)
            assertEquals(8, player.stats[stats.hitpoints])
        }

    @Test
    fun GameTestState.`canifis gap succeeds on a good roll`() =
        runGameTest(AgilityCourses::class) {
            val gap = place("rooftops_canifis_jump_5", CoordGrid(3485, 3499, 3))
            player.stats[stats.agility] = 40
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(3486, 3499, 3))
            random.next = 0
            player.opLoc1(gap)
            advance(ticks = 6)
            assertEquals(CoordGrid(3479, 3499, 3), player.coords)
            assertEquals(10, player.statMap.getXP(stats.agility) - startXp)
        }

    @Test
    fun GameTestState.`full gnome lap pays the bonus and can spawn a mark of grace`() =
        runInjectedGameTest(MarkDeps::class, null, AgilityCourses::class) { deps ->
            val pipe =
                place(
                    "obstical_pipe3_1",
                    CoordGrid(2484, 3431, 0),
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.observerUUID = 1
            player.stats[stats.agility] = 1
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(2484, 3430, 0))
            // Every obstacle but the pipe has been passed this lap.
            player.currentCourse = 1
            player.courseProgress = 0b0111111
            random.next = 0 // Mark of grace roll succeeds.
            random.then = 0 // First (only) spawn tile.
            player.opLoc1(pipe)
            advance(ticks = 12)
            assertEquals(CoordGrid(2484, 3437, 0), player.coords)
            // 7.5 for the pipe and the 50 experience lap bonus.
            assertEquals(57, player.statMap.getXP(stats.agility) - startXp)
            assertEquals(0, player.courseProgress)
            val marks = deps.objs.findAll(CoordGrid(2486, 3420, 2)).toList()
            assertTrue(marks.any { it.type == course_objs.mark_of_grace.id }) { "$marks" }
        }

    @Test
    fun GameTestState.`skipping an obstacle gives no lap bonus`() =
        runGameTest(AgilityCourses::class) {
            val pipe =
                place(
                    "obstical_pipe3_1",
                    CoordGrid(2484, 3431, 0),
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.stats[stats.agility] = 1
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(2484, 3430, 0))
            player.currentCourse = 1
            player.courseProgress = 0b0111011
            player.opLoc1(pipe)
            advance(ticks = 12)
            assertEquals(7, player.statMap.getXP(stats.agility) - startXp)
        }

    @Test
    fun GameTestState.`draynor crate finishes the rooftop lap`() =
        runGameTest(AgilityCourses::class) {
            val crate = place("rooftops_draynor_crate", CoordGrid(3102, 3261, 3))
            player.stats[stats.agility] = 1
            val startXp = player.statMap.getXP(stats.agility)
            player.teleport(CoordGrid(3101, 3261, 3))
            player.currentCourse = 2
            player.courseProgress = 0b0111111
            random.next = 149 // Mark of grace roll misses.
            player.opLoc1(crate)
            advance(ticks = 6)
            assertEquals(CoordGrid(3103, 3261, 0), player.coords)
            assertEquals(79, player.statMap.getXP(stats.agility) - startXp)
        }

    class MarkDeps @Inject constructor(val objs: ObjRegistry)

    private fun GameTestScope.place(
        internal: String,
        coords: CoordGrid,
        shape: LocShape = LocShape.CentrepieceStraight,
        angle: LocAngle = LocAngle.West,
    ): BoundLocInfo {
        allocZoneCollision(coords)
        val type = findLocTypes { it.internalName == internal }.single()
        return placeMapLoc(coords, type, shape, angle)
    }
}
