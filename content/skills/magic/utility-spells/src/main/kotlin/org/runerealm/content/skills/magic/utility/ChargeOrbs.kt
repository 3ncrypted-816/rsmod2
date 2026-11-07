package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.objs
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.script.onOpLocT
import org.runerealm.game.type.loc.LocType
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.game.type.synth.SynthType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * The charge orb spells, following the OSRS Wiki ([Charge Water
 * Orb](https://oldschool.runescape.wiki/w/Charge_Water_Orb)): cast on the matching obelisk, each
 * cast turns one unpowered orb into an elemental orb. The unpowered orb is part of the spell's
 * cost.
 */
class ChargeOrbs @Inject constructor(private val caster: UtilitySpellCaster) : PluginScript() {
    override fun ScriptContext.startup() {
        for (orb in ORBS) {
            val spell = caster.spell(orb.spell)
            for (obelisk in OBELISKS) {
                onOpLocT(obelisk, spell.component) { chargeOrb(spell, orb, obelisk) }
            }
        }
    }

    private suspend fun ProtectedAccess.chargeOrb(spell: MagicSpell, orb: OrbSpell, at: LocType) {
        if (at.id != orb.obelisk.id) {
            mes("You need to cast this spell on the ${orb.element} obelisk.")
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        anim(utility_seqs.charge_orb)
        spotanim(orb.spot, height = 92)
        soundSynth(orb.synth)
        invAdd(player.inv, orb.product)
        delay(CAST_DELAY)
    }

    private class OrbSpell(
        val spell: ObjType,
        val element: String,
        val obelisk: LocType,
        val product: ObjType,
        val spot: SpotanimType,
        val synth: SynthType,
    )

    companion object {
        const val CAST_DELAY: Int = 3

        private val ORBS: List<OrbSpell> =
            listOf(
                OrbSpell(
                    objs.spell_chargeorb_water,
                    "water",
                    utility_locs.obelisk_water,
                    utility_objs.water_orb,
                    utility_spots.charge_water_orb,
                    utility_synths.charge_water_orb,
                ),
                OrbSpell(
                    objs.spell_chargeorb_earth,
                    "earth",
                    utility_locs.obelisk_earth,
                    utility_objs.earth_orb,
                    utility_spots.charge_earth_orb,
                    utility_synths.charge_earth_orb,
                ),
                OrbSpell(
                    objs.spell_chargeorb_fire,
                    "fire",
                    utility_locs.obelisk_fire,
                    utility_objs.fire_orb,
                    utility_spots.charge_fire_orb,
                    utility_synths.charge_fire_orb,
                ),
                OrbSpell(
                    objs.spell_chargeorb_air,
                    "air",
                    utility_locs.obelisk_air,
                    utility_objs.air_orb,
                    utility_spots.charge_air_orb,
                    utility_synths.charge_air_orb,
                ),
            )

        private val OBELISKS: List<LocType> =
            listOf(
                utility_locs.obelisk_water,
                utility_locs.obelisk_earth,
                utility_locs.obelisk_fire,
                utility_locs.obelisk_air,
            )
    }
}
