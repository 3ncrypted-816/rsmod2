package org.runerealm.content.generic.locs.staircase

import org.runerealm.api.config.refs.content
import org.runerealm.api.type.editors.loc.LocEditor
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.game.type.loc.LocType

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
 * Spiral staircases keep their cache ops, as in OSRS: "Top-floor"/"Bottom-floor" are not added by
 * the server but belong to dedicated loc types. Lumbridge Castle's three-floor staircases use
 * `spiralstairsbottom_3` on the ground floor ("Climb-up", "Top-floor") and `spiralstairstop_3` on
 * the top floor ("Climb-down", "Bottom-floor"), while its middle floor is a plain
 * `spiralstairsmiddle` ("Climb", "Climb-up", "Climb-down"). Every other staircase only has the
 * regular climb ops.
 */
internal object StaircaseLocEdits : LocEditor() {
    init {
        up(staircase_locs.spiral_bottom)
        up(staircase_locs.wooden_spiral_bottom)
        up(staircase_locs.lumbridge_spiral_bottom)

        down(staircase_locs.spiral_top)
        down(staircase_locs.spiral_down)
        down(staircase_locs.wooden_spiral_top)
        down(staircase_locs.wooden_spiral_down)
        down(staircase_locs.lumbridge_spiral_top)

        option(staircase_locs.spiral_middle)
        option(staircase_locs.wooden_spiral_middle)
    }

    private fun up(type: LocType) = edit(type) { contentGroup = content.spiralstaircase_up }

    private fun down(type: LocType) = edit(type) { contentGroup = content.spiralstaircase_down }

    private fun option(type: LocType) = edit(type) { contentGroup = content.spiralstaircase_option }
}
