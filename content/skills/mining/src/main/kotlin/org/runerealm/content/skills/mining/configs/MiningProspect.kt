package org.runerealm.content.skills.mining.configs

import org.runerealm.api.type.editors.loc.LocEditor
import org.runerealm.api.type.refs.loc.LocReferences

/**
 * Rocks that are given the classic "Prospect" option.
 *
 * Jagex removed the option from OSRS when rocks were renamed to "[ore] rocks", but it is kept here
 * on request. The op is added to op2, which is unused on every mining rock.
 */
internal object ProspectRockLocs : LocReferences() {
    val ores =
        listOf(
                "copperrock1",
                "copperrock2",
                "tinrock1",
                "tinrock2",
                "clayrock1",
                "clayrock2",
                "ironrock1",
                "ironrock2",
                "coalrock1",
                "coalrock2",
                "silverrock1",
                "silverrock2",
                "goldrock1",
                "goldrock2",
                "mithrilrock1",
                "mithrilrock2",
                "adamantiterock1",
                "adamantiterock2",
                "runiterock1",
                "runiterock2",
                "blurite_rock_1",
                "blurite_rock_2",
                "gemrock1",
                "gemrock",
                "amethystrock1",
                "amethystrock2",
                "lovakite_rock1",
                "lovakite_rock2",
                "softclayrock1",
                "softclayrock2",
                "enakh_granite_rocks",
                "enakh_sandstone_rocks",
                "gim_ironrock",
                "prif_mine_ironrock1",
                "prif_mine_coalrock1",
                "prif_mine_silverrock1",
                "prif_mine_goldrock1",
                "prif_mine_mithrilrock1",
                "prif_mine_adamantiterock1",
                "prif_mine_runiterock1",
                "prif_mine_softclayrock1",
                "lotr_mine_wall_tin_mid",
                "lotr_mine_wall_tin_l",
                "lotr_mine_wall_tin_r",
                "lotr_mine_wall_tin_single",
                "lotr_mine_wall_copper_mid",
                "lotr_mine_wall_copper_l",
                "lotr_mine_wall_copper_r",
                "lotr_mine_wall_copper_single",
                "lotr_mine_wall_clay_mid",
                "lotr_mine_wall_clay_l",
                "lotr_mine_wall_clay_r",
                "lotr_mine_wall_clay_single",
                "lotr_mine_wall_coal_l",
                "lotr_mine_wall_coal_r",
                "lotr_mine_wall_coal_single",
                "lotr_mine_wall_coal_mid",
                "lotr_mine_wall_adamant_l",
                "lotr_mine_wall_adamant_r",
                "lotr_mine_wall_adamant_single",
                "lotr_mine_wall_adamant_mid",
                "lotr_mine_wall_mithril_l",
                "lotr_mine_wall_mithril_r",
                "lotr_mine_wall_mithril_single",
                "lotr_mine_wall_mithril_mid",
                "lotr_mine_wall_iron_l",
                "lotr_mine_wall_iron_r",
                "lotr_mine_wall_iron_single",
                "lotr_mine_wall_iron_mid",
            )
            .map { find(it) }

    val empties =
        listOf(
                "rocks1",
                "rocks2",
                "rocks3",
                "prif_mine_rocks1_empty",
                "lotr_mine_wall_depleted_l",
                "lotr_mine_wall_depleted_mid",
                "lotr_mine_wall_depleted_r",
                "lotr_mine_wall_depleted_single",
                "amethystrock_empty",
            )
            .map { find(it) }
}

internal object ProspectRockEditor : LocEditor() {
    init {
        for (type in ProspectRockLocs.ores + ProspectRockLocs.empties) {
            // Editing one op replaces the loc's whole op list, so the original ops are restated.
            edit(type) {
                op1 = "Mine"
                op2 = PROSPECT_OP
                op3 = "hidden"
            }
        }
    }

    const val PROSPECT_OP = "Prospect"
}
