package org.runerealm.content.skills.herblore.scripts

import kotlin.math.min
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.content.skills.herblore.configs.HerbloreRecipes
import org.runerealm.content.skills.herblore.configs.PotionSeries
import org.runerealm.content.skills.herblore.configs.herblore_objs

/**
 * Combining and decanting potion doses, per the OSRS Wiki
 * ([Decanting](https://oldschool.runescape.wiki/w/Decanting)).
 */
internal object PotionDoses {
    /**
     * Pours the potion in [fromSlot] into the potion in [toSlot] (both of [series]), filling the
     * target up to its maximum dose count. An emptied vial is left behind.
     */
    fun combine(access: ProtectedAccess, fromSlot: Int, toSlot: Int, series: PotionSeries) {
        with(access) {
            val from = inv[fromSlot] ?: return
            val to = inv[toSlot] ?: return
            val fromDoses = series.dosesOf(from.id)
            val toDoses = series.dosesOf(to.id)
            if (fromDoses == 0 || toDoses == 0) {
                return
            }
            if (fromDoses == series.maxDoses && toDoses == series.maxDoses) {
                mes("Nothing interesting happens.")
                return
            }
            // Fill whichever potion holds more doses already.
            val (source, target, sourceDoses, targetDoses) =
                if (toDoses >= fromDoses) {
                    Pour(fromSlot, toSlot, fromDoses, toDoses)
                } else {
                    Pour(toSlot, fromSlot, toDoses, fromDoses)
                }
            val moved = min(sourceDoses, series.maxDoses - targetDoses)
            val remaining = sourceDoses - moved
            invReplaceSlot(inv, target, 1, series.dose(targetDoses + moved))
            val leftover = if (remaining > 0) series.dose(remaining) else herblore_objs.vial
            invReplaceSlot(inv, source, 1, leftover)
            mes("You have combined the liquid into ${targetDoses + moved} doses.")
        }
    }

    /**
     * Decants every (unnoted) potion carried into potions of [doses] doses each. Returns the number
     * of potion kinds that were decanted.
     */
    fun decantAll(access: ProtectedAccess, doses: Int): Int {
        var decanted = 0
        for (series in HerbloreRecipes.potionSeries) {
            if (access.decant(series, doses)) {
                decanted++
            }
        }
        return decanted
    }

    private fun ProtectedAccess.decant(series: PotionSeries, doses: Int): Boolean {
        val slots = inv.indices.filter { slot -> series.dosesOf(inv[slot]?.id ?: -1) > 0 }
        if (slots.isEmpty()) {
            return false
        }
        val total = slots.sumOf { series.dosesOf(inv[it]!!.id) }
        val full = total / doses
        val rest = total % doses
        val outputs = full + if (rest > 0) 1 else 0
        // Already decanted: every potion holds the requested dose (bar one remainder).
        val alreadyDone =
            slots.count { series.dosesOf(inv[it]!!.id) == doses } == full && slots.size == outputs
        if (alreadyDone) {
            return false
        }
        val extraVials = outputs - slots.size
        if (extraVials > 0 && invTotal(inv, herblore_objs.vial) < extraVials) {
            mes("You don't have enough empty vials to decant your ${series.name} potions.")
            return false
        }
        if (extraVials > 0 && inv.freeSpace() + invTotal(inv, herblore_objs.vial) < extraVials) {
            mes("You don't have enough inventory space to decant your ${series.name} potions.")
            return false
        }
        for (slot in slots) {
            val type = series.dose(series.dosesOf(inv[slot]!!.id))
            invDel(inv, type, slot = slot)
        }
        if (extraVials > 0) {
            invDel(inv, herblore_objs.vial, count = extraVials)
        }
        if (full > 0) {
            invAdd(inv, series.dose(doses), count = full)
        }
        if (rest > 0) {
            invAdd(inv, series.dose(rest))
        }
        if (extraVials < 0) {
            invAdd(inv, herblore_objs.vial, count = -extraVials)
        }
        return true
    }

    private data class Pour(
        val source: Int,
        val target: Int,
        val sourceDoses: Int,
        val targetDoses: Int,
    )
}
