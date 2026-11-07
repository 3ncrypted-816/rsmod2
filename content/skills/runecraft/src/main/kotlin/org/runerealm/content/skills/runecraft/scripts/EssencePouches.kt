package org.runerealm.content.skills.runecraft.scripts

import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.runecraftingLvl
import org.runerealm.api.script.onOpHeld1
import org.runerealm.api.script.onOpHeld2
import org.runerealm.api.script.onOpHeld3
import org.runerealm.api.script.onOpHeldU
import org.runerealm.content.skills.runecraft.configs.runecraft_objs
import org.runerealm.content.skills.runecraft.configs.runecraft_varps
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.varp.VarpType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Essence pouches, per the OSRS Wiki ([Essence
 * pouch](https://oldschool.runescape.wiki/w/Essence_pouch)): small (3), medium (6, level 25), large
 * (9, level 50) and giant (12, level 75). Pouches hold rune, pure and daeyalt essence.
 *
 * Degradation is not implemented: degraded pouches behave as normal pouches.
 */
class EssencePouches : PluginScript() {
    override fun ScriptContext.startup() {
        for (pouch in POUCHES) {
            for (obj in pouch.objs) {
                onOpHeld1(obj) { fill(pouch) }
                onOpHeld2(obj) { empty(pouch) }
                onOpHeld3(obj) { check(pouch) }
                for (essence in ESSENCES) {
                    onOpHeldU(essence, obj) { fill(pouch) }
                }
            }
        }
    }

    private fun ProtectedAccess.fill(pouch: Pouch) {
        if (player.runecraftingLvl < pouch.level) {
            mes("You need level ${pouch.level} Runecraft to use this pouch.")
            return
        }
        val stored = PouchContents(vars[pouch.varp])
        var space = pouch.capacity - stored.total
        if (space <= 0) {
            mes("Your pouch is full.")
            return
        }
        val carried = ESSENCES.sumOf { invTotal(inv, it) }
        if (carried == 0) {
            mes("You do not have any essence to fill the pouch with.")
            return
        }
        val counts = stored.counts.toMutableList()
        // Pure and daeyalt essence go in before rune essence.
        for (index in FILL_ORDER) {
            if (space <= 0) {
                break
            }
            val essence = ESSENCES[index]
            val moved = minOf(space, invTotal(inv, essence))
            if (moved > 0) {
                invDel(inv, essence, count = moved)
                counts[index] += moved
                space -= moved
            }
        }
        vars[pouch.varp] = PouchContents.pack(counts)
    }

    private fun ProtectedAccess.empty(pouch: Pouch) {
        val stored = PouchContents(vars[pouch.varp])
        if (stored.total == 0) {
            mes("There is no essence in this pouch.")
            return
        }
        val counts = stored.counts.toMutableList()
        for (index in FILL_ORDER) {
            val space = inv.freeSpace()
            val moved = minOf(space, counts[index])
            if (moved > 0) {
                invAdd(inv, ESSENCES[index], count = moved)
                counts[index] -= moved
            }
        }
        vars[pouch.varp] = PouchContents.pack(counts)
    }

    private fun ProtectedAccess.check(pouch: Pouch) {
        val total = PouchContents(vars[pouch.varp]).total
        when (total) {
            0 -> mes("There is no essence in this pouch.")
            1 -> mes("There is 1 essence in this pouch.")
            else -> mes("There are $total essences in this pouch.")
        }
    }

    data class Pouch(
        val objs: List<ObjType>,
        val varp: VarpType,
        val level: Int,
        val capacity: Int,
    )

    private companion object {
        val ESSENCES =
            listOf(
                runecraft_objs.rune_essence,
                runecraft_objs.pure_essence,
                runecraft_objs.daeyalt_essence,
            )
        val FILL_ORDER = listOf(1, 2, 0)

        val POUCHES =
            listOf(
                Pouch(listOf(runecraft_objs.small_pouch), runecraft_varps.small_pouch, 1, 3),
                Pouch(
                    listOf(runecraft_objs.medium_pouch, runecraft_objs.medium_pouch_degraded),
                    runecraft_varps.medium_pouch,
                    25,
                    6,
                ),
                Pouch(
                    listOf(runecraft_objs.large_pouch, runecraft_objs.large_pouch_degraded),
                    runecraft_varps.large_pouch,
                    50,
                    9,
                ),
                Pouch(
                    listOf(runecraft_objs.giant_pouch, runecraft_objs.giant_pouch_degraded),
                    runecraft_varps.giant_pouch,
                    75,
                    12,
                ),
            )
    }
}

/** Essence counts packed in a pouch varp: rune essence (bits 0-7), pure (8-15), daeyalt (16-23). */
@JvmInline
internal value class PouchContents(val packed: Int) {
    val counts: List<Int>
        get() = List(ESSENCE_KINDS) { (packed shr (it * BITS)) and MASK }

    val total: Int
        get() = counts.sum()

    companion object {
        private const val ESSENCE_KINDS = 3
        private const val BITS = 8
        private const val MASK = 0xFF

        fun pack(counts: List<Int>): Int =
            counts.foldIndexed(0) { index, acc, count -> acc or (count shl (index * BITS)) }
    }
}
