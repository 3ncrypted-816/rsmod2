package org.rsmod.content.generic.locs.staircase

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences

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

    private fun bottom(type: org.rsmod.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_up
            op2 = TOP_FLOOR
        }

    private fun top(type: org.rsmod.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_down
            op2 = BOTTOM_FLOOR
        }

    private fun middle(type: org.rsmod.game.type.loc.LocType) =
        edit(type) {
            contentGroup = content.spiralstaircase_option
            op4 = TOP_FLOOR
            op5 = BOTTOM_FLOOR
        }

    const val TOP_FLOOR = "Top-floor"
    const val BOTTOM_FLOOR = "Bottom-floor"
}
