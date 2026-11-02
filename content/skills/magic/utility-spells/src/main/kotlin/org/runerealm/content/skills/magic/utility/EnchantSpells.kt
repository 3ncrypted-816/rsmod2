package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.objs
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.script.onIfOverlayButtonT
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.UnpackedObjType
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * The jewellery enchantment spells (Lvl-1 to Lvl-7 Enchant) and Enchant Crossbow Bolt, following
 * the OSRS Wiki ([Lvl-1 Enchant](https://oldschool.runescape.wiki/w/Lvl-1_Enchant),
 * [Enchant Crossbow Bolt](https://oldschool.runescape.wiki/w/Enchant_Crossbow_Bolt)).
 * - Jewellery enchantments are cast on one piece of jewellery and turn it into its enchanted form.
 * - Enchant Crossbow Bolt lists the bolts the player can enchant; each cast enchants a set of up to
 *   10 bolts and needs its own Magic level and runes.
 */
class EnchantSpells
@Inject
constructor(
    private val caster: UtilitySpellCaster,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (level in JEWELLERY_ENCHANTS) {
            val spell = caster.spell(level.spell)
            onIfOverlayButtonT(spell.component, components.inv_items) {
                val slot = targetSlot
                val target = targetObj ?: return@onIfOverlayButtonT
                protectedAccess.launch(player) { enchantJewellery(level, spell, slot, target) }
            }
        }

        val boltSpell = caster.spell(objs.view_bolt_enchantment_spells)
        onIfOverlayButton(boltSpell.component) {
            protectedAccess.launch(player) { enchantBolts(boltSpell) }
        }
    }

    private fun ProtectedAccess.enchantJewellery(
        level: JewelleryEnchant,
        spell: MagicSpell,
        slot: Int,
        target: UnpackedObjType,
    ) {
        if (player.isUtilitySpellDelayed()) {
            return
        }
        if (player.inv[slot]?.id != target.id) {
            return
        }
        val product = level.products[target.id]
        if (product == null) {
            mes(WRONG_ITEM_MESSAGE)
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        player.setUtilitySpellDelay(ENCHANT_DELAY)
        invReplace(player.inv, slot, 1, product)
        if (target.isRing()) {
            anim(utility_seqs.enchant_ring)
            spotanim(utility_spots.enchant_ring, height = 92)
            soundSynth(utility_synths.enchant_ring)
        } else {
            anim(level.seq)
            spotanim(level.spot, height = 92)
            soundSynth(utility_synths.enchant_amulet)
        }
    }

    private suspend fun ProtectedAccess.enchantBolts(boltSpell: MagicSpell) {
        if (player.isUtilitySpellDelayed()) {
            return
        }
        val available =
            BOLT_ENCHANTS.filter { enchant ->
                enchant.bolts.keys.any { invTotal(player.inv, it) > 0 }
            }
        if (available.isEmpty()) {
            mes(NO_BOLTS_MESSAGE)
            return
        }
        val choices = available.map { it.displayObj(this) }
        val selection = skillMulti(BOLT_TITLE, choices, maxQuantity = MAX_BOLT_CASTS)
        val enchant = available.first { it.displayObj(this).id == selection.obj.id }

        val spell =
            boltSpell.copy(
                levelReq = enchant.level,
                castXp = enchant.xp,
                objReqs = enchant.runes.map { MagicSpell.ObjRequirement(it.first, it.second, null) },
            )
        repeat(selection.quantity) { cast ->
            if (cast > 0) {
                delay(BOLT_CAST_DELAY)
            }
            val bolts = enchant.bolts.entries.firstOrNull { invTotal(player.inv, it.key) > 0 }
            if (bolts == null) {
                return
            }
            if (!caster.cast(this, spell)) {
                return
            }
            val count = minOf(BOLTS_PER_CAST, invTotal(player.inv, bolts.key))
            invDel(player.inv, bolts.key, count)
            invAdd(player.inv, bolts.value, count)
            anim(utility_seqs.enchant_bolts)
            spotanim(utility_spots.enchant_bolts)
            mes(BOLTS_ENCHANTED_MESSAGE)
        }
    }

    private fun UnpackedObjType.isRing(): Boolean = name.contains("ring", ignoreCase = true)

    /** The unenchanted bolt shown on the selection menu, preferring the one the player has. */
    private fun BoltEnchant.displayObj(access: ProtectedAccess): ObjType =
        bolts.keys.firstOrNull { access.invTotal(access.player.inv, it) > 0 } ?: bolts.keys.first()

    private class JewelleryEnchant(
        val spell: ObjType,
        val seq: SeqType,
        val spot: SpotanimType,
        val products: Map<Int, ObjType>,
    )

    private class BoltEnchant(
        val level: Int,
        val xp: Double,
        val runes: List<Pair<ObjType, Int>>,
        val bolts: Map<ObjType, ObjType>,
    )

    companion object {
        const val WRONG_ITEM_MESSAGE: String = "This spell cannot be cast on that item."
        const val NO_BOLTS_MESSAGE: String = "You don't have any bolts that can be enchanted."
        const val BOLTS_ENCHANTED_MESSAGE: String =
            "The magic of the runes coaxes out the true nature of the gem tips."
        const val BOLT_TITLE: String = "What would you like to enchant?"
        const val ENCHANT_DELAY: Int = 3
        const val BOLTS_PER_CAST: Int = 10
        const val BOLT_CAST_DELAY: Int = 3
        const val MAX_BOLT_CASTS: Int = 28

        private val o = utility_enchant_objs

        private val JEWELLERY_ENCHANTS: List<JewelleryEnchant> =
            listOf(
                JewelleryEnchant(
                    objs.spell_enchant_lvl_1,
                    utility_seqs.enchant_amulet_1,
                    utility_spots.enchant_amulet_1,
                    products(
                        o.sapphire_ring to o.ring_of_recoil,
                        o.sapphire_necklace to o.games_necklace,
                        o.sapphire_bracelet to o.bracelet_of_clay,
                        o.sapphire_amulet to o.amulet_of_magic,
                        o.opal_ring to o.ring_of_pursuit,
                        o.opal_necklace to o.dodgy_necklace,
                        o.opal_bracelet to o.expeditious_bracelet,
                        o.opal_amulet to o.amulet_of_bounty,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_2,
                    utility_seqs.enchant_amulet_2,
                    utility_spots.enchant_amulet_2,
                    products(
                        o.emerald_ring to o.ring_of_dueling,
                        o.emerald_necklace to o.binding_necklace,
                        o.emerald_bracelet to o.castle_wars_bracelet,
                        o.emerald_amulet to o.amulet_of_defence,
                        o.jade_ring to o.ring_of_returning,
                        o.jade_necklace to o.necklace_of_passage,
                        o.jade_bracelet to o.flamtaer_bracelet,
                        o.jade_amulet to o.amulet_of_chemistry,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_3,
                    utility_seqs.enchant_amulet_3,
                    utility_spots.enchant_amulet_3,
                    products(
                        o.ruby_ring to o.ring_of_forging,
                        o.ruby_necklace to o.digsite_pendant,
                        o.ruby_bracelet to o.inoculation_bracelet,
                        o.ruby_amulet to o.amulet_of_strength,
                        o.topaz_ring to o.efaritays_aid,
                        o.topaz_necklace to o.necklace_of_faith,
                        o.topaz_bracelet to o.bracelet_of_slaughter,
                        o.topaz_amulet to o.burning_amulet,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_4,
                    utility_seqs.enchant_amulet_3,
                    utility_spots.enchant_amulet_4,
                    products(
                        o.diamond_ring to o.ring_of_life,
                        o.diamond_necklace to o.phoenix_necklace,
                        o.diamond_bracelet to o.abyssal_bracelet,
                        o.diamond_amulet to o.amulet_of_power,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_5,
                    utility_seqs.enchant_amulet_3,
                    utility_spots.enchant_amulet_5,
                    products(
                        o.dragonstone_ring to o.ring_of_wealth,
                        o.dragon_necklace to o.skills_necklace,
                        o.dragonstone_bracelet to o.combat_bracelet,
                        o.dragonstone_amulet to o.amulet_of_glory,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_6,
                    utility_seqs.enchant_amulet_3,
                    utility_spots.enchant_amulet_6,
                    products(
                        o.onyx_ring to o.ring_of_stone,
                        o.onyx_necklace to o.berserker_necklace,
                        o.onyx_bracelet to o.regen_bracelet,
                        o.onyx_amulet to o.amulet_of_fury,
                    ),
                ),
                JewelleryEnchant(
                    objs.spell_enchant_lvl_7,
                    utility_seqs.enchant_amulet_3,
                    utility_spots.enchant_amulet_6,
                    products(
                        o.zenyte_ring to o.ring_of_suffering,
                        o.zenyte_necklace to o.necklace_of_anguish,
                        o.zenyte_bracelet to o.tormented_bracelet,
                        o.zenyte_amulet to o.amulet_of_torture,
                    ),
                ),
            )

        /** Per the wiki's Enchant Crossbow Bolt table. Each cast enchants up to 10 bolts. */
        private val BOLT_ENCHANTS: List<BoltEnchant> =
            listOf(
                BoltEnchant(
                    4,
                    9.0,
                    listOf(objs.cosmic_rune to 1, objs.air_rune to 2),
                    mapOf(
                        o.opal_bolts to o.opal_bolts_e,
                        o.opal_dragon_bolts to o.opal_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    7,
                    17.5,
                    listOf(objs.cosmic_rune to 1, objs.water_rune to 1, objs.mind_rune to 1),
                    mapOf(
                        o.sapphire_bolts to o.sapphire_bolts_e,
                        o.sapphire_dragon_bolts to o.sapphire_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    14,
                    19.0,
                    listOf(objs.cosmic_rune to 1, objs.earth_rune to 2),
                    mapOf(
                        o.jade_bolts to o.jade_bolts_e,
                        o.jade_dragon_bolts to o.jade_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    24,
                    29.5,
                    listOf(objs.cosmic_rune to 1, objs.water_rune to 2),
                    mapOf(
                        o.pearl_bolts to o.pearl_bolts_e,
                        o.pearl_dragon_bolts to o.pearl_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    27,
                    37.0,
                    listOf(objs.cosmic_rune to 1, objs.air_rune to 3, objs.nature_rune to 1),
                    mapOf(
                        o.emerald_bolts to o.emerald_bolts_e,
                        o.emerald_dragon_bolts to o.emerald_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    29,
                    33.0,
                    listOf(objs.cosmic_rune to 1, objs.fire_rune to 2),
                    mapOf(
                        o.topaz_bolts to o.topaz_bolts_e,
                        o.topaz_dragon_bolts to o.topaz_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    49,
                    59.0,
                    listOf(objs.cosmic_rune to 1, objs.fire_rune to 5, objs.blood_rune to 1),
                    mapOf(
                        o.ruby_bolts to o.ruby_bolts_e,
                        o.ruby_dragon_bolts to o.ruby_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    57,
                    67.0,
                    listOf(objs.cosmic_rune to 1, objs.earth_rune to 10, objs.law_rune to 2),
                    mapOf(
                        o.diamond_bolts to o.diamond_bolts_e,
                        o.diamond_dragon_bolts to o.diamond_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    68,
                    78.0,
                    listOf(objs.cosmic_rune to 1, objs.earth_rune to 15, objs.soul_rune to 1),
                    mapOf(
                        o.dragonstone_bolts to o.dragonstone_bolts_e,
                        o.dragonstone_dragon_bolts to o.dragonstone_dragon_bolts_e,
                    ),
                ),
                BoltEnchant(
                    87,
                    97.0,
                    listOf(objs.cosmic_rune to 1, objs.fire_rune to 20, objs.death_rune to 1),
                    mapOf(
                        o.onyx_bolts to o.onyx_bolts_e,
                        o.onyx_dragon_bolts to o.onyx_dragon_bolts_e,
                    ),
                ),
            )

        private fun products(vararg pairs: Pair<ObjType, ObjType>): Map<Int, ObjType> =
            pairs.associate { (base, product) -> base.id to product }
    }
}
