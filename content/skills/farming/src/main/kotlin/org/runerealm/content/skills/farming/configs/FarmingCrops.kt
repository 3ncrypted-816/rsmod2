package org.runerealm.content.skills.farming.configs

import org.runerealm.game.type.loc.LocType
import org.runerealm.game.type.obj.ObjType

/**
 * The kinds of farming patch, with their growth cycle length in 5-minute farming ticks.
 *
 * Cycle lengths follow [Farming#Growth ticks](https://oldschool.runescape.wiki/w/Farming): flowers
 * every 5 minutes, allotments and hops every 10, herbs every 20, trees every 40 and fruit trees
 * every 160.
 */
enum class PatchType(val cycleTicks: Int, val label: String) {
    Allotment(cycleTicks = 2, label = "allotment"),
    Herb(cycleTicks = 4, label = "herb patch"),
    Flower(cycleTicks = 1, label = "flower patch"),
    Tree(cycleTicks = 8, label = "tree patch"),
    FruitTree(cycleTicks = 32, label = "fruit tree patch"),
    Hops(cycleTicks = 2, label = "hops patch");

    /** Whether a crop of this type can be watered with a watering can. */
    val waterable: Boolean
        get() = this == Allotment || this == Flower || this == Hops

    /** Whether the crop is harvested by "harvest lives" (see [FarmingCrop.ctsLow]). */
    val usesLives: Boolean
        get() = this == Allotment || this == Herb || this == Hops

    /** Whether the crop is a tree that is cured by pruning with secateurs. */
    val isTree: Boolean
        get() = this == Tree || this == FruitTree
}

/**
 * A plantable crop.
 *
 * @property id the value stored in the patch varp (1-based, append-only: never reorder).
 * @property base the first `farming_transmit_*` value of the crop's growing stages in its patch
 *   multiloc (taken from the cache multiloc tables).
 * @property stages the number of growing stages; the crop is fully grown after this many cycles.
 * @property seed the obj planted (a seed, or a sapling for trees).
 * @property seedCount how many [seed] are needed to plant the patch.
 * @property plantXp xp for planting.
 * @property harvestXp xp per harvested item (per fruit for fruit trees).
 * @property checkXp xp for checking the health of a fully grown tree.
 * @property ctsLow chance-to-save a harvest life at level 1 (out of 256, before the +1).
 * @property ctsHigh chance-to-save at level 99.
 * @property disease chance (out of 128) to become diseased at the end of a growth cycle, before
 *   compost.
 * @property diseasedBase herbs only: the first diseased transmit value of this herb.
 * @property protectedBy flower crops that protect this allotment crop from disease once grown.
 * @property woodcuttingTree the regular tree whose woodcutting values a farmed tree uses.
 * @property roots the roots dug up from a cleared tree stump.
 */
class FarmingCrop(
    val id: Int,
    val type: PatchType,
    val name: String,
    val level: Int,
    val base: Int,
    val stages: Int,
    val seed: ObjType,
    val seedCount: Int,
    val produce: ObjType?,
    val plantXp: Double,
    val harvestXp: Double,
    val checkXp: Double = 0.0,
    val ctsLow: Int = 0,
    val ctsHigh: Int = 0,
    val disease: Int,
    val diseasedBase: Int = -1,
    val protectedBy: Set<String> = emptySet(),
    val woodcuttingTree: LocType? = null,
    val roots: ObjType? = null,
) {
    override fun toString(): String = "FarmingCrop(id=$id, name=$name, type=$type)"
}

/**
 * Every plantable crop, with levels, xp, chance-to-save values and transmit layouts from the wiki
 * ([Seeds](https://oldschool.runescape.wiki/w/Seeds),
 * [Variable crop yield](https://oldschool.runescape.wiki/w/Farming#Variable_crop_yield),
 * [Disease](https://oldschool.runescape.wiki/w/Disease_(Farming))) and the cache's patch multilocs.
 *
 * Disease chances the wiki lists exactly: herbs 27/128, fruit trees 18/128, maple 13/128 and magic
 * 9/128. The rest are not published; allotments, flowers and hops use the herb rate and the other
 * trees are interpolated between maple and magic.
 */
object FarmingCrops {
    private val o = FarmingObjs
    private val trees = FarmingLocs

    private const val VEG_DISEASE = 27
    private const val HERB_DISEASE = 27
    private const val FRUIT_DISEASE = 18
    private const val HIGH_CTS = 180
    private const val HERB_HIGH_CTS = 80

    /** Flower crop names, used for allotment protection. */
    const val MARIGOLD = "marigold"
    const val ROSEMARY = "rosemary"
    const val NASTURTIUM = "nasturtium"
    const val WHITE_LILY = "white lily"

    val all: List<FarmingCrop> =
        listOf(
            // Allotments (3 seeds, 10-minute cycles).
            veg(1, "potato", 1, 6, 4, o.potato_seed, o.potato, 8.0, 9.0, 101, setOf(MARIGOLD)),
            veg(2, "onion", 5, 13, 4, o.onion_seed, o.onion, 9.5, 10.5, 105, setOf(MARIGOLD)),
            veg(
                3,
                "cabbage",
                7,
                20,
                4,
                o.cabbage_seed,
                o.cabbage,
                10.0,
                11.5,
                107,
                setOf(ROSEMARY),
            ),
            veg(4, "tomato", 12, 27, 4, o.tomato_seed, o.tomato, 12.5, 14.0, 112, setOf(MARIGOLD)),
            veg(5, "sweetcorn", 20, 34, 6, o.sweetcorn_seed, o.sweetcorn, 17.0, 19.0, 88),
            veg(6, "strawberry", 31, 43, 6, o.strawberry_seed, o.strawberry, 26.0, 29.0, 103),
            veg(
                7,
                "watermelon",
                47,
                52,
                8,
                o.watermelon_seed,
                o.watermelon,
                48.5,
                54.5,
                126,
                setOf(NASTURTIUM),
            ),
            // Herbs (1 seed, 20-minute cycles).
            herb(8, "guam", 9, 4, 128, o.guam_seed, o.grimy_guam, 11.0, 12.5, 25),
            herb(
                9,
                "marrentill",
                14,
                11,
                131,
                o.marrentill_seed,
                o.grimy_marrentill,
                13.5,
                15.0,
                28,
            ),
            herb(10, "tarromin", 19, 18, 134, o.tarromin_seed, o.grimy_tarromin, 16.0, 18.0, 31),
            herb(
                11,
                "harralander",
                26,
                25,
                137,
                o.harralander_seed,
                o.grimy_harralander,
                21.5,
                24.0,
                36,
            ),
            herb(12, "ranarr", 32, 32, 140, o.ranarr_seed, o.grimy_ranarr, 27.0, 30.5, 39),
            herb(13, "toadflax", 38, 39, 143, o.toadflax_seed, o.grimy_toadflax, 34.0, 38.5, 43),
            herb(14, "irit", 44, 46, 146, o.irit_seed, o.grimy_irit, 43.0, 48.5, 46),
            herb(15, "avantoe", 50, 53, 149, o.avantoe_seed, o.grimy_avantoe, 54.5, 61.5, 50),
            herb(16, "kwuarm", 56, 68, 152, o.kwuarm_seed, o.grimy_kwuarm, 69.0, 78.0, 54),
            herb(
                17,
                "snapdragon",
                62,
                75,
                155,
                o.snapdragon_seed,
                o.grimy_snapdragon,
                87.5,
                98.5,
                57,
            ),
            herb(
                18,
                "cadantine",
                67,
                82,
                158,
                o.cadantine_seed,
                o.grimy_cadantine,
                106.5,
                120.0,
                60,
            ),
            herb(
                19,
                "lantadyme",
                73,
                89,
                161,
                o.lantadyme_seed,
                o.grimy_lantadyme,
                134.5,
                151.5,
                64,
            ),
            herb(
                20,
                "dwarf weed",
                79,
                96,
                164,
                o.dwarf_weed_seed,
                o.grimy_dwarf_weed,
                170.5,
                192.0,
                67,
            ),
            herb(21, "torstol", 85, 103, 167, o.torstol_seed, o.grimy_torstol, 199.5, 224.5, 71),
            // Flowers (1 seed, 5-minute cycles).
            flower(22, MARIGOLD, 2, 8, o.marigold_seed, o.marigolds, 8.5, 47.0),
            flower(23, ROSEMARY, 11, 13, o.rosemary_seed, o.rosemary, 12.0, 66.5),
            flower(24, NASTURTIUM, 24, 18, o.nasturtium_seed, o.nasturtiums, 19.5, 111.0),
            flower(25, "woad", 25, 23, o.woad_seed, o.woad_leaf, 20.5, 115.5),
            flower(26, "limpwurt", 26, 28, o.limpwurt_seed, o.limpwurt_root, 21.5, 120.0),
            flower(27, WHITE_LILY, 58, 37, o.white_lily_seed, o.white_lily, 42.0, 250.0),
            // Hops (4 seeds, 3 for jute, 10-minute cycles).
            hops(28, "barley", 3, 49, 4, 4, o.barley_seed, o.barley, 8.5, 9.5, 103),
            hops(
                29,
                "hammerstone",
                4,
                4,
                4,
                4,
                o.hammerstone_seed,
                o.hammerstone_hops,
                9.0,
                10.0,
                104,
            ),
            hops(30, "asgarnian", 8, 11, 5, 4, o.asgarnian_seed, o.asgarnian_hops, 10.9, 12.0, 108),
            hops(31, "jute", 13, 56, 5, 3, o.jute_seed, o.jute_fibre, 13.0, 14.5, 113),
            hops(
                32,
                "yanillian",
                16,
                19,
                6,
                4,
                o.yanillian_seed,
                o.yanillian_hops,
                14.5,
                16.0,
                116,
            ),
            hops(
                33,
                "krandorian",
                21,
                28,
                7,
                4,
                o.krandorian_seed,
                o.krandorian_hops,
                17.5,
                19.5,
                121,
            ),
            hops(
                34,
                "wildblood",
                28,
                38,
                8,
                4,
                o.wildblood_seed,
                o.wildblood_hops,
                23.0,
                26.0,
                128,
            ),
            // Trees (1 sapling, 40-minute cycles).
            tree(
                35,
                "oak",
                15,
                8,
                4,
                o.oak_sapling,
                o.oak_logs,
                14.0,
                467.3,
                22,
                trees.oak_tree_ref,
                o.oak_roots,
            ),
            tree(
                36,
                "willow",
                30,
                15,
                6,
                o.willow_sapling,
                o.willow_logs,
                25.0,
                1456.5,
                17,
                trees.willow_tree_ref,
                o.willow_roots,
            ),
            tree(
                37,
                "maple",
                45,
                24,
                8,
                o.maple_sapling,
                o.maple_logs,
                45.0,
                3403.4,
                13,
                trees.maple_tree_ref,
                o.maple_roots,
            ),
            tree(
                38,
                "yew",
                60,
                35,
                10,
                o.yew_sapling,
                o.yew_logs,
                81.0,
                7069.9,
                11,
                trees.yew_tree_ref,
                o.yew_roots,
            ),
            tree(
                39,
                "magic",
                75,
                48,
                12,
                o.magic_sapling,
                o.magic_logs,
                145.5,
                13768.3,
                9,
                trees.magic_tree_ref,
                o.magic_roots,
            ),
            // Fruit trees (1 sapling, 160-minute cycles).
            fruit(40, "apple", 27, 8, o.apple_sapling, o.cooking_apple, 22.0, 1199.5, 8.5),
            fruit(41, "banana", 33, 35, o.banana_sapling, o.banana, 28.0, 1750.5, 10.5),
            fruit(42, "orange", 39, 72, o.orange_sapling, o.orange, 35.5, 2470.2, 13.5),
            fruit(43, "curry", 42, 99, o.curry_sapling, o.curry_leaf, 40.0, 2906.9, 15.0),
            fruit(44, "pineapple", 51, 136, o.pineapple_sapling, o.pineapple, 57.0, 4605.7, 21.5),
            fruit(45, "papaya", 57, 163, o.papaya_sapling, o.papaya, 72.0, 6146.4, 27.0),
            fruit(46, "palm", 68, 200, o.palm_sapling, o.coconut, 110.5, 10150.1, 41.5),
            fruit(
                47,
                "dragonfruit",
                81,
                227,
                o.dragonfruit_sapling,
                o.dragonfruit,
                140.0,
                17335.0,
                70.0,
            ),
        )

    private val byId: Map<Int, FarmingCrop> = all.associateBy { it.id }

    init {
        check(byId.size == all.size) { "Duplicate farming crop ids." }
    }

    operator fun get(id: Int): FarmingCrop? = byId[id]

    fun forSeed(type: PatchType, seed: ObjType): FarmingCrop? =
        all.firstOrNull { it.type == type && it.seed.id == seed.id }

    fun isPlantable(seed: ObjType): Boolean = all.any { it.seed.id == seed.id }

    private fun veg(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        stages: Int,
        seed: ObjType,
        produce: ObjType,
        plantXp: Double,
        harvestXp: Double,
        ctsLow: Int,
        protectedBy: Set<String> = emptySet(),
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.Allotment,
            name = name,
            level = level,
            base = base,
            stages = stages,
            seed = seed,
            seedCount = 3,
            produce = produce,
            plantXp = plantXp,
            harvestXp = harvestXp,
            ctsLow = ctsLow,
            ctsHigh = HIGH_CTS,
            disease = VEG_DISEASE,
            protectedBy = protectedBy + WHITE_LILY,
        )

    private fun herb(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        diseasedBase: Int,
        seed: ObjType,
        produce: ObjType,
        plantXp: Double,
        harvestXp: Double,
        ctsLow: Int,
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.Herb,
            name = name,
            level = level,
            base = base,
            stages = 4,
            seed = seed,
            seedCount = 1,
            produce = produce,
            plantXp = plantXp,
            harvestXp = harvestXp,
            ctsLow = ctsLow,
            ctsHigh = HERB_HIGH_CTS,
            disease = HERB_DISEASE,
            diseasedBase = diseasedBase,
        )

    private fun flower(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        seed: ObjType,
        produce: ObjType,
        plantXp: Double,
        harvestXp: Double,
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.Flower,
            name = name,
            level = level,
            base = base,
            stages = 4,
            seed = seed,
            seedCount = 1,
            produce = produce,
            plantXp = plantXp,
            harvestXp = harvestXp,
            disease = VEG_DISEASE,
        )

    private fun hops(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        stages: Int,
        seedCount: Int,
        seed: ObjType,
        produce: ObjType,
        plantXp: Double,
        harvestXp: Double,
        ctsLow: Int,
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.Hops,
            name = name,
            level = level,
            base = base,
            stages = stages,
            seed = seed,
            seedCount = seedCount,
            produce = produce,
            plantXp = plantXp,
            harvestXp = harvestXp,
            ctsLow = ctsLow,
            ctsHigh = HIGH_CTS,
            disease = VEG_DISEASE,
        )

    private fun tree(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        stages: Int,
        sapling: ObjType,
        logs: ObjType,
        plantXp: Double,
        checkXp: Double,
        disease: Int,
        woodcuttingTree: LocType,
        roots: ObjType,
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.Tree,
            name = name,
            level = level,
            base = base,
            stages = stages,
            seed = sapling,
            seedCount = 1,
            produce = logs,
            plantXp = plantXp,
            harvestXp = 0.0,
            checkXp = checkXp,
            disease = disease,
            woodcuttingTree = woodcuttingTree,
            roots = roots,
        )

    private fun fruit(
        id: Int,
        name: String,
        level: Int,
        base: Int,
        sapling: ObjType,
        produce: ObjType,
        plantXp: Double,
        checkXp: Double,
        harvestXp: Double,
    ): FarmingCrop =
        FarmingCrop(
            id = id,
            type = PatchType.FruitTree,
            name = name,
            level = level,
            base = base,
            stages = 6,
            seed = sapling,
            seedCount = 1,
            produce = produce,
            plantXp = plantXp,
            harvestXp = harvestXp,
            checkXp = checkXp,
            disease = FRUIT_DISEASE,
        )
}

/** A tree or fruit tree seed, the seedling it makes in a filled plant pot, and its sapling. */
class TreeSeed(
    val seed: ObjType,
    val seedling: ObjType,
    val watered: ObjType,
    val sapling: ObjType,
    val level: Int,
)

object TreeSeeds {
    private val o = FarmingObjs

    val all: List<TreeSeed> =
        listOf(
            TreeSeed(o.acorn, o.oak_seedling, o.oak_seedling_watered, o.oak_sapling, 15),
            TreeSeed(
                o.willow_seed,
                o.willow_seedling,
                o.willow_seedling_watered,
                o.willow_sapling,
                30,
            ),
            TreeSeed(o.maple_seed, o.maple_seedling, o.maple_seedling_watered, o.maple_sapling, 45),
            TreeSeed(o.yew_seed, o.yew_seedling, o.yew_seedling_watered, o.yew_sapling, 60),
            TreeSeed(o.magic_seed, o.magic_seedling, o.magic_seedling_watered, o.magic_sapling, 75),
            TreeSeed(o.apple_seed, o.apple_seedling, o.apple_seedling_watered, o.apple_sapling, 27),
            TreeSeed(
                o.banana_seed,
                o.banana_seedling,
                o.banana_seedling_watered,
                o.banana_sapling,
                33,
            ),
            TreeSeed(
                o.orange_seed,
                o.orange_seedling,
                o.orange_seedling_watered,
                o.orange_sapling,
                39,
            ),
            TreeSeed(o.curry_seed, o.curry_seedling, o.curry_seedling_watered, o.curry_sapling, 42),
            TreeSeed(
                o.pineapple_seed,
                o.pineapple_seedling,
                o.pineapple_seedling_watered,
                o.pineapple_sapling,
                51,
            ),
            TreeSeed(
                o.papaya_seed,
                o.papaya_seedling,
                o.papaya_seedling_watered,
                o.papaya_sapling,
                57,
            ),
            TreeSeed(o.palm_seed, o.palm_seedling, o.palm_seedling_watered, o.palm_sapling, 68),
            TreeSeed(
                o.dragonfruit_seed,
                o.dragonfruit_seedling,
                o.dragonfruit_seedling_watered,
                o.dragonfruit_sapling,
                81,
            ),
        )
}
