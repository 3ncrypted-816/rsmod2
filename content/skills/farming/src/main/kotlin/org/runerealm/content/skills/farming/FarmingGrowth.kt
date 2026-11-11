package org.runerealm.content.skills.farming

import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.random.GameRandom
import org.runerealm.content.skills.farming.configs.FarmingCrop
import org.runerealm.content.skills.farming.configs.FarmingPatch
import org.runerealm.content.skills.farming.configs.FarmingRegion
import org.runerealm.content.skills.farming.configs.PatchType
import org.runerealm.content.skills.farming.configs.farming_varbits
import org.runerealm.content.skills.farming.configs.farming_varps
import org.runerealm.game.entity.Player

/**
 * Real-time clock for farming. Growth happens on global 5-minute "farming ticks" counted from the
 * Unix epoch, so every patch type grows at the same wall-clock times ([Growth
 * ticks](https://oldschool.runescape.wiki/w/Farming#Growth_ticks)).
 */
object FarmingClock {
    const val TICK_MILLIS: Long = 5 * 60 * 1000L

    /** Added to the real tick; tests use it to fast-forward time. */
    @Volatile var offsetTicks: Long = 0

    fun currentTick(): Long = System.currentTimeMillis() / TICK_MILLIS + offsetTicks
}

/**
 * Growth, disease and weed regrowth for farming patches.
 *
 * Every crop advances one stage per growth cycle of its [PatchType] (a cycle ends on each farming
 * tick that is a multiple of [PatchType.cycleTicks]). When a healthy crop past its first stage ends
 * a cycle it may become diseased instead of growing; a diseased crop dies at the end of the next
 * cycle unless cured. Weeds grow back on empty patches one stage per farming tick.
 */
object FarmingGrowth {
    /** Fruit trees grow one fruit every 40 minutes. */
    const val FRUIT_REGROW_TICKS: Int = 8

    /** Caps catch-up work for very long absences; every crop is long finished by then. */
    private const val MAX_CYCLES = 64

    const val BASE_LIVES: Int = 3
    const val MAX_FRUIT: Int = 6
    private const val DISEASE_ROLL = 128

    private val PATCH_ORDER: List<FarmingPatch> =
        FarmingPatch.entries.sortedBy { if (it.type == PatchType.Flower) 0 else 1 }

    fun read(player: Player, patch: FarmingPatch): PatchState = PatchState(player.vars[patch.varp])

    fun write(player: Player, patch: FarmingPatch, state: PatchState) {
        VarPlayerIntMapSetter.set(player, patch.varp, state.packed)
    }

    /**
     * Applies every farming tick between the last processed tick and [now]. Used on login (for the
     * time spent offline) and every game tick while online.
     *
     * @return `true` if at least one farming tick was applied.
     */
    fun catchUp(player: Player, now: Long, random: GameRandom): Boolean {
        val last = player.vars[farming_varps.last_cycle].toLong()
        if (last <= 0) {
            VarPlayerIntMapSetter.set(player, farming_varps.last_cycle, now.toInt())
            return false
        }
        if (now <= last) {
            return false
        }
        // Flowers first, so allotments see whether their flower is in bloom.
        for (patch in PATCH_ORDER) {
            val state = read(player, patch)
            val flower = patch.flower?.invoke()?.let { read(player, it) }
            val protected = flower != null && flowerProtects(flower, state.crop)
            val next = advance(patch.type, state, last, now, random, protected)
            if (next != state) {
                write(player, patch, next)
            }
        }
        VarPlayerIntMapSetter.set(player, farming_varps.last_cycle, now.toInt())
        return true
    }

    /** Applies the farming ticks in `(from, to]` to a single patch. */
    fun advance(
        type: PatchType,
        state: PatchState,
        from: Long,
        to: Long,
        random: GameRandom,
        protected: Boolean = false,
    ): PatchState {
        if (to <= from) {
            return state
        }
        val crop = state.crop
        if (crop == null) {
            val ticks = (to - from).coerceAtMost(PatchState.WEEDED.toLong()).toInt()
            return state.withWeeding((state.weeding - ticks).coerceAtLeast(0))
        }
        var next = state
        val cycles = cyclesBetween(from, to, type.cycleTicks).coerceAtMost(MAX_CYCLES)
        repeat(cycles) { next = cycle(crop, next, random, protected) }
        if (type == PatchType.FruitTree && next.status == CropStatus.Grown) {
            val regrown = cyclesBetween(from, to, FRUIT_REGROW_TICKS)
            next = next.withLives((next.lives + regrown).coerceAtMost(MAX_FRUIT))
        }
        return next
    }

    /** The number of multiples of [length] in `(from, to]`. */
    fun cyclesBetween(from: Long, to: Long, length: Int): Int =
        (Math.floorDiv(to, length.toLong()) - Math.floorDiv(from, length.toLong())).toInt()

    private fun cycle(
        crop: FarmingCrop,
        state: PatchState,
        random: GameRandom,
        protected: Boolean,
    ): PatchState =
        when (state.status) {
            CropStatus.Growing -> grow(crop, state, random, protected)
            CropStatus.Diseased -> state.withStatus(CropStatus.Dead).withWatered(false)
            // A chopped tree regrows from its stump; fruit tree stumps stay until dug up.
            CropStatus.Stump ->
                if (crop.type == PatchType.Tree) state.withStatus(CropStatus.Grown) else state
            else -> state
        }

    private fun grow(
        crop: FarmingCrop,
        state: PatchState,
        random: GameRandom,
        protected: Boolean,
    ): PatchState {
        if (state.stage >= 1 && !protected) {
            var chance = state.compost.diseaseChance(crop.disease)
            if (state.watered) {
                chance = (chance / 2).coerceAtLeast(1)
            }
            if (random.of(DISEASE_ROLL) < chance) {
                return state.withStatus(CropStatus.Diseased).withWatered(false)
            }
        }
        val stage = state.stage + 1
        val grown = state.withWatered(false)
        if (stage < crop.stages) {
            return grown.withStage(stage)
        }
        return when (crop.type) {
            PatchType.Allotment,
            PatchType.Herb,
            PatchType.Hops ->
                grown
                    .withStage(crop.stages)
                    .withStatus(CropStatus.Grown)
                    .withLives(BASE_LIVES + state.compost.id)
            PatchType.Flower -> grown.withStage(crop.stages).withStatus(CropStatus.Grown)
            PatchType.Tree,
            PatchType.FruitTree -> grown.withStage(crop.stages).withStatus(CropStatus.CheckHealth)
        }
    }

    /** Whether a fully grown flower protects [crop] from disease. */
    fun flowerProtects(flower: PatchState, crop: FarmingCrop?): Boolean {
        val flowerCrop = flower.crop ?: return false
        if (crop == null || flower.status != CropStatus.Grown) {
            return false
        }
        return flowerCrop.name in crop.protectedBy
    }

    /** Sends the current area's patch states through `farming_transmit_a`..`d`. */
    fun transmit(player: Player, region: FarmingRegion?) {
        if (region == null) {
            return
        }
        val varp = farming_varps.transmit_abcd
        var packed = player.vars[varp]
        val slots =
            listOf(farming_varbits.transmit_a, farming_varbits.transmit_b) +
                listOf(farming_varbits.transmit_c, farming_varbits.transmit_d)
        for ((index, patch) in region.patches.withIndex()) {
            val value = if (patch == null) 0 else FarmingTransmit.value(read(player, patch))
            val bits = slots[index].bits
            val mask = ((1 shl (bits.last - bits.first + 1)) - 1) shl bits.first
            packed = (packed and mask.inv()) or ((value shl bits.first) and mask)
        }
        if (packed != player.vars[varp]) {
            VarPlayerIntMapSetter.set(player, varp, packed)
        }
    }
}

/**
 * Maps a [PatchState] to the `farming_transmit_*` value the patch multilocs use. The layouts were
 * read from the cache's multiloc tables:
 * - allotments, flowers and hops: growing `base + stage` (`+ 64` watered), grown `base + stages`,
 *   diseased `base + 128 + stage`, dead `base + 192 + stage`.
 * - herbs: growing `base + stage`, grown `base + 4`, diseased per-herb, dead `170 + stage - 1`.
 * - trees: growing `base + stage`, check-health `base + stages`, choppable `+ 1`, stump `+ 2`,
 *   diseased `base + 64 + stage`, dead `base + 128 + stage`.
 * - fruit trees: growing `base + stage`, fruit `base + 6 + fruit`, diseased `base + 12 + stage`,
 *   dead `base + 18 + stage`, stump `base + 25`, check-health `base + 26`.
 * - weeds: `0` (overgrown) to `3` (clear).
 */
object FarmingTransmit {
    private const val HERB_DEAD_BASE = 170

    fun value(state: PatchState): Int {
        val crop = state.crop ?: return state.weeding
        val base = crop.base
        val stage = state.stage
        val stages = crop.stages
        return when (crop.type) {
            PatchType.Allotment,
            PatchType.Flower,
            PatchType.Hops ->
                when (state.status) {
                    CropStatus.Growing -> base + stage + if (state.watered) 64 else 0
                    CropStatus.Diseased -> base + 128 + stage
                    CropStatus.Dead -> base + 192 + stage
                    else -> base + stages
                }
            PatchType.Herb ->
                when (state.status) {
                    CropStatus.Growing -> base + stage
                    CropStatus.Diseased -> crop.diseasedBase + (stage - 1).coerceAtLeast(0)
                    CropStatus.Dead -> HERB_DEAD_BASE + (stage - 1).coerceAtLeast(0)
                    else -> base + stages
                }
            PatchType.Tree ->
                when (state.status) {
                    CropStatus.Growing -> base + stage
                    CropStatus.CheckHealth -> base + stages
                    CropStatus.Grown -> base + stages + 1
                    CropStatus.Stump -> base + stages + 2
                    CropStatus.Diseased -> base + 64 + stage
                    CropStatus.Dead -> base + 128 + stage
                }
            PatchType.FruitTree ->
                when (state.status) {
                    CropStatus.Growing -> base + stage
                    CropStatus.Grown -> base + 6 + state.lives.coerceIn(0, FarmingGrowth.MAX_FRUIT)
                    CropStatus.Diseased -> base + 12 + stage
                    CropStatus.Dead -> base + 18 + stage
                    CropStatus.Stump -> base + 25
                    CropStatus.CheckHealth -> base + 26
                }
        }
    }
}
