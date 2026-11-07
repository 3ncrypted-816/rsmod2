package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.hands
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.player.stat.smithingLvl
import org.runerealm.api.script.onIfOverlayButtonT
import org.runerealm.content.skills.smithing.configs.SmithingBar
import org.runerealm.content.skills.smithing.configs.SmithingBars
import org.runerealm.content.skills.smithing.configs.smithing_objs
import org.runerealm.game.type.obj.UnpackedObjType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * [Superheat Item](https://oldschool.runescape.wiki/w/Superheat_Item): smelts ore into a bar
 * without a furnace, with a 100% success rate.
 * - It needs the same Smithing level and ores (including coal) as smelting at a furnace, and gives
 *   the same Smithing experience (goldsmith gauntlets apply).
 * - Iron ore with at least two coal always makes a steel bar (at 30 Smithing); otherwise an iron
 *   bar.
 * - It cannot be cast on noted ore.
 */
class SuperheatItem
@Inject
constructor(
    private val caster: UtilitySpellCaster,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = caster.spell(objs.spell_superheat)
        onIfOverlayButtonT(spell.component, components.inv_items) {
            val slot = targetSlot
            val target = targetObj ?: return@onIfOverlayButtonT
            protectedAccess.launch(player) { superheat(spell, slot, target) }
        }
    }

    private fun ProtectedAccess.superheat(spell: MagicSpell, slot: Int, target: UnpackedObjType) {
        if (player.isUtilitySpellDelayed()) {
            return
        }
        if (player.inv[slot]?.id != target.id) {
            return
        }
        if (target.id == utility_objs.elemental_ore.id) {
            fail(NOT_HOT_ENOUGH_MESSAGE)
            return
        }
        val bar = resolveBar(target)
        if (bar == null) {
            fail(NOT_ORE_MESSAGE)
            return
        }
        if (player.smithingLvl < bar.level) {
            mes("You need a Smithing level of ${bar.level} to smelt ${bar.metal}.")
            return
        }
        val missing = bar.ores.firstOrNull { (ore, count) -> invTotal(player.inv, ore) < count }
        if (missing != null) {
            mes(missingMessage(bar))
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        player.setUtilitySpellDelay(DELAY)
        for ((ore, count) in bar.ores) {
            invDel(player.inv, ore, count)
        }
        invAdd(player.inv, bar.bar)
        statAdvance(stats.smithing, smithingXp(bar))
        anim(utility_seqs.superheat)
        spotanim(utility_spots.superheat, height = 92)
        soundSynth(utility_synths.superheat)
    }

    private fun ProtectedAccess.fail(message: String) {
        mes(message)
        soundSynth(utility_synths.superheat_fail)
    }

    private fun ProtectedAccess.resolveBar(target: UnpackedObjType): SmithingBar? {
        val ore = target.id
        return when (ore) {
            smithing_objs.copper_ore.id,
            smithing_objs.tin_ore.id -> SmithingBars.bronze
            smithing_objs.iron_ore.id -> ironBar()
            else -> SmithingBars.all.firstOrNull { it.ores.first().first.id == ore }
        }
    }

    /** Iron ore makes steel when the player has two coal and the Smithing level for it. */
    private fun ProtectedAccess.ironBar(): SmithingBar {
        val steel = SmithingBars.steel
        val coal = invTotal(player.inv, smithing_objs.coal)
        return if (coal >= 2 && player.smithingLvl >= steel.level) steel else SmithingBars.iron
    }

    private fun ProtectedAccess.smithingXp(bar: SmithingBar): Double =
        if (bar == SmithingBars.gold && player.hands?.id == utility_objs.goldsmith_gauntlets.id) {
            SmithingBars.GOLDSMITH_GAUNTLETS_XP
        } else {
            bar.xp
        }

    private fun missingMessage(bar: SmithingBar): String {
        if (bar == SmithingBars.bronze) {
            return "You need one copper ore and one tin ore to make bronze."
        }
        val coal = bar.ores.firstOrNull { it.first.id == smithing_objs.coal.id }?.second
        if (coal != null) {
            return "You need $coal heaps of coal to smelt ${bar.metal}."
        }
        return "You need ${bar.metal} ore to make a ${bar.metal} bar."
    }

    companion object {
        const val NOT_ORE_MESSAGE: String = "You need to cast superheat item on ore."
        const val NOT_HOT_ENOUGH_MESSAGE: String =
            "Even this spell is not hot enough to heat this item."
        const val DELAY: Int = 3
    }
}
