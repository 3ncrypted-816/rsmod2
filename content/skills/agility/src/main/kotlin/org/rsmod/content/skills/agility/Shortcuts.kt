@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

/** How the player moves across a shortcut, which decides its animations and timing. */
enum class ShortcutStyle {
    /** Climbing over a low or crumbling wall. */
    ClimbOver,
    /** Jumping over a fence. */
    FenceJump,
    /** Crawling through a tunnel under a wall. */
    Tunnel,
    /** Squeezing through a pipe. */
    Pipe,
    /** Squeezing through a narrow gap, railing or crevice. */
    Squeeze,
    /** Jumping across stepping stones. */
    SteppingStones,
    /** Scrambling up or down a rocky slope. */
    Scramble,
}

/**
 * An agility shortcut between [sideA] and [sideB]. Using any of [locs] (obj internal names) moves
 * the player from whichever side they are on to the other.
 *
 * Levels and experience are from the OSRS Wiki
 * [Shortcuts](https://oldschool.runescape.wiki/w/Shortcuts) list; loc positions and the walkable
 * tiles either side are taken from the game map.
 */
data class Shortcut(
    val name: String,
    val locs: List<String>,
    val level: Int,
    val xp: Double,
    val sideA: CoordGrid,
    val sideB: CoordGrid,
    val style: ShortcutStyle,
)

val SHORTCUTS: List<Shortcut> =
    listOf(
        Shortcut(
            "Crumbling wall (Falador)",
            listOf("fai_falador_castle_crumble_mid"),
            level = 5,
            xp = 0.5,
            sideA = CoordGrid(2934, 3355, 0),
            sideB = CoordGrid(2936, 3355, 0),
            style = ShortcutStyle.ClimbOver,
        ),
        Shortcut(
            "Fence (Varrock)",
            listOf("lumbridge_sc_fencejump"),
            level = 13,
            xp = 0.0,
            // The fence is a wall on the south edge of 3240,3335.
            sideA = CoordGrid(3240, 3334, 0),
            sideB = CoordGrid(3240, 3335, 0),
            style = ShortcutStyle.FenceJump,
        ),
        Shortcut(
            "Underwall tunnel (Grand Exchange)",
            listOf("varrock_sc_tunnel_west", "varrock_sc_tunnel_east"),
            level = 21,
            xp = 0.0,
            sideA = CoordGrid(3137, 3516, 0),
            sideB = CoordGrid(3142, 3513, 0),
            style = ShortcutStyle.Tunnel,
        ),
        Shortcut(
            "Underwall tunnel (Falador)",
            listOf("falador_sc_castlewall_south", "falador_sc_castlewall_north"),
            level = 26,
            xp = 0.0,
            sideA = CoordGrid(2948, 3309, 0),
            sideB = CoordGrid(2948, 3313, 0),
            style = ShortcutStyle.Tunnel,
        ),
        Shortcut(
            "Stepping stones (Champions' Guild)",
            listOf("lumbridge_sc_stepstone"),
            level = 31,
            xp = 3.0,
            sideA = CoordGrid(3149, 3363, 0),
            sideB = CoordGrid(3154, 3363, 0),
            style = ShortcutStyle.SteppingStones,
        ),
        Shortcut(
            "Rocks (Al Kharid)",
            listOf("alkharid_mine_sc_bottom", "alkharid_mine_sc_top"),
            level = 38,
            xp = 0.0,
            sideA = CoordGrid(3302, 3315, 0),
            sideB = CoordGrid(3306, 3315, 0),
            style = ShortcutStyle.Scramble,
        ),
        Shortcut(
            "Narrow crevice (Dwarven Mine)",
            listOf("dwarf_mines_sc_wall_crack"),
            level = 42,
            xp = 0.0,
            sideA = CoordGrid(3028, 9806, 0),
            sideB = CoordGrid(3035, 9806, 0),
            style = ShortcutStyle.Squeeze,
        ),
        Shortcut(
            "Underwall tunnel (Draynor Village)",
            listOf("draynor_diary_under_wall_w", "draynor_diary_under_wall_e"),
            level = 42,
            xp = 0.0,
            sideA = CoordGrid(3066, 3257, 0),
            sideB = CoordGrid(3070, 3257, 0),
            style = ShortcutStyle.Tunnel,
        ),
        Shortcut(
            "Obstacle pipe (Edgeville Dungeon)",
            listOf("varrock_dungeon_pipe_sc"),
            level = 51,
            xp = 10.0,
            sideA = CoordGrid(3149, 9906, 0),
            sideB = CoordGrid(3155, 9906, 0),
            style = ShortcutStyle.Pipe,
        ),
        Shortcut(
            "Loose railing (Taverley Dungeon)",
            listOf("deepdungeonlooserailing"),
            level = 63,
            xp = 0.0,
            // The railing is a wall on the east edge of 2935,9810.
            sideA = CoordGrid(2935, 9810, 0),
            sideB = CoordGrid(2936, 9810, 0),
            style = ShortcutStyle.Squeeze,
        ),
        Shortcut(
            "Obstacle pipe (Taverley Dungeon)",
            listOf("taverly_dungeon_pipe_sc"),
            level = 70,
            xp = 10.0,
            sideA = CoordGrid(2886, 9799, 0),
            sideB = CoordGrid(2892, 9799, 0),
            style = ShortcutStyle.Pipe,
        ),
    )
