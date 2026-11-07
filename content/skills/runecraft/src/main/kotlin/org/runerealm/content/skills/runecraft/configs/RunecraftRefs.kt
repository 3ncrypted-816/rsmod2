@file:Suppress("SpellCheckingInspection")

package org.runerealm.content.skills.runecraft.configs

import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.api.type.refs.varp.VarpReferences

typealias runecraft_objs = RunecraftObjs

typealias runecraft_locs = RunecraftLocs

typealias runecraft_varbits = RunecraftVarBits

typealias runecraft_varps = RunecraftVarps

typealias runecraft_seqs = RunecraftSeqs

typealias runecraft_spotanims = RunecraftSpotanims

object RunecraftSeqs : SeqReferences() {
    val bind = find("human_runecraft")
}

object RunecraftSpotanims : SpotanimReferences() {
    val bind = find("runecrafting")
}

object RunecraftVarps : VarpReferences() {
    /** Essence stored in each pouch: rune essence in bits 0-7, pure 8-15, daeyalt 16-23. */
    val small_pouch = find("rc_pouch_small")
    val medium_pouch = find("rc_pouch_medium")
    val large_pouch = find("rc_pouch_large")
    val giant_pouch = find("rc_pouch_giant")
}

internal object RunecraftVarpBuilder : VarpBuilder() {
    init {
        build("rc_pouch_small")
        build("rc_pouch_medium")
        build("rc_pouch_large")
        build("rc_pouch_giant")
    }
}

/** Client varbits that switch each set of mysterious ruins to the "Enter" variant. */
object RunecraftVarBits : VarBitReferences() {
    val air = find("rc_no_tally_required_air")
    val mind = find("rc_no_tally_required_mind")
    val water = find("rc_no_tally_required_water")
    val earth = find("rc_no_tally_required_earth")
    val fire = find("rc_no_tally_required_fire")
    val body = find("rc_no_tally_required_body")
    val cosmic = find("rc_no_tally_required_cosmic")
    val law = find("rc_no_tally_required_law")
    val nature = find("rc_no_tally_required_nature")
    val chaos = find("rc_no_tally_required_chaos")
    val death = find("rc_no_tally_required_death")
    val wrath = find("rc_no_tally_required_wrath")
    val blood = find("rc_no_tally_required_blood")
}

object RunecraftLocs : LocReferences() {
    val air_ruins = find("airtemple_ruined")
    val air_ruins_old = find("airtemple_ruined_old")
    val air_ruins_new = find("airtemple_ruined_new")
    val mind_ruins = find("mindtemple_ruined")
    val mind_ruins_old = find("mindtemple_ruined_old")
    val mind_ruins_new = find("mindtemple_ruined_new")
    val water_ruins = find("watertemple_ruined")
    val water_ruins_old = find("watertemple_ruined_old")
    val water_ruins_new = find("watertemple_ruined_new")
    val earth_ruins = find("earthtemple_ruined")
    val earth_ruins_old = find("earthtemple_ruined_old")
    val earth_ruins_new = find("earthtemple_ruined_new")
    val fire_ruins = find("firetemple_ruined")
    val fire_ruins_old = find("firetemple_ruined_old")
    val fire_ruins_new = find("firetemple_ruined_new")
    val body_ruins = find("bodytemple_ruined")
    val body_ruins_old = find("bodytemple_ruined_old")
    val body_ruins_new = find("bodytemple_ruined_new")
    val cosmic_ruins = find("cosmictemple_ruined")
    val cosmic_ruins_old = find("cosmictemple_ruined_old")
    val cosmic_ruins_new = find("cosmictemple_ruined_new")
    val chaos_ruins = find("chaostemple_ruined")
    val chaos_ruins_old = find("chaostemple_ruined_old")
    val chaos_ruins_new = find("chaostemple_ruined_new")
    val nature_ruins = find("naturetemple_ruined")
    val nature_ruins_old = find("naturetemple_ruined_old")
    val nature_ruins_new = find("naturetemple_ruined_new")
    val law_ruins = find("lawtemple_ruined")
    val law_ruins_old = find("lawtemple_ruined_old")
    val law_ruins_new = find("lawtemple_ruined_new")
    val death_ruins = find("deathtemple_ruined")
    val death_ruins_old = find("deathtemple_ruined_old")
    val death_ruins_new = find("deathtemple_ruined_new")
    val blood_ruins = find("bloodtemple_ruined")
    val blood_ruins_old = find("bloodtemple_ruined_0op")
    val blood_ruins_new = find("bloodtemple_ruined_1op")
    val wrath_ruins = find("wrathtemple_ruined")
    val wrath_ruins_old = find("wrathtemple_ruined_0op")
    val wrath_ruins_new = find("wrathtemple_ruined_1op")

    val air_altar = find("air_altar")
    val mind_altar = find("mind_altar")
    val water_altar = find("water_altar")
    val earth_altar = find("earth_altar")
    val fire_altar = find("fire_altar")
    val body_altar = find("body_altar")
    val cosmic_altar = find("cosmic_altar")
    val chaos_altar = find("chaos_altar")
    val nature_altar = find("nature_altar")
    val law_altar = find("law_altar")
    val death_altar = find("death_altar")
    val blood_altar = find("blood_altar")
    val astral_altar = find("astral_altar")
    val wrath_altar = find("wrath_altar")
    val kourend_blood_altar = find("archeus_altar_blood")
    val soul_altar = find("archeus_altar_soul")

    val air_portal = find("airtemple_exit_portal")
    val mind_portal = find("mindtemple_exit_portal")
    val water_portal = find("watertemple_exit_portal")
    val earth_portal = find("earthtemple_exit_portal")
    val fire_portal = find("firetemple_exit_portal")
    val body_portal = find("bodytemple_exit_portal")
    val cosmic_portal = find("cosmictemple_exit_portal")
    val chaos_portal = find("chaostemple_exit_portal")
    val nature_portal = find("naturetemple_exit_portal")
    val law_portal = find("lawtemple_exit_portal")
    val death_portal = find("deathtemple_exit_portal")
    val blood_portal = find("bloodtemple_exit_portal")
    val wrath_portal = find("wrathtemple_exit_portal")
}

object RunecraftObjs : ObjReferences() {
    val rune_essence = find("blankrune")
    val pure_essence = find("blankrune_high")
    val daeyalt_essence = find("blankrune_daeyalt")
    val dark_essence_fragments = find("bigblankrune")

    val air_rune = find("airrune")
    val mind_rune = find("mindrune")
    val water_rune = find("waterrune")
    val earth_rune = find("earthrune")
    val fire_rune = find("firerune")
    val body_rune = find("bodyrune")
    val cosmic_rune = find("cosmicrune")
    val chaos_rune = find("chaosrune")
    val astral_rune = find("astralrune")
    val nature_rune = find("naturerune")
    val law_rune = find("lawrune")
    val death_rune = find("deathrune")
    val blood_rune = find("bloodrune")
    val soul_rune = find("soulrune")
    val wrath_rune = find("wrathrune")
    val mist_rune = find("mistrune")
    val dust_rune = find("dustrune")
    val mud_rune = find("mudrune")
    val smoke_rune = find("smokerune")
    val steam_rune = find("steamrune")
    val lava_rune = find("lavarune")

    val air_talisman = find("air_talisman")
    val mind_talisman = find("mind_talisman")
    val water_talisman = find("water_talisman")
    val earth_talisman = find("earth_talisman")
    val fire_talisman = find("fire_talisman")
    val body_talisman = find("body_talisman")
    val cosmic_talisman = find("cosmic_talisman")
    val chaos_talisman = find("chaos_talisman")
    val nature_talisman = find("nature_talisman")
    val law_talisman = find("law_talisman")
    val death_talisman = find("death_talisman")
    val blood_talisman = find("blood_talisman")
    val wrath_talisman = find("wrath_talisman")
    val elemental_talisman = find("elemental_talisman")
    val catalytic_talisman = find("catalytic_talisman")

    val tiara = find("tiara")
    val air_tiara = find("tiara_air")
    val mind_tiara = find("tiara_mind")
    val water_tiara = find("tiara_water")
    val earth_tiara = find("tiara_earth")
    val fire_tiara = find("tiara_fire")
    val body_tiara = find("tiara_body")
    val cosmic_tiara = find("tiara_cosmic")
    val chaos_tiara = find("tiara_chaos")
    val nature_tiara = find("tiara_nature")
    val law_tiara = find("tiara_law")
    val death_tiara = find("tiara_death")
    val blood_tiara = find("tiara_blood")
    val wrath_tiara = find("tiara_wrath")
    val elemental_tiara = find("tiara_elemental")
    val catalytic_tiara = find("tiara_catalytic")

    val runecraft_cape = find("skillcape_runecrafting")
    val runecraft_cape_t = find("skillcape_runecrafting_trimmed")
    val binding_necklace = find("magic_emerald_necklace")

    val small_pouch = find("rcu_pouch_small")
    val medium_pouch = find("rcu_pouch_medium")
    val medium_pouch_degraded = find("rcu_pouch_medium_degrade")
    val large_pouch = find("rcu_pouch_large")
    val large_pouch_degraded = find("rcu_pouch_large_degrade")
    val giant_pouch = find("rcu_pouch_giant")
    val giant_pouch_degraded = find("rcu_pouch_giant_degrade")
}
