package org.runerealm.content.skills.magic.utility

import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.api.type.refs.synth.SynthReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences

typealias utility_components = UtilitySpellComponents

typealias utility_seqs = UtilitySpellSeqs

typealias utility_spots = UtilitySpellSpots

typealias utility_synths = UtilitySpellSynths

typealias utility_objs = UtilitySpellObjs

typealias utility_locs = UtilitySpellLocs

typealias utility_varbits = UtilitySpellVarBits

object UtilitySpellComponents : ComponentReferences() {
    val low_alchemy = find("magic_spellbook:low_alchemy")
    val high_alchemy = find("magic_spellbook:high_alchemy")
    val superheat = find("magic_spellbook:superheat")
    val enchant_1 = find("magic_spellbook:enchant_1")
    val enchant_2 = find("magic_spellbook:enchant_2")
    val enchant_3 = find("magic_spellbook:enchant_3")
    val enchant_4 = find("magic_spellbook:enchant_4")
    val enchant_5 = find("magic_spellbook:enchant_5")
    val enchant_6 = find("magic_spellbook:enchant_6")
    val enchant_7 = find("magic_spellbook:enchant_7")
    val enchant_bolts = find("magic_spellbook:xbows_enchant")
    val bones_bananas = find("magic_spellbook:bones_bananas")
    val bones_peaches = find("magic_spellbook:bones_peaches")
    val telegrab = find("magic_spellbook:telegrab")
    val charge_water_orb = find("magic_spellbook:charge_water_orb")
    val charge_earth_orb = find("magic_spellbook:charge_earth_orb")
    val charge_fire_orb = find("magic_spellbook:charge_fire_orb")
    val charge_air_orb = find("magic_spellbook:charge_air_orb")
    val charge = find("magic_spellbook:charge")
}

object UtilitySpellSeqs : SeqReferences() {
    val low_alchemy = find("human_castlowlvlalchemy")
    val high_alchemy = find("human_casthighlvlalchemy")
    val superheat = find("human_castsuperheatitem")
    val enchant_amulet_1 = find("human_enchantamuletlvl1")
    val enchant_amulet_2 = find("human_enchantamuletlvl2")
    val enchant_amulet_3 = find("human_enchantamuletlvl3")
    val enchant_ring = find("human_cast_enchantring")
    val enchant_bolts = find("human_xbow_enchant_arrowtip")
    val bones_to_fruit = find("human_castbonestobananas")
    val telegrab = find("human_casttelegrab")
    val charge_orb = find("human_castchargeorb")
    val charge = find("human_casting")
}

object UtilitySpellSpots : SpotanimReferences() {
    val low_alchemy = find("lowlvlalchemy_casting")
    val high_alchemy = find("highlvlalchemy_casting")
    val superheat = find("superheatitem_casting")
    val enchant_amulet_1 = find("enchant_amulet_lvl1")
    val enchant_amulet_2 = find("enchant_amulet_lvl2")
    val enchant_amulet_3 = find("enchant_amulet_lvl3")
    val enchant_amulet_4 = find("enchant_amulet2_lvl4")
    val enchant_amulet_5 = find("enchant_amulet2_lvl5")
    val enchant_amulet_6 = find("enchant_amulet2_lvl6")
    val enchant_ring = find("enchant_ring")
    val enchant_bolts = find("xbows_enchanting_tips_spot")
    val bones_to_fruit = find("bonestobananas_casting")
    val telegrab_casting = find("telegrab_casting")
    val telegrab_travel = find("telegrab_travel")
    val telegrab_impact = find("telegrab_impact")
    val charge_water_orb = find("chargewaterorb_casting")
    val charge_air_orb = find("chargewindorb_casting")
    val charge_earth_orb = find("chargeearthorb_casting")
    val charge_fire_orb = find("chargefireorb_casting")
}

object UtilitySpellSynths : SynthReferences() {
    val low_alchemy = find("low_alchemy")
    val high_alchemy = find("high_alchemy")
    val superheat = find("superheat_all")
    val superheat_fail = find("superheat_fail")
    val enchant_amulet = find("enchant_sapphire_amulet")
    val enchant_ring = find("enchant_sapphire_ring")
    val bones_to_fruit = find("bones_to_bananas_all")
    val telegrab = find("telegrab_all")
    val charge_water_orb = find("charge_water_orb")
    val charge_earth_orb = find("charge_earth_orb")
    val charge_fire_orb = find("charge_fire_orb")
    val charge_air_orb = find("charge_air_orb")
}

object UtilitySpellObjs : ObjReferences() {
    val banana = find("banana")
    val peach = find("peach")
    val bones = find("bones")
    val big_bones = find("big_bones")
    val unpowered_orb = find("stafforb")
    val water_orb = find("water_orb")
    val earth_orb = find("earth_orb")
    val fire_orb = find("fire_orb")
    val air_orb = find("air_orb")
    val goldsmith_gauntlets = find("gauntlets_of_goldsmithing")
    val elemental_ore = find("elemental_workshop_ore")
    val saradomin_cape = find("saradomin_cape")
    val guthix_cape = find("guthix_cape")
    val zamorak_cape = find("zamorak_cape")
}

object UtilitySpellLocs : LocReferences() {
    val obelisk_water = find("obelisk_water")
    val obelisk_earth = find("obelisk_earth")
    val obelisk_fire = find("obelisk_fire")
    val obelisk_air = find("obelisk_air")
}

object UtilitySpellVarBits : VarBitReferences() {
    /** The "Minimum item value needed for Alchemy spells warning" setting. */
    val alchemy_warning_threshold = find("alchemy_warning_valuethreshold")
}
