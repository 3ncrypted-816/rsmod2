@file:Suppress("SpellCheckingInspection", "MagicNumber")

package org.runerealm.content.skills.agility.courses

import org.runerealm.content.skills.agility.courses.ObstacleMove.Balance
import org.runerealm.content.skills.agility.courses.ObstacleMove.Climb
import org.runerealm.content.skills.agility.courses.ObstacleMove.ClimbDown
import org.runerealm.content.skills.agility.courses.ObstacleMove.CrumblingWall
import org.runerealm.content.skills.agility.courses.ObstacleMove.HandHolds
import org.runerealm.content.skills.agility.courses.ObstacleMove.Hurdle
import org.runerealm.content.skills.agility.courses.ObstacleMove.Jump
import org.runerealm.content.skills.agility.courses.ObstacleMove.JumpDown
import org.runerealm.content.skills.agility.courses.ObstacleMove.Ledge
import org.runerealm.content.skills.agility.courses.ObstacleMove.MonkeyBars
import org.runerealm.content.skills.agility.courses.ObstacleMove.PoleVault
import org.runerealm.content.skills.agility.courses.ObstacleMove.Squeeze
import org.runerealm.content.skills.agility.courses.ObstacleMove.SteppingStones
import org.runerealm.content.skills.agility.courses.ObstacleMove.Swing
import org.runerealm.content.skills.agility.courses.ObstacleMove.ZipLine
import org.runerealm.map.CoordGrid

/*
 * Agility courses, per the OSRS Wiki course pages (e.g.
 * https://oldschool.runescape.wiki/w/Gnome_Stronghold_Agility_Course and
 * https://oldschool.runescape.wiki/w/Rooftop_Agility). Obstacle experience, level requirements,
 * fail points and success rates are from the wiki; loc names and positions are read from the game
 * map, and landing tiles are checked against map collision in `AgilityCourseDataTest`.
 *
 * Mark of grace spawn chances follow https://oldschool.runescape.wiki/w/Mark_of_grace: 1/3 per
 * finished lap on most courses, 2/3 on Canifis and Ardougne, 2/5 on Rellekka.
 */

private const val ONE_THIRD = 10
private const val TWO_THIRDS = 20
private const val TWO_FIFTHS = 12

private fun c(x: Int, z: Int, level: Int = 0): CoordGrid = CoordGrid(x, z, level)

private fun to(x: Int?, z: Int?, level: Int): Dest = Dest(x, z, level)

/**
 * A linear success chance that is [start] (0..1) at [level] and reaches 100% at [safe], expressed
 * as the level-1 / level-99 rates out of 256 that skilling success rolls use.
 */
private fun failUntil(
    level: Int,
    start: Double,
    safe: Int,
    landing: CoordGrid,
    minDamage: Int,
    maxDamage: Int = minDamage,
    hpFraction: Double = 0.0,
): ObstacleFail {
    val perLevel = (1.0 - start) / (safe - level)
    val low = ((start - perLevel * (level - 1)) * 256).toInt()
    val high = ((start + perLevel * (99 - level)) * 256).toInt()
    return ObstacleFail(low, high, landing, minDamage, maxDamage, hpFraction)
}

val GNOME_STRONGHOLD: AgilityCourse =
    AgilityCourse(
        name = "Gnome Stronghold",
        level = 1,
        lapBonus = 50.0,
        markChance = ONE_THIRD,
        markSpawns = listOf(c(2486, 3420, 2)),
        obstacles =
            listOf(
                Obstacle(
                    "Log balance",
                    listOf("gnome_log_balance1"),
                    10.0,
                    Balance,
                    to(2474, 3429, 0),
                ),
                Obstacle("Obstacle net", listOf("obstical_net2"), 10.0, Climb, to(null, 3424, 1)),
                Obstacle("Tree branch", listOf("climbing_branch"), 6.5, Climb, to(2473, 3420, 2)),
                Obstacle(
                    "Balancing rope",
                    listOf("balancing_rope", "balancing_rope_mid"),
                    10.0,
                    Balance,
                    to(2483, 3420, 2),
                ),
                Obstacle(
                    "Tree branch (down)",
                    listOf("climbing_tree", "climbing_tree2"),
                    6.5,
                    ClimbDown,
                    to(2487, 3420, 0),
                ),
                Obstacle(
                    "Obstacle net (second)",
                    listOf("obstical_net3"),
                    10.0,
                    Climb,
                    to(null, 3427, 0),
                ),
                Obstacle(
                    "Obstacle pipe",
                    listOf("obstical_pipe3_1", "obstical_pipe3_2"),
                    7.5,
                    Squeeze,
                    to(null, 3437, 0),
                ),
            ),
    )

val DRAYNOR_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Draynor Village Rooftop",
        level = 1,
        markChance = ONE_THIRD,
        markSpawns = listOf(c(3099, 3259, 3), c(3099, 3279, 3), c(3092, 3266, 3), c(3088, 3274, 3)),
        obstacles =
            listOf(
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_draynor_wallclimb"),
                    5.0,
                    Climb,
                    to(3102, 3279, 3),
                ),
                Obstacle(
                    "Tightrope",
                    listOf("rooftops_draynor_tightrope_1"),
                    8.0,
                    Balance,
                    to(3090, 3276, 3),
                ),
                Obstacle(
                    "Tightrope (second)",
                    listOf("rooftops_draynor_tightrope_2"),
                    7.0,
                    Balance,
                    to(3092, 3266, 3),
                ),
                Obstacle(
                    "Narrow wall",
                    listOf("rooftops_draynor_wallcrossing"),
                    7.0,
                    Ledge,
                    to(3088, 3261, 3),
                ),
                Obstacle(
                    "Wall",
                    listOf("rooftops_draynor_wallscramble"),
                    10.0,
                    Climb,
                    to(3088, 3255, 3),
                ),
                Obstacle("Gap", listOf("rooftops_draynor_leapdown"), 4.0, Jump, to(3096, 3256, 3)),
                Obstacle(
                    "Crate",
                    listOf("rooftops_draynor_crate"),
                    79.0,
                    ClimbDown,
                    to(3103, 3261, 0),
                ),
            ),
    )

val AL_KHARID_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Al Kharid Rooftop",
        level = 20,
        markChance = ONE_THIRD,
        markSpawns =
            listOf(
                c(3315, 3161, 1),
                c(3266, 3166, 3),
                c(3284, 3172, 3),
                c(3276, 3185, 3),
                c(3302, 3187, 3),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_kharid_wallclimb"),
                    12.0,
                    Climb,
                    to(3273, 3192, 3),
                    level = 20,
                ),
                Obstacle(
                    "Tightrope",
                    listOf("rooftops_kharid_tightrope_1"),
                    36.0,
                    Balance,
                    to(3272, 3172, 3),
                    level = 20,
                    fail = failUntil(20, 0.85, 40, c(3272, 3177), minDamage = 1, maxDamage = 5),
                ),
                Obstacle(
                    "Cable",
                    listOf("rooftops_kharid_rope_swing"),
                    48.0,
                    Swing,
                    to(3284, 3166, 3),
                    level = 20,
                ),
                Obstacle(
                    "Zip line",
                    listOf("rooftops_kharid_slide_side"),
                    48.0,
                    ZipLine,
                    to(3315, 3163, 1),
                    level = 20,
                    fail = failUntil(20, 0.85, 40, c(3308, 3163), minDamage = 1, maxDamage = 5),
                ),
                Obstacle(
                    "Tropical tree",
                    listOf("rooftops_kharid_bamboo_tree_top"),
                    12.0,
                    Swing,
                    to(3317, 3174, 2),
                    level = 20,
                ),
                Obstacle(
                    "Roof top beams",
                    listOf("rooftops_kharid_wallclimb_2"),
                    6.0,
                    Climb,
                    to(3316, 3180, 3),
                    level = 20,
                ),
                Obstacle(
                    "Tightrope (second)",
                    listOf("rooftops_kharid_tightrope_4"),
                    18.0,
                    Balance,
                    to(3302, 3186, 3),
                    level = 20,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_kharid_leapdown"),
                    36.0,
                    JumpDown,
                    to(3299, 3194, 0),
                    level = 20,
                ),
            ),
    )

val VARROCK_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Varrock Rooftop",
        level = 30,
        markChance = ONE_THIRD,
        markSpawns =
            listOf(
                c(3185, 3394, 3),
                c(3191, 3394, 3),
                c(3196, 3416, 1),
                c(3238, 3406, 3),
                c(3220, 3402, 3),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_varrock_wallclimb"),
                    13.5,
                    Climb,
                    to(3219, 3414, 3),
                    level = 30,
                ),
                Obstacle(
                    "Clothes line",
                    listOf("rooftops_varrock_clothesline"),
                    23.0,
                    Balance,
                    to(3208, 3414, 3),
                    level = 30,
                    fail = failUntil(30, 0.85, 50, c(3210, 3414), minDamage = 3, maxDamage = 8),
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_varrock_leaptoruins"),
                    19.0,
                    Jump,
                    to(3197, 3416, 1),
                    level = 30,
                ),
                Obstacle(
                    "Wall",
                    listOf("rooftops_varrock_wallswing"),
                    28.0,
                    Ledge,
                    to(3192, 3406, 3),
                    level = 30,
                    fail = failUntil(30, 0.85, 50, c(3190, 3410), minDamage = 2, maxDamage = 5),
                ),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_varrock_wallscramble"),
                    10.0,
                    Jump,
                    to(3193, 3398, 3),
                    level = 30,
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_varrock_leaptobalcony"),
                    24.5,
                    Jump,
                    to(3218, 3399, 3),
                    level = 30,
                    ticks = 4,
                ),
                Obstacle(
                    "Gap (fourth)",
                    listOf("rooftops_varrock_leapdown"),
                    4.5,
                    Jump,
                    to(3236, 3403, 3),
                    level = 30,
                ),
                Obstacle(
                    "Ledge",
                    listOf("rooftops_varrock_stepuproof"),
                    3.5,
                    Hurdle,
                    to(3236, 3410, 3),
                    level = 30,
                ),
                Obstacle(
                    "Edge",
                    listOf("rooftops_varrock_finish"),
                    143.7,
                    JumpDown,
                    to(3236, 3417, 0),
                    level = 30,
                ),
            ),
    )

val CANIFIS_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Canifis Rooftop",
        level = 40,
        markChance = TWO_THIRDS,
        markLevelPenalty = false,
        markSpawns =
            listOf(
                c(3506, 3495, 2),
                c(3501, 3505, 2),
                c(3490, 3501, 2),
                c(3478, 3496, 3),
                c(3478, 3484, 2),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Tall tree",
                    listOf("rooftops_canifis_start_tree"),
                    10.0,
                    Climb,
                    to(3506, 3492, 2),
                    level = 40,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_canifis_jump"),
                    8.0,
                    Jump,
                    to(3502, 3504, 2),
                    level = 40,
                ),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_canifis_jump_2"),
                    8.0,
                    Jump,
                    to(3492, 3504, 2),
                    level = 40,
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_canifis_jump_5"),
                    10.0,
                    Jump,
                    to(3479, 3499, 3),
                    level = 40,
                    // https://oldschool.runescape.wiki/w/Gap_(Canifis_Rooftop_Course): 175/300.
                    fail = ObstacleFail(175, 300, c(3482, 3498), minDamage = 1, maxDamage = 3),
                ),
                Obstacle(
                    "Gap (fourth)",
                    listOf("rooftops_canifis_jump_3"),
                    8.0,
                    Jump,
                    to(3478, 3486, 2),
                    level = 40,
                ),
                Obstacle(
                    "Pole-vault",
                    listOf("rooftops_canifis_polevault"),
                    10.0,
                    PoleVault,
                    to(3489, 3476, 3),
                    level = 40,
                    ticks = 4,
                ),
                Obstacle(
                    "Gap (fifth)",
                    listOf("rooftops_canifis_jump_4"),
                    11.0,
                    Jump,
                    to(3510, 3476, 2),
                    level = 40,
                ),
                Obstacle(
                    "Gap (last)",
                    listOf("rooftops_canifis_leapdown"),
                    175.0,
                    JumpDown,
                    to(3510, 3485, 0),
                    level = 40,
                ),
            ),
    )

val FALADOR_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Falador Rooftop",
        level = 50,
        markChance = ONE_THIRD,
        markSpawns =
            listOf(
                c(3045, 3345, 3),
                c(3049, 3357, 3),
                c(3019, 3345, 3),
                c(3013, 3345, 3),
                c(3015, 3333, 3),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_falador_wallclimb"),
                    11.0,
                    Climb,
                    to(3036, 3342, 3),
                    level = 50,
                ),
                Obstacle(
                    "Tightrope",
                    listOf("rooftops_falador_tightrope_1"),
                    22.0,
                    Balance,
                    to(3047, 3343, 3),
                    level = 50,
                ),
                Obstacle(
                    "Hand holds",
                    listOf("rooftops_falador_handholds_start"),
                    61.0,
                    HandHolds,
                    to(3050, 3357, 3),
                    level = 50,
                    fail = failUntil(50, 0.85, 66, c(3051, 3353), minDamage = 3, maxDamage = 8),
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_falador_gap_1"),
                    27.0,
                    Jump,
                    to(3048, 3361, 3),
                    level = 50,
                ),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_falador_gap_2"),
                    26.0,
                    Jump,
                    to(3041, 3361, 3),
                    level = 50,
                ),
                Obstacle(
                    "Tightrope (second)",
                    listOf("rooftops_falador_tightrope_2"),
                    61.0,
                    Balance,
                    to(3028, 3354, 3),
                    level = 50,
                ),
                Obstacle(
                    "Tightrope (third)",
                    listOf("rooftops_falador_tightrope_3"),
                    53.0,
                    Balance,
                    to(3020, 3353, 3),
                    level = 50,
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_falador_gap_3"),
                    30.0,
                    Jump,
                    to(3018, 3349, 3),
                    level = 50,
                ),
                Obstacle(
                    "Ledge",
                    listOf("rooftops_falador_ledge_1"),
                    14.0,
                    Jump,
                    to(3013, 3344, 3),
                    level = 50,
                ),
                Obstacle(
                    "Ledge (second)",
                    listOf("rooftops_falador_ledge_2"),
                    13.0,
                    Jump,
                    to(3013, 3335, 3),
                    level = 50,
                ),
                Obstacle(
                    "Ledge (third)",
                    listOf("rooftops_falador_ledge_3a", "rooftops_falador_ledge_3b"),
                    13.0,
                    Jump,
                    to(3017, 3333, 3),
                    level = 50,
                ),
                Obstacle(
                    "Ledge (fourth)",
                    listOf("rooftops_falador_ledge_4"),
                    14.0,
                    Jump,
                    to(3022, 3333, 3),
                    level = 50,
                ),
                Obstacle(
                    "Edge",
                    listOf("rooftops_falador_edge"),
                    241.0,
                    JumpDown,
                    to(3029, 3333, 0),
                    level = 50,
                ),
            ),
    )

val SEERS_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Seers' Village Rooftop",
        level = 60,
        markChance = ONE_THIRD,
        markSpawns =
            listOf(
                c(2727, 3496, 3),
                c(2707, 3493, 2),
                c(2710, 3493, 2),
                c(2712, 3481, 2),
                c(2698, 3465, 2),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Wall",
                    listOf("rooftops_seers_wallclimb"),
                    45.0,
                    Climb,
                    to(2729, 3491, 3),
                    level = 60,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_seers_jump"),
                    20.0,
                    Jump,
                    to(2713, 3494, 2),
                    level = 60,
                    fail = failUntil(60, 0.85, 79, c(2715, 3494), minDamage = 1, maxDamage = 4),
                ),
                Obstacle(
                    "Tightrope",
                    listOf("rooftops_seers_tightrope"),
                    20.0,
                    Balance,
                    to(2710, 3480, 2),
                    level = 60,
                    fail = failUntil(60, 0.85, 79, c(2710, 3485), minDamage = 1, maxDamage = 4),
                ),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_seers_jump_1"),
                    35.0,
                    Jump,
                    to(2712, 3472, 3),
                    level = 60,
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_seers_jump_2"),
                    15.0,
                    Jump,
                    to(2702, 3465, 2),
                    level = 60,
                ),
                Obstacle(
                    "Edge",
                    listOf("rooftops_seers_leapdown"),
                    435.0,
                    JumpDown,
                    to(2704, 3464, 0),
                    level = 60,
                ),
            ),
    )

val POLLNIVNEACH_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Pollnivneach Rooftop",
        level = 70,
        markChance = ONE_THIRD,
        markSpawns =
            listOf(
                c(3347, 2968, 1),
                c(3353, 2975, 1),
                c(3359, 2983, 2),
                c(3357, 3002, 2),
                c(3362, 2993, 2),
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Basket",
                    listOf("rooftops_pollnivneach_basket"),
                    10.0,
                    Climb,
                    to(3351, 2964, 1),
                    level = 70,
                ),
                Obstacle(
                    "Market stall",
                    listOf("rooftops_pollnivneach_marketstall"),
                    45.0,
                    Jump,
                    to(3352, 2973, 1),
                    level = 70,
                    // https://oldschool.runescape.wiki/w/Market_stall_(Pollnivneach_Rooftop_Course)
                    fail =
                        ObstacleFail(61, 302, c(3350, 2969), minDamage = 2, hpFraction = 1.0 / 17),
                ),
                Obstacle(
                    "Banner",
                    listOf("rooftops_pollnivneach_hangingbanner"),
                    65.0,
                    Swing,
                    to(3360, 2977, 1),
                    level = 70,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_pollnivneach_gap"),
                    35.0,
                    Jump,
                    to(3366, 2976, 1),
                    level = 70,
                ),
                Obstacle(
                    "Tree",
                    listOf("rooftops_pollnivneach_tree"),
                    75.0,
                    Jump,
                    to(3365, 2982, 1),
                    level = 70,
                ),
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_pollnivneach_wallclimb"),
                    5.0,
                    Climb,
                    to(3365, 2983, 2),
                    level = 70,
                ),
                Obstacle(
                    "Monkeybars",
                    listOf("rooftops_pollnivneach_monkeybars_start"),
                    55.0,
                    MonkeyBars,
                    to(3358, 2991, 2),
                    level = 70,
                ),
                Obstacle(
                    "Tree (second)",
                    listOf("rooftops_pollnivneach_treetop"),
                    60.0,
                    Jump,
                    to(3359, 2995, 2),
                    level = 70,
                ),
                Obstacle(
                    "Drying line",
                    listOf("rooftops_pollnivneach_line"),
                    540.0,
                    Swing,
                    to(3363, 2998, 0),
                    level = 70,
                    ticks = 4,
                ),
            ),
    )

val RELLEKKA_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Rellekka Rooftop",
        level = 80,
        markChance = TWO_FIFTHS,
        markSpawns = listOf(c(2623, 3675, 3), c(2628, 3652, 3), c(2641, 3650, 3)),
        obstacles =
            listOf(
                Obstacle(
                    "Rough wall",
                    listOf("rooftops_rellekka_wallclimb"),
                    20.0,
                    Climb,
                    to(2625, 3676, 3),
                    level = 80,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_rellekka_gap_1"),
                    30.0,
                    Jump,
                    to(2622, 3668, 3),
                    level = 80,
                ),
                Obstacle(
                    "Tightrope",
                    listOf("rooftops_rellekka_tightrope_1"),
                    40.0,
                    Balance,
                    to(2627, 3654, 3),
                    level = 80,
                    // Fails until 85, dealing 10% of current hitpoints.
                    fail = failUntil(80, 0.85, 85, c(2625, 3656), minDamage = 0, hpFraction = 0.1),
                ),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_rellekka_gap_2"),
                    85.0,
                    Jump,
                    to(2639, 3653, 3),
                    level = 80,
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_rellekka_gap_3"),
                    25.0,
                    Hurdle,
                    to(2643, 3657, 3),
                    level = 80,
                ),
                Obstacle(
                    "Tightrope (second)",
                    listOf("rooftops_rellekka_tightrope_3"),
                    105.0,
                    Balance,
                    to(2655, 3670, 3),
                    level = 80,
                ),
                Obstacle(
                    "Pile of fish",
                    listOf("rooftops_rellekka_dropoff"),
                    475.0,
                    JumpDown,
                    to(2652, 3676, 0),
                    level = 80,
                ),
            ),
    )

val ARDOUGNE_ROOFTOP: AgilityCourse =
    AgilityCourse(
        name = "Ardougne Rooftop",
        level = 90,
        markChance = TWO_THIRDS,
        markSpawns = listOf(c(2657, 3320, 3)),
        obstacles =
            listOf(
                Obstacle(
                    "Wooden beams",
                    listOf("rooftops_ardy_wallclimb"),
                    43.0,
                    Climb,
                    to(2671, 3299, 3),
                    level = 90,
                ),
                Obstacle(
                    "Gap",
                    listOf("rooftops_ardy_jump"),
                    65.0,
                    Jump,
                    to(2665, 3318, 3),
                    level = 90,
                    fail = failUntil(90, 0.85, 95, c(2668, 3312), minDamage = 1, maxDamage = 5),
                    ticks = 4,
                ),
                Obstacle("Plank", listOf("rooftops_ardy_plank"), 50.0, Balance, to(2656, 3318, 3)),
                Obstacle(
                    "Gap (second)",
                    listOf("rooftops_ardy_jump_2"),
                    21.0,
                    Jump,
                    to(2653, 3314, 3),
                    level = 90,
                    fail = failUntil(90, 0.85, 95, c(2650, 3313), minDamage = 1, maxDamage = 5),
                ),
                Obstacle(
                    "Gap (third)",
                    listOf("rooftops_ardy_jump_3"),
                    28.0,
                    Jump,
                    to(2651, 3300, 3),
                ),
                Obstacle(
                    "Steep roof",
                    listOf("rooftops_ardy_wallcrossing"),
                    57.0,
                    Ledge,
                    to(2654, 3296, 3),
                ),
                Obstacle(
                    "Gap (last)",
                    listOf("rooftops_ardy_jump_4"),
                    625.0,
                    JumpDown,
                    to(2668, 3297, 0),
                ),
            ),
    )

val BARBARIAN_OUTPOST: AgilityCourse =
    AgilityCourse(
        name = "Barbarian Outpost",
        level = 35,
        lapBonus = 46.3,
        strengthBonus = 41.3,
        markChance = ONE_THIRD,
        markSpawns = listOf(c(2537, 3546, 1)),
        extras =
            listOf(
                Obstacle(
                    "Obstacle pipe",
                    listOf("agility_obstical_pipe_barbarian"),
                    10.0,
                    Squeeze,
                    to(2552, 3558, 0),
                    level = 35,
                )
            ),
        obstacles =
            listOf(
                Obstacle(
                    "Ropeswing",
                    listOf("obstical_ropeswing1"),
                    22.0,
                    Swing,
                    to(2551, 3549, 0),
                    level = 35,
                    fail = failUntil(35, 0.8, 93, c(2551, 3554), minDamage = 1, maxDamage = 3),
                ),
                Obstacle(
                    "Log balance",
                    listOf("barbarian_log_balance1"),
                    13.7,
                    Balance,
                    to(2541, 3546, 0),
                    level = 35,
                    fail = failUntil(35, 0.8, 93, c(2545, 3541), minDamage = 1, maxDamage = 3),
                ),
                Obstacle(
                    "Obstacle net",
                    listOf("agility_obstical_net_barbarian"),
                    8.2,
                    Climb,
                    to(2537, 3546, 1),
                    level = 35,
                ),
                Obstacle(
                    "Balancing ledge",
                    listOf("balancing_ledge1"),
                    22.0,
                    Ledge,
                    to(2532, 3547, 1),
                    level = 35,
                    fail = failUntil(35, 0.8, 93, c(2534, 3546), minDamage = 1, maxDamage = 3),
                ),
                Obstacle(
                    "Ladder",
                    listOf("barbarian_laddertop_norim"),
                    0.0,
                    ClimbDown,
                    to(2532, 3546, 0),
                ),
                Obstacle(
                    "Crumbling wall",
                    listOf("castlecrumbly1"),
                    13.7,
                    CrumblingWall,
                    to(2537, 3553, 0),
                    level = 35,
                    at = 2536 to 3553,
                ),
                Obstacle(
                    "Crumbling wall (second)",
                    listOf("castlecrumbly1"),
                    13.7,
                    CrumblingWall,
                    to(2540, 3553, 0),
                    level = 35,
                    at = 2539 to 3553,
                ),
                Obstacle(
                    "Crumbling wall (third)",
                    listOf("castlecrumbly1"),
                    13.7,
                    CrumblingWall,
                    to(2543, 3553, 0),
                    level = 35,
                    at = 2542 to 3553,
                ),
            ),
    )

val WILDERNESS: AgilityCourse =
    AgilityCourse(
        name = "Wilderness",
        level = 52,
        lapBonus = 498.9,
        obstacles =
            listOf(
                Obstacle(
                    "Obstacle pipe",
                    listOf("obstical_pipe2"),
                    12.5,
                    Squeeze,
                    to(3004, 3950, 0),
                    level = 49,
                ),
                Obstacle(
                    "Ropeswing",
                    listOf("obstical_ropeswing2"),
                    20.0,
                    Swing,
                    to(3005, 3958, 0),
                    level = 52,
                    // Rope swing: 78% at level 1, 98% at level 99 (Mod Ash).
                    fail =
                        ObstacleFail(
                            200,
                            250,
                            c(3004, 10357),
                            minDamage = 1,
                            hpFraction = 0.15,
                            message = "You slip and fall to the pit below.",
                        ),
                ),
                Obstacle(
                    "Stepping stone",
                    listOf("steppingstone1"),
                    20.0,
                    SteppingStones,
                    to(2996, 3960, 0),
                    level = 52,
                    ticks = 6,
                    fail =
                        ObstacleFail(
                            180,
                            250,
                            c(3002, 3963),
                            minDamage = 1,
                            hpFraction = 0.2,
                            message = "...You lose your footing and fall into the lava.",
                        ),
                ),
                Obstacle(
                    "Log balance",
                    listOf("wilderness_log_balance1"),
                    20.0,
                    Balance,
                    to(2994, 3945, 0),
                    level = 52,
                    fail =
                        ObstacleFail(
                            200,
                            250,
                            c(3002, 10346),
                            minDamage = 1,
                            hpFraction = 0.15,
                            message = "You slip and fall to the pit below.",
                        ),
                ),
                Obstacle(
                    "Rocks",
                    listOf("wildclimbingrock"),
                    0.0,
                    Climb,
                    to(null, 3933, 0),
                    level = 52,
                ),
            ),
    )

val AGILITY_COURSES: List<AgilityCourse> =
    listOf(
        GNOME_STRONGHOLD,
        DRAYNOR_ROOFTOP,
        AL_KHARID_ROOFTOP,
        VARROCK_ROOFTOP,
        CANIFIS_ROOFTOP,
        FALADOR_ROOFTOP,
        SEERS_ROOFTOP,
        POLLNIVNEACH_ROOFTOP,
        RELLEKKA_ROOFTOP,
        ARDOUGNE_ROOFTOP,
        BARBARIAN_OUTPOST,
        WILDERNESS,
    )
