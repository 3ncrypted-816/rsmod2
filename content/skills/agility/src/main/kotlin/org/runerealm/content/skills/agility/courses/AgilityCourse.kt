package org.runerealm.content.skills.agility.courses

import org.runerealm.map.CoordGrid

/** How the player crosses an obstacle, which decides its animation and timing. */
enum class ObstacleMove {
    /** Walking across a log, rope, tightrope, plank or clothes line. */
    Balance,
    /** Climbing up or over a wall, net, branch or tree; the player appears at the destination. */
    Climb,
    /** Climbing down a crate, branch or ladder. */
    ClimbDown,
    /** Jumping across a gap between roofs. */
    Jump,
    /** Jumping down from the last roof to the ground. */
    JumpDown,
    /** Squeezing through an obstacle pipe. */
    Squeeze,
    /** Swinging across on a rope, cable or banner. */
    Swing,
    /** Sliding down a zip line by the teeth. */
    ZipLine,
    /** Shimmying across hand holds. */
    HandHolds,
    /** Side-stepping along a narrow wall or ledge. */
    Ledge,
    /** Crossing monkey bars. */
    MonkeyBars,
    /** Vaulting with a pole. */
    PoleVault,
    /** Hurdling a low wall or ledge. */
    Hurdle,
    /** Climbing over a crumbling wall. */
    CrumblingWall,
    /** Hopping across stepping stones. */
    SteppingStones,
}

/**
 * Where an obstacle takes the player. A `null` [x] or [z] keeps the player's own coordinate, which
 * lets wide obstacles (such as the Gnome Stronghold nets and pipes) be crossed at any tile.
 */
data class Dest(val x: Int?, val z: Int?, val level: Int) {
    fun resolve(from: CoordGrid): CoordGrid = CoordGrid(x ?: from.x, z ?: from.z, level)

    companion object {
        fun at(x: Int, z: Int, level: Int): Dest = Dest(x, z, level)
    }
}

/**
 * How failing an obstacle works. The success chance is interpolated linearly between [low] at level
 * 1 and [high] at level 99, out of 256, like other skilling success rolls.
 *
 * Damage is either a random value in [minDamage]..[maxDamage], or, when [hpFraction] is set, the
 * player's current hitpoints times [hpFraction], rounded down, plus [minDamage].
 */
data class ObstacleFail(
    val low: Int,
    val high: Int,
    val landing: CoordGrid,
    val minDamage: Int,
    val maxDamage: Int = minDamage,
    val hpFraction: Double = 0.0,
    val message: String = "You lose your footing and fall.",
)

/**
 * One obstacle of an [AgilityCourse].
 *
 * @property locs internal names of the locs that start this obstacle.
 * @property at when set, only the loc at these x/z coordinates belongs to this obstacle (for loc
 *   types that are reused several times along one course).
 * @property ticks how long the crossing takes; derived from the distance when `null`.
 */
data class Obstacle(
    val name: String,
    val locs: List<String>,
    val xp: Double,
    val move: ObstacleMove,
    val dest: Dest,
    val level: Int = 1,
    val at: Pair<Int, Int>? = null,
    val fail: ObstacleFail? = null,
    val ticks: Int? = null,
)

/**
 * An Agility course from the OSRS Wiki.
 *
 * @property level the course requirement; marks of grace are 80% rarer once the player's base level
 *   is 20 or more above it (except on Canifis).
 * @property lapBonus extra Agility experience for finishing a lap with every obstacle passed in
 *   order (the Gnome Stronghold, Barbarian Outpost and Wilderness courses). Rooftop courses fold
 *   their lap reward into the last obstacle's experience instead.
 * @property markChance chance out of [MARK_CHANCE_SCALE] to spawn a mark of grace when a lap is
 *   finished; `0` for courses without marks.
 */
data class AgilityCourse(
    val name: String,
    val level: Int,
    val obstacles: List<Obstacle>,
    val lapBonus: Double = 0.0,
    val strengthBonus: Double = 0.0,
    val markChance: Int = 0,
    val markLevelPenalty: Boolean = true,
    val markSpawns: List<CoordGrid> = emptyList(),
    /** Obstacles that are not part of the lap order, such as an entrance pipe. */
    val extras: List<Obstacle> = emptyList(),
) {
    companion object {
        const val MARK_CHANCE_SCALE: Int = 30
    }
}
