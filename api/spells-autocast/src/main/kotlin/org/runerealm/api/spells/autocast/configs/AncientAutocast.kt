package org.runerealm.api.spells.autocast.configs

import org.runerealm.api.config.refs.objs
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.game.type.obj.ObjType

/**
 * The weapons that can autocast Ancient Magicks combat spells, per the wiki
 * ([Autocast](https://oldschool.runescape.wiki/w/Autocast)): the ancient staff, ancient sceptres,
 * the Blue moon spear, the master, Kodai and dragon hunter wands, the Nightmare staff (plain,
 * eldritch and volatile), and Thammaron's and Accursed sceptre (a).
 *
 * Ahrim's staff is left out: it can only autocast ancients with the amulet of the damned and the
 * full Ahrim's set, which is not modelled.
 */
internal object AncientAutocastObjs : ObjReferences() {
    val ancient_sceptre = find("ancient_sceptre")
    val ancient_sceptre_trouver = find("ancient_sceptre_trouver")
    val ancient_sceptre_blood = find("ancient_sceptre_blood")
    val ancient_sceptre_ice = find("ancient_sceptre_ice")
    val ancient_sceptre_smoke = find("ancient_sceptre_smoke")
    val ancient_sceptre_shadow = find("ancient_sceptre_shadow")
    val ancient_sceptre_blood_trouver = find("ancient_sceptre_blood_trouver")
    val ancient_sceptre_ice_trouver = find("ancient_sceptre_ice_trouver")
    val ancient_sceptre_smoke_trouver = find("ancient_sceptre_smoke_trouver")
    val ancient_sceptre_shadow_trouver = find("ancient_sceptre_shadow_trouver")
    val blue_moon_spear = find("frostmoon_spear")
    val master_wand = find("magictraining_wand_master")
    val kodai_wand = find("kodai_wand")
    val nightmare_staff = find("nightmare_staff")
    val eldritch_nightmare_staff = find("nightmare_staff_eldritch")
    val volatile_nightmare_staff = find("nightmare_staff_volatile")
}

private val ANCIENT_AUTOCAST_WEAPONS: List<ObjType> =
    with(AncientAutocastObjs) {
        listOf(
            objs.ancient_staff,
            ancient_sceptre,
            ancient_sceptre_trouver,
            ancient_sceptre_blood,
            ancient_sceptre_ice,
            ancient_sceptre_smoke,
            ancient_sceptre_shadow,
            ancient_sceptre_blood_trouver,
            ancient_sceptre_ice_trouver,
            ancient_sceptre_smoke_trouver,
            ancient_sceptre_shadow_trouver,
            blue_moon_spear,
            master_wand,
            kodai_wand,
            objs.dragon_hunter_wand,
            nightmare_staff,
            eldritch_nightmare_staff,
            volatile_nightmare_staff,
            objs.thammarons_sceptre_a,
            objs.accursed_sceptre_a,
        )
    }

internal val ANCIENT_AUTOCAST_WEAPON_IDS: Set<Int> by lazy {
    ANCIENT_AUTOCAST_WEAPONS.mapTo(HashSet()) { it.id }
}

internal val ANCIENT_COMBAT_SPELL_IDS: Set<Int> by lazy {
    listOf(
            objs.spell_smoke_rush,
            objs.spell_smoke_burst,
            objs.spell_smoke_blitz,
            objs.spell_smoke_barrage,
            objs.spell_shadow_rush,
            objs.spell_shadow_burst,
            objs.spell_shadow_blitz,
            objs.spell_shadow_barrage,
            objs.spell_blood_rush,
            objs.spell_blood_burst,
            objs.spell_blood_blitz,
            objs.spell_blood_barrage,
            objs.spell_ice_rush,
            objs.spell_ice_burst,
            objs.spell_ice_blitz,
            objs.spell_ice_barrage,
        )
        .mapTo(HashSet()) { it.id }
}
