package org.runerealm.content.generic.locs.staircase

import org.runerealm.api.config.refs.content
import org.runerealm.api.type.editors.loc.LocEditor
import org.runerealm.api.type.refs.loc.LocReferences

typealias staircase_locs = StaircaseLocs

object StaircaseLocs : LocReferences() {
    val spiral_top = find("spiralstairstop")
    val spiral_bottom = find("spiralstairs")
    val spiral_middle = find("spiralstairsmiddle")
    val spiral_down = find("spiralstairsdown")
    val wooden_spiral_top = find("spiralstairstop_wooden")
    val wooden_spiral_bottom = find("spiralstairs_wooden")
    val wooden_spiral_middle = find("spiralstairsmiddle_wooden")
    val wooden_spiral_down = find("spiralstairsdown_wooden")
    val lumbridge_spiral_bottom = find("spiralstairsbottom_3")
    val lumbridge_spiral_top = find("spiralstairstop_3")
}

/**
 * Spiral staircases. Besides the regular climb options, every staircase that leads up offers
 * "Top-floor" and every staircase that leads down offers "Bottom-floor", jumping straight to the
 * highest/lowest floor of the staircase stack. (Lumbridge Castle's staircases have these options in
 * the base game; they are added to the other spiral staircases on this server.)
 */
// An edit that sets any op replaces the loc's whole op list, so the original climb ops are set
// again alongside the new ones.
internal object StaircaseLocEdits : LocEditor() {
    init {
        bottom(staircase_locs.spiral_bottom)
        bottom(staircase_locs.wooden_spiral_bottom)
        bottom(staircase_locs.lumbridge_spiral_bottom)

        top(staircase_locs.spiral_top)
        top(staircase_locs.spiral_down)
        top(staircase_locs.wooden_spiral_top)
        top(staircase_locs.wooden_spiral_down)
        top(staircase_locs.lumbridge_spiral_top)

        middle(staircase_locs.spiral_middle)
        middle(staircase_locs.wooden_spiral_middle)
    }

    private fun bottom(type: org.runerealm.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_up
            op1 = "Climb-up"
            op2 = TOP_FLOOR
        }

    private fun top(type: org.runerealm.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_down
            op1 = "Climb-down"
            op2 = BOTTOM_FLOOR
        }

    private fun middle(type: org.runerealm.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_option
            op1 = "Climb"
            op2 = "Climb-up"
            op3 = "Climb-down"
            op4 = TOP_FLOOR
            op5 = BOTTOM_FLOOR
        }

    const val TOP_FLOOR = "Top-floor"
    const val BOTTOM_FLOOR = "Bottom-floor"
}
