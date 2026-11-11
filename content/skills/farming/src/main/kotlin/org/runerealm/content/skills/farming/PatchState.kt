package org.runerealm.content.skills.farming

import org.runerealm.content.skills.farming.configs.FarmingCrop
import org.runerealm.content.skills.farming.configs.FarmingCrops

/** What a planted crop is doing. */
enum class CropStatus(val id: Int) {
    Growing(0),
    Diseased(1),
    Dead(2),

    /**
     * Ready: harvestable allotments/herbs/hops/flowers, a choppable tree, or a fruit tree that has
     * had its health checked (its fruit count is in [PatchState.lives]).
     */
    Grown(3),

    /** A fully grown tree or fruit tree waiting for its health to be checked. */
    CheckHealth(4),

    /** A chopped tree stump. */
    Stump(5);

    companion object {
        private val byId = entries.associateBy { it.id }

        operator fun get(id: Int): CropStatus = byId[id] ?: Growing
    }
}

/** Compost a patch has been treated with. */
enum class Compost(val id: Int, val label: String, val xp: Double) {
    None(0, "", 0.0),
    Regular(1, "compost", 18.0),
    Super(2, "supercompost", 26.0),
    Ultra(3, "ultracompost", 36.0);

    /**
     * Disease chance (out of 128) after this compost, rounded up as on the
     * [Disease](https://oldschool.runescape.wiki/w/Disease_(Farming)) table.
     */
    fun diseaseChance(base: Int): Int =
        when (this) {
            None -> base
            Regular -> ceilDiv(base * 50, 100)
            Super -> ceilDiv(base * 20, 100)
            Ultra -> ceilDiv(base * 10, 100)
        }.coerceAtLeast(1)

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

    companion object {
        private val byId = entries.associateBy { it.id }

        operator fun get(id: Int): Compost = byId[id] ?: None
    }
}

/**
 * The server-side state of one patch, packed into its patch varp:
 * - bits 0-7: crop id ([FarmingCrop.id], 0 = nothing planted)
 * - bits 8-11: growth stage
 * - bits 12-14: [CropStatus]
 * - bits 15-16: weeding progress (0 = fully overgrown .. 3 = clear soil)
 * - bits 17-18: [Compost]
 * - bit 19: watered this cycle
 * - bits 20-23: harvest lives left, or fruit left on a fruit tree
 */
@JvmInline
value class PatchState(val packed: Int) {
    val cropId: Int
        get() = packed and 0xFF

    val crop: FarmingCrop?
        get() = if (cropId == 0) null else FarmingCrops[cropId]

    val stage: Int
        get() = (packed shr 8) and 0xF

    val status: CropStatus
        get() = CropStatus[(packed shr 12) and 0x7]

    val weeding: Int
        get() = (packed shr 15) and 0x3

    val compost: Compost
        get() = Compost[(packed shr 17) and 0x3]

    val watered: Boolean
        get() = (packed shr 19) and 0x1 == 1

    val lives: Int
        get() = (packed shr 20) and 0xF

    val isEmpty: Boolean
        get() = cropId == 0

    val isWeeded: Boolean
        get() = isEmpty && weeding == WEEDED

    fun withCrop(crop: FarmingCrop?): PatchState = set(0, 0xFF, crop?.id ?: 0)

    fun withStage(stage: Int): PatchState = set(8, 0xF, stage)

    fun withStatus(status: CropStatus): PatchState = set(12, 0x7, status.id)

    fun withWeeding(weeding: Int): PatchState = set(15, 0x3, weeding)

    fun withCompost(compost: Compost): PatchState = set(17, 0x3, compost.id)

    fun withWatered(watered: Boolean): PatchState = set(19, 0x1, if (watered) 1 else 0)

    fun withLives(lives: Int): PatchState = set(20, 0xF, lives.coerceIn(0, 0xF))

    private fun set(shift: Int, mask: Int, value: Int): PatchState =
        PatchState((packed and (mask shl shift).inv()) or ((value and mask) shl shift))

    override fun toString(): String =
        "PatchState(crop=${crop?.name}, stage=$stage, status=$status, weeding=$weeding, " +
            "compost=$compost, watered=$watered, lives=$lives)"

    companion object {
        const val WEEDED: Int = 3

        /** A clear, weeded patch with nothing planted. */
        val CLEAR: PatchState = PatchState(0).withWeeding(WEEDED)

        fun planted(crop: FarmingCrop, compost: Compost): PatchState =
            CLEAR.withCrop(crop).withCompost(compost).withStatus(CropStatus.Growing).withStage(0)
    }
}
