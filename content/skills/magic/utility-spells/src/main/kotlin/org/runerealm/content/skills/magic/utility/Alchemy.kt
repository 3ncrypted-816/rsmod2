package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.objs
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.script.onIfOverlayButtonT
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.UnpackedObjType
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.game.type.synth.SynthType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Low and High Level Alchemy, following the OSRS Wiki ([High Level
 * Alchemy](https://oldschool.runescape.wiki/w/High_Level_Alchemy)):
 * - The item is turned into coins: 60% of its value for High Level Alchemy, 40% for Low Level
 *   Alchemy (rounded down). Stacks (including noted items) are alchemised one at a time.
 * - High Level Alchemy can be recast every 5 ticks, Low Level Alchemy every 3.
 * - Casting on an item worth at least the "Minimum item value needed for Alchemy spells warning"
 *   setting (1,000 coins by default) asks for confirmation first.
 */
class Alchemy
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val caster: UtilitySpellCaster,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(LOW)
        register(HIGH)
    }

    private fun ScriptContext.register(type: AlchemyType) {
        val spell = caster.spell(type.spell)
        onIfOverlayButtonT(spell.component, components.inv_items) {
            val slot = targetSlot
            val target = targetObj ?: return@onIfOverlayButtonT
            protectedAccess.launch(player) { alchemise(type, spell, slot, target) }
        }
    }

    private suspend fun ProtectedAccess.alchemise(
        type: AlchemyType,
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
        if (target.id == objs.coins.id) {
            mes(COINS_MESSAGE)
            return
        }
        if (!caster.canCast(player, spell)) {
            return
        }

        val base = objTypes.uncert(target)
        val coins = alchemyValue(base.cost, type.percent)
        if (base.cost >= warningThreshold()) {
            val proceed =
                choice2(
                    "Yes",
                    true,
                    "No",
                    false,
                    title = "Really cast ${type.name} on ${base.name}?",
                )
            if (!proceed || player.inv[slot]?.id != target.id) {
                return
            }
        }

        val invObj = player.inv[slot] ?: return
        val lastOfStack = invObj.count == 1
        val hasCoinStack = invTotal(player.inv, objs.coins) > 0
        val freeSlots = player.inv.freeSpace()
        if (coins > 0 && !lastOfStack && !hasCoinStack && freeSlots == 0) {
            mes(INV_SPACE_MESSAGE)
            return
        }

        if (!caster.cast(this, spell)) {
            return
        }
        player.setUtilitySpellDelay(type.delay)
        invDel(player.inv, target, count = 1, slot = slot)
        if (coins > 0) {
            invAdd(player.inv, objs.coins, coins)
        }
        anim(type.seq)
        spotanim(type.spot, height = 92)
        soundSynth(type.synth)
    }

    private fun ProtectedAccess.warningThreshold(): Int {
        val setting = player.vars[utility_varbits.alchemy_warning_threshold]
        return if (setting > 0) setting else DEFAULT_WARNING_THRESHOLD
    }

    private class AlchemyType(
        val name: String,
        val spell: ObjType,
        val percent: Int,
        val delay: Int,
        val seq: SeqType,
        val spot: SpotanimType,
        val synth: SynthType,
    )

    companion object {
        /** The wiki's default warning threshold. */
        const val DEFAULT_WARNING_THRESHOLD: Int = 1_000

        const val COINS_MESSAGE: String = "Coins are already made of gold."
        const val INV_SPACE_MESSAGE: String = "You don't have enough inventory space."

        private val LOW =
            AlchemyType(
                name = "Low Level Alchemy",
                spell = objs.spell_low_alchemy,
                percent = 40,
                delay = 3,
                seq = utility_seqs.low_alchemy,
                spot = utility_spots.low_alchemy,
                synth = utility_synths.low_alchemy,
            )

        private val HIGH =
            AlchemyType(
                name = "High Level Alchemy",
                spell = objs.spell_high_alchemy,
                percent = 60,
                delay = 5,
                seq = utility_seqs.high_alchemy,
                spot = utility_spots.high_alchemy,
                synth = utility_synths.high_alchemy,
            )

        /** High Level Alchemy gives 60% of an item's value, Low Level Alchemy 40%. */
        fun alchemyValue(cost: Int, percent: Int): Int = (cost.toLong() * percent / 100).toInt()
    }
}
