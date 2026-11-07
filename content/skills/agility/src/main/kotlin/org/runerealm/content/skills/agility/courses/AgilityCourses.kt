package org.runerealm.content.skills.agility.courses

import jakarta.inject.Inject
import kotlin.math.abs
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.stat
import org.runerealm.api.player.stat.statBase
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.repo.obj.ObjRepository
import org.runerealm.api.script.onOpLoc1
import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.entity.Player
import org.runerealm.game.hit.HitType
import org.runerealm.game.loc.BoundLocInfo
import org.runerealm.game.type.loc.LocTypeList
import org.runerealm.game.type.seq.SeqType
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Agility courses listed in [AGILITY_COURSES]: obstacle requirements, experience, failing, lap
 * bonuses and marks of grace.
 *
 * Every obstacle passed sets its bit in [Player.courseProgress]. Passing the last obstacle of a
 * course with every bit set completes a lap, which pays the course's lap bonus (if any) and rolls
 * for a [mark of grace](https://oldschool.runescape.wiki/w/Mark_of_grace).
 */
class AgilityCourses
@Inject
constructor(private val locTypes: LocTypeList, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val byLoc = HashMap<String, MutableList<CourseObstacle>>()
        for ((courseIndex, course) in AGILITY_COURSES.withIndex()) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                val entry = CourseObstacle(courseIndex, course, index, obstacle)
                obstacle.locs.forEach { byLoc.getOrPut(it) { mutableListOf() } += entry }
            }
            for (extra in course.extras) {
                val entry = CourseObstacle(courseIndex, course, EXTRA_INDEX, extra)
                extra.locs.forEach { byLoc.getOrPut(it) { mutableListOf() } += entry }
            }
        }
        for (type in locTypes.values) {
            val candidates = byLoc[type.internalName] ?: continue
            onOpLoc1(type) { attempt(it.loc, candidates) }
        }
    }

    private suspend fun ProtectedAccess.attempt(
        loc: BoundLocInfo,
        candidates: List<CourseObstacle>,
    ) {
        val entry = candidates.select(loc.coords) ?: return
        val obstacle = entry.obstacle
        if (player.statBase(stats.agility) < obstacle.level) {
            mes("You need an Agility level of ${obstacle.level} to attempt this obstacle.")
            return
        }
        val start = player.coords
        val dest = obstacle.dest.resolve(start)
        if (dest == start) {
            return
        }
        val fail = obstacle.fail
        if (fail != null && !statRandom(stats.agility, fail.low, fail.high, invisibleBoost = 0)) {
            fall(obstacle, fail, start, dest)
            return
        }
        cross(obstacle, start, dest)
        complete(entry)
    }

    private fun List<CourseObstacle>.select(coords: CoordGrid): CourseObstacle? {
        val exact = firstOrNull { it.obstacle.at == coords.x to coords.z }
        return exact ?: firstOrNull { it.obstacle.at == null }
    }

    private suspend fun ProtectedAccess.cross(
        obstacle: Obstacle,
        start: CoordGrid,
        dest: CoordGrid,
    ) {
        val ticks = obstacle.ticks ?: defaultTicks(obstacle.move, start, dest)
        anim(obstacle.move.seq())
        if (obstacle.move.teleports) {
            delay(ticks)
            telejump(dest)
        } else {
            exactMove(
                start,
                dest,
                delay1 = 0,
                delay2 = ticks * CLIENT_CYCLES_PER_TICK,
                faceTowards(start, dest),
            )
            delay(ticks)
            telejump(dest)
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.fall(
        obstacle: Obstacle,
        fail: ObstacleFail,
        start: CoordGrid,
        dest: CoordGrid,
    ) {
        val ticks = (obstacle.ticks ?: defaultTicks(obstacle.move, start, dest)) / 2
        val halfway = CoordGrid((start.x + dest.x) / 2, (start.z + dest.z) / 2, start.level)
        anim(obstacle.move.seq())
        if (halfway != start) {
            val face = faceTowards(start, dest)
            exactMove(
                start,
                halfway,
                delay1 = 0,
                delay2 = ticks.coerceAtLeast(1) * CLIENT_CYCLES_PER_TICK,
                face,
            )
        }
        delay(ticks.coerceAtLeast(1))
        anim(course_seqs.fall)
        delay(1)
        telejump(fail.landing)
        resetAnim()
        val damage =
            if (fail.hpFraction > 0.0) {
                (player.stat(stats.hitpoints) * fail.hpFraction).toInt() + fail.minDamage
            } else {
                random.of(fail.minDamage, fail.maxDamage)
            }
        if (damage > 0) {
            takeInstantHit(HitType.Typeless, damage)
        }
        mes(fail.message)
    }

    private fun ProtectedAccess.complete(entry: CourseObstacle) {
        val obstacle = entry.obstacle
        if (obstacle.xp > 0.0) {
            statAdvance(stats.agility, obstacle.xp)
        }
        if (entry.index == EXTRA_INDEX) {
            return
        }
        val course = entry.course
        val sameCourse = player.currentCourse == entry.courseIndex + 1
        var progress = if (sameCourse && entry.index != 0) player.courseProgress else 0
        progress = progress or (1 shl entry.index)
        player.currentCourse = entry.courseIndex + 1
        if (entry.index != course.obstacles.lastIndex) {
            player.courseProgress = progress
            return
        }
        player.courseProgress = 0
        val allPassed = (1 shl course.obstacles.size) - 1
        if (progress != allPassed) {
            return
        }
        if (course.lapBonus > 0.0) {
            statAdvance(stats.agility, course.lapBonus)
        }
        if (course.strengthBonus > 0.0) {
            statAdvance(stats.strength, course.strengthBonus)
        }
        rollMarkOfGrace(course)
    }

    private fun ProtectedAccess.rollMarkOfGrace(course: AgilityCourse) {
        if (course.markChance <= 0 || course.markSpawns.isEmpty()) {
            return
        }
        val minute = mapClock / TICKS_PER_MINUTE
        val nextAllowed = player.markCooldown - 1
        // A stored minute far in the future comes from an earlier server run; ignore it.
        val coolingDown = nextAllowed in (minute + 1)..(minute + MARK_COOLDOWN_MINUTES)
        if (coolingDown) {
            return
        }
        val overLevelled =
            course.markLevelPenalty &&
                player.statBase(stats.agility) >= course.level + MARK_PENALTY_LEVELS
        // A 20-levels-over penalty makes marks 80% rarer: scale the roll by five.
        val chance = if (overLevelled) course.markChance else course.markChance * PENALTY_FACTOR
        if (random.of(AgilityCourse.MARK_CHANCE_SCALE * PENALTY_FACTOR) >= chance) {
            return
        }
        val tile = course.markSpawns[random.of(course.markSpawns.size)]
        objRepo.add(course_objs.mark_of_grace, tile, MARK_DURATION, receiver = player)
        player.markCooldown = minute + MARK_COOLDOWN_MINUTES + 1
    }

    private fun defaultTicks(move: ObstacleMove, start: CoordGrid, dest: CoordGrid): Int {
        val distance = maxOf(abs(dest.x - start.x), abs(dest.z - start.z))
        return when (move) {
            ObstacleMove.Climb,
            ObstacleMove.ClimbDown -> 2
            ObstacleMove.Jump,
            ObstacleMove.JumpDown,
            ObstacleMove.Hurdle,
            ObstacleMove.CrumblingWall,
            ObstacleMove.PoleVault -> if (distance <= 3) 2 else 3
            ObstacleMove.Swing,
            ObstacleMove.ZipLine -> (distance / 3).coerceIn(2, MAX_TICKS)
            else -> distance.coerceIn(1, MAX_TICKS)
        }
    }

    private fun faceTowards(from: CoordGrid, to: CoordGrid): Int {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return when {
            abs(dx) >= abs(dz) && dx > 0 -> FACE_EAST
            abs(dx) >= abs(dz) -> FACE_WEST
            dz > 0 -> FACE_NORTH
            else -> FACE_SOUTH
        }
    }

    private data class CourseObstacle(
        val courseIndex: Int,
        val course: AgilityCourse,
        val index: Int,
        val obstacle: Obstacle,
    )

    private companion object {
        const val EXTRA_INDEX = -1
        const val CLIENT_CYCLES_PER_TICK = 30
        const val MAX_TICKS = 12
        const val TICKS_PER_MINUTE = 100
        const val MARK_COOLDOWN_MINUTES = 3
        const val MARK_PENALTY_LEVELS = 20
        const val PENALTY_FACTOR = 5
        /** Marks of grace stay for 10 minutes. */
        const val MARK_DURATION = 1000
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536
    }
}

private val ObstacleMove.teleports: Boolean
    get() = this == ObstacleMove.Climb || this == ObstacleMove.ClimbDown

private fun ObstacleMove.seq(): SeqType =
    when (this) {
        ObstacleMove.Balance -> course_seqs.balance
        ObstacleMove.Climb -> course_seqs.climb
        ObstacleMove.ClimbDown -> course_seqs.climb_down
        ObstacleMove.Jump -> course_seqs.jump
        ObstacleMove.JumpDown -> course_seqs.jump_down
        ObstacleMove.Squeeze -> course_seqs.pipe
        ObstacleMove.Swing -> course_seqs.rope_swing
        ObstacleMove.ZipLine -> course_seqs.zip_line
        ObstacleMove.HandHolds -> course_seqs.hand_holds
        ObstacleMove.Ledge -> course_seqs.ledge
        ObstacleMove.MonkeyBars -> course_seqs.monkey_bars
        ObstacleMove.PoleVault -> course_seqs.pole_vault
        ObstacleMove.Hurdle -> course_seqs.hurdle
        ObstacleMove.CrumblingWall -> course_seqs.crumbling_wall
        ObstacleMove.SteppingStones -> course_seqs.stepping_stone
    }

/** Course progress bits for the current lap. */
var Player.courseProgress by intVarp(course_varps.progress)

/** 1 + the index in [AGILITY_COURSES] of the course the player is running, or 0. */
var Player.currentCourse by intVarp(course_varps.current)

/** 1 + the game minute from which a mark of grace may spawn again, or 0. */
var Player.markCooldown by intVarp(course_varps.mark_cooldown)

internal object course_seqs : SeqReferences() {
    val balance = find("human_walk_logbalance")
    val climb = find("human_reachforladder")
    val climb_down = find("agility_shortcut_wall_jumpdown2")
    val jump = find("agility_shortcut_wall_jumpdown")
    val jump_down = find("agility_shortcut_wall_jumpdown2")
    val pipe = find("human_doublepipesqueeze")
    val rope_swing = find("human_ropeswing_long")
    val zip_line = find("zipline_slide")
    val hand_holds = find("agilityarena_handholds_middle")
    val ledge = find("human_walk_sidestepl")
    val monkey_bars = find("human_monkeybars_walk")
    val pole_vault = find("rooftops_pole_vault")
    val hurdle = find("human_jump_hurdle")
    val crumbling_wall = find("human_walk_crumbledwall")
    val stepping_stone = find("human_steppingstonejump")
    val fall = find("human_wobbleandfall_l")
}

object course_objs : ObjReferences() {
    val mark_of_grace = find("grace")
}

internal object course_varps : VarpReferences() {
    val progress = find("agility_course_progress")
    val current = find("agility_course_current")
    val mark_cooldown = find("agility_mark_cooldown")
}

internal object CourseVarpBuilds : VarpBuilder() {
    init {
        build("agility_course_progress") { temporary = true }
        build("agility_course_current") { temporary = true }
        build("agility_mark_cooldown") { permanent = true }
    }
}
