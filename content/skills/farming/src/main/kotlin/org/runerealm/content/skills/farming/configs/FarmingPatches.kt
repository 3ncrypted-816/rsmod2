package org.runerealm.content.skills.farming.configs

import org.runerealm.game.type.loc.LocType
import org.runerealm.game.type.varp.VarpType
import org.runerealm.map.CoordGrid

private typealias locs = FarmingLocs

private typealias varps = FarmingVarps

/**
 * A farming patch. Each patch has its own base loc (every tile of a multi-tile allotment uses the
 * same loc), so the loc type alone identifies the patch.
 *
 * @property flower the flower patch next to an allotment, which protects it once grown.
 */
enum class FarmingPatch(
    val type: PatchType,
    val location: String,
    private val locRef: () -> LocType,
    private val varpRef: () -> VarpType,
    val flower: (() -> FarmingPatch)? = null,
) {
    FaladorAllotmentNw(
        PatchType.Allotment,
        "Falador",
        { locs.falador_allotment_nw },
        { varps.falador_allotment_nw },
        { FaladorFlower },
    ),
    FaladorAllotmentSe(
        PatchType.Allotment,
        "Falador",
        { locs.falador_allotment_se },
        { varps.falador_allotment_se },
        { FaladorFlower },
    ),
    FaladorFlower(PatchType.Flower, "Falador", { locs.falador_flower }, { varps.falador_flower }),
    FaladorHerb(PatchType.Herb, "Falador", { locs.falador_herb }, { varps.falador_herb }),
    CatherbyAllotmentN(
        PatchType.Allotment,
        "Catherby",
        { locs.catherby_allotment_n },
        { varps.catherby_allotment_n },
        { CatherbyFlower },
    ),
    CatherbyAllotmentS(
        PatchType.Allotment,
        "Catherby",
        { locs.catherby_allotment_s },
        { varps.catherby_allotment_s },
        { CatherbyFlower },
    ),
    CatherbyFlower(
        PatchType.Flower,
        "Catherby",
        { locs.catherby_flower },
        { varps.catherby_flower },
    ),
    CatherbyHerb(PatchType.Herb, "Catherby", { locs.catherby_herb }, { varps.catherby_herb }),
    ArdougneAllotmentN(
        PatchType.Allotment,
        "Ardougne",
        { locs.ardougne_allotment_n },
        { varps.ardougne_allotment_n },
        { ArdougneFlower },
    ),
    ArdougneAllotmentS(
        PatchType.Allotment,
        "Ardougne",
        { locs.ardougne_allotment_s },
        { varps.ardougne_allotment_s },
        { ArdougneFlower },
    ),
    ArdougneFlower(
        PatchType.Flower,
        "Ardougne",
        { locs.ardougne_flower },
        { varps.ardougne_flower },
    ),
    ArdougneHerb(PatchType.Herb, "Ardougne", { locs.ardougne_herb }, { varps.ardougne_herb }),
    MorytaniaAllotmentNw(
        PatchType.Allotment,
        "Port Phasmatys",
        { locs.morytania_allotment_nw },
        { varps.morytania_allotment_nw },
        { MorytaniaFlower },
    ),
    MorytaniaAllotmentSe(
        PatchType.Allotment,
        "Port Phasmatys",
        { locs.morytania_allotment_se },
        { varps.morytania_allotment_se },
        { MorytaniaFlower },
    ),
    MorytaniaFlower(
        PatchType.Flower,
        "Port Phasmatys",
        { locs.morytania_flower },
        { varps.morytania_flower },
    ),
    MorytaniaHerb(
        PatchType.Herb,
        "Port Phasmatys",
        { locs.morytania_herb },
        { varps.morytania_herb },
    ),
    TaverleyTree(PatchType.Tree, "Taverley", { locs.taverley_tree }, { varps.taverley_tree }),
    FaladorTree(PatchType.Tree, "Falador", { locs.falador_tree }, { varps.falador_tree }),
    VarrockTree(PatchType.Tree, "Varrock", { locs.varrock_tree }, { varps.varrock_tree }),
    LumbridgeTree(PatchType.Tree, "Lumbridge", { locs.lumbridge_tree }, { varps.lumbridge_tree }),
    GnomeTree(PatchType.Tree, "Gnome Stronghold", { locs.gnome_tree }, { varps.gnome_tree }),
    GnomeFruitTree(
        PatchType.FruitTree,
        "Gnome Stronghold",
        { locs.gnome_fruit_tree },
        { varps.gnome_fruit_tree },
    ),
    VillageFruitTree(
        PatchType.FruitTree,
        "Tree Gnome Village",
        { locs.village_fruit_tree },
        { varps.village_fruit_tree },
    ),
    BrimhavenFruitTree(
        PatchType.FruitTree,
        "Brimhaven",
        { locs.brimhaven_fruit_tree },
        { varps.brimhaven_fruit_tree },
    ),
    CatherbyFruitTree(
        PatchType.FruitTree,
        "Catherby",
        { locs.catherby_fruit_tree },
        { varps.catherby_fruit_tree },
    ),
    YanilleHops(PatchType.Hops, "Yanille", { locs.yanille_hops }, { varps.yanille_hops }),
    EntranaHops(PatchType.Hops, "Entrana", { locs.entrana_hops }, { varps.entrana_hops }),
    LumbridgeHops(PatchType.Hops, "Lumbridge", { locs.lumbridge_hops }, { varps.lumbridge_hops }),
    SeersHops(PatchType.Hops, "Seers' Village", { locs.seers_hops }, { varps.seers_hops });

    val loc: LocType
        get() = locRef()

    val varp: VarpType
        get() = varpRef()

    companion object {
        private val byLoc: Map<Int, FarmingPatch> by lazy { entries.associateBy { it.loc.id } }

        fun forLoc(locId: Int): FarmingPatch? = byLoc[locId]
    }
}

/**
 * The areas the client renders farming patches in, and which patch each `farming_transmit_*` varbit
 * shows there. The cache's patch multilocs read `farming_transmit_a`..`d`, so the server fills them
 * with the current area's patch states, as the official server does.
 *
 * Mapsquares and bounds follow the RuneLite time tracking plugin's `FarmingWorld`, and the loc
 * placements were checked against the cache map files.
 */
enum class FarmingRegion(
    private val mapsquares: Set<Int>,
    val a: FarmingPatch? = null,
    val b: FarmingPatch? = null,
    val c: FarmingPatch? = null,
    val d: FarmingPatch? = null,
    private val bounds: (CoordGrid) -> Boolean = { true },
) {
    FaladorFarm(
        setOf(12083),
        a = FarmingPatch.FaladorAllotmentNw,
        b = FarmingPatch.FaladorAllotmentSe,
        c = FarmingPatch.FaladorFlower,
        d = FarmingPatch.FaladorHerb,
        // The Port Sarim spirit tree shares the mapsquare south of here.
        bounds = { it.z >= 3272 },
    ),
    CatherbyFarm(
        setOf(11062, 11061, 11318, 11317),
        a = FarmingPatch.CatherbyAllotmentN,
        b = FarmingPatch.CatherbyAllotmentS,
        c = FarmingPatch.CatherbyFlower,
        d = FarmingPatch.CatherbyHerb,
        bounds = { c ->
            if (c.x >= 2816 && c.z < 3456) c.x < 2840 && c.z >= 3440 && c.level == 0 else true
        },
    ),
    CatherbyOrchard(
        setOf(11317),
        a = FarmingPatch.CatherbyFruitTree,
        bounds = { c -> c.x >= 2840 || c.z < 3440 || c.level == 1 },
    ),
    ArdougneFarm(
        setOf(10548),
        a = FarmingPatch.ArdougneAllotmentN,
        b = FarmingPatch.ArdougneAllotmentS,
        c = FarmingPatch.ArdougneFlower,
        d = FarmingPatch.ArdougneHerb,
    ),
    MorytaniaFarm(
        setOf(14391, 14390),
        a = FarmingPatch.MorytaniaAllotmentNw,
        b = FarmingPatch.MorytaniaAllotmentSe,
        c = FarmingPatch.MorytaniaFlower,
        d = FarmingPatch.MorytaniaHerb,
    ),
    TaverleyTree(setOf(11573, 11829), a = FarmingPatch.TaverleyTree),
    FaladorTree(setOf(11828, 12084), a = FarmingPatch.FaladorTree),
    VarrockTree(setOf(12854, 12853), a = FarmingPatch.VarrockTree),
    LumbridgeTree(setOf(12594, 12850), a = FarmingPatch.LumbridgeTree),
    GnomeStronghold(
        setOf(9781, 9782, 9526, 9525),
        a = FarmingPatch.GnomeTree,
        b = FarmingPatch.GnomeFruitTree,
    ),
    TreeGnomeVillage(setOf(9777, 10033), a = FarmingPatch.VillageFruitTree),
    Brimhaven(setOf(11058, 11057), a = FarmingPatch.BrimhavenFruitTree),
    YanilleHops(setOf(10288), a = FarmingPatch.YanilleHops),
    EntranaHops(setOf(11060, 11316), a = FarmingPatch.EntranaHops),
    LumbridgeHops(setOf(12851), a = FarmingPatch.LumbridgeHops),
    SeersHops(setOf(10551, 10550), a = FarmingPatch.SeersHops);

    val patches: List<FarmingPatch?>
        get() = listOf(a, b, c, d)

    fun contains(coords: CoordGrid): Boolean = mapsquare(coords) in mapsquares && bounds(coords)

    companion object {
        fun mapsquare(coords: CoordGrid): Int = ((coords.x shr 6) shl 8) or (coords.z shr 6)

        fun at(coords: CoordGrid): FarmingRegion? = entries.firstOrNull { it.contains(coords) }
    }
}
