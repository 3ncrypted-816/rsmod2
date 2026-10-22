package org.runerealm.content.skills.runecraft.configs

import org.runerealm.game.type.loc.LocType
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.varbit.VarBitType
import org.runerealm.map.CoordGrid

/** Which essence an altar binds. */
enum class EssenceKind {
    /** Rune, pure or daeyalt essence (air to body). */
    Any,
    /** Pure or daeyalt essence. */
    Pure,
    /** Dark essence fragments (Kourend blood and soul altars). */
    Dark,
}

/** The mysterious ruins outside an altar, and where entering and leaving them places a player. */
data class Ruins(
    val variants: List<LocType>,
    val varbit: VarBitType,
    val talisman: ObjType,
    val tiara: ObjType,
    val inside: CoordGrid,
    val outside: CoordGrid,
)

/**
 * A runic altar.
 *
 * @param multiples the Runecraft levels at which each essence makes 1, 2, 3, ... runes.
 */
data class Altar(
    val name: String,
    val altar: LocType,
    val rune: ObjType,
    val level: Int,
    val xp: Double,
    val essence: EssenceKind,
    val multiples: List<Int>,
    val ruins: Ruins? = null,
    val portal: LocType? = null,
    val tiaraXp: Double = 0.0,
) {
    /** The number of runes made per essence at Runecraft [level]. */
    fun runesPerEssence(level: Int): Int = multiples.count { level >= it }.coerceAtLeast(1)
}

/** A combination rune made at [altar] with [rune] and the matching [talisman]. */
data class ComboRune(
    val product: ObjType,
    val altar: LocType,
    val rune: ObjType,
    val talisman: ObjType,
    val level: Int,
    val xp: Double,
)

/**
 * Runic altars, per the OSRS Wiki ([Runecraft](https://oldschool.runescape.wiki/w/Runecraft),
 * [Mysterious ruins](https://oldschool.runescape.wiki/w/Mysterious_ruins)). Ruin, altar and portal
 * positions come from the cache map; arrival and exit squares are next to the exit portal and on
 * the south side of the ruins.
 */
object RunecraftAltars {
    private val objs = runecraft_objs
    private val locs = runecraft_locs
    private val varbits = runecraft_varbits

    val air =
        Altar(
            name = "air",
            altar = locs.air_altar,
            rune = objs.air_rune,
            level = 1,
            xp = 5.0,
            essence = EssenceKind.Any,
            multiples = listOf(1, 11, 22, 33, 44, 55, 66, 77, 88, 99),
            ruins =
                Ruins(
                    listOf(locs.air_ruins_old, locs.air_ruins_new),
                    varbits.air,
                    objs.air_talisman,
                    objs.air_tiara,
                    inside = CoordGrid(2841, 4829),
                    outside = CoordGrid(2985, 3290),
                ),
            portal = locs.air_portal,
            tiaraXp = 25.0,
        )

    val mind =
        Altar(
            name = "mind",
            altar = locs.mind_altar,
            rune = objs.mind_rune,
            level = 2,
            xp = 5.5,
            essence = EssenceKind.Any,
            multiples = listOf(2, 14, 28, 42, 56, 70, 84, 98),
            ruins =
                Ruins(
                    listOf(locs.mind_ruins_old, locs.mind_ruins_new),
                    varbits.mind,
                    objs.mind_talisman,
                    objs.mind_tiara,
                    inside = CoordGrid(2793, 4828),
                    outside = CoordGrid(2982, 3512),
                ),
            portal = locs.mind_portal,
            tiaraXp = 27.5,
        )

    val water =
        Altar(
            name = "water",
            altar = locs.water_altar,
            rune = objs.water_rune,
            level = 5,
            xp = 6.0,
            essence = EssenceKind.Any,
            multiples = listOf(5, 19, 38, 57, 76, 95),
            ruins =
                Ruins(
                    listOf(locs.water_ruins_old, locs.water_ruins_new),
                    varbits.water,
                    objs.water_talisman,
                    objs.water_tiara,
                    inside = CoordGrid(2726, 4832),
                    outside = CoordGrid(3185, 3163),
                ),
            portal = locs.water_portal,
            tiaraXp = 30.0,
        )

    val earth =
        Altar(
            name = "earth",
            altar = locs.earth_altar,
            rune = objs.earth_rune,
            level = 9,
            xp = 6.5,
            essence = EssenceKind.Any,
            multiples = listOf(9, 26, 52, 78, 104),
            ruins =
                Ruins(
                    listOf(locs.earth_ruins_old, locs.earth_ruins_new),
                    varbits.earth,
                    objs.earth_talisman,
                    objs.earth_tiara,
                    inside = CoordGrid(2655, 4830),
                    outside = CoordGrid(3306, 3472),
                ),
            portal = locs.earth_portal,
            tiaraXp = 32.5,
        )

    val fire =
        Altar(
            name = "fire",
            altar = locs.fire_altar,
            rune = objs.fire_rune,
            level = 14,
            xp = 7.0,
            essence = EssenceKind.Any,
            multiples = listOf(14, 35, 70),
            ruins =
                Ruins(
                    listOf(locs.fire_ruins_old, locs.fire_ruins_new),
                    varbits.fire,
                    objs.fire_talisman,
                    objs.fire_tiara,
                    inside = CoordGrid(2574, 4849),
                    outside = CoordGrid(3313, 3253),
                ),
            portal = locs.fire_portal,
            tiaraXp = 35.0,
        )

    val body =
        Altar(
            name = "body",
            altar = locs.body_altar,
            rune = objs.body_rune,
            level = 20,
            xp = 7.5,
            essence = EssenceKind.Any,
            multiples = listOf(20, 46, 92),
            ruins =
                Ruins(
                    listOf(locs.body_ruins_old, locs.body_ruins_new),
                    varbits.body,
                    objs.body_talisman,
                    objs.body_tiara,
                    inside = CoordGrid(2521, 4834),
                    outside = CoordGrid(3053, 3443),
                ),
            portal = locs.body_portal,
            tiaraXp = 37.5,
        )

    val cosmic =
        Altar(
            name = "cosmic",
            altar = locs.cosmic_altar,
            rune = objs.cosmic_rune,
            level = 27,
            xp = 8.0,
            essence = EssenceKind.Pure,
            multiples = listOf(27, 59),
            ruins =
                Ruins(
                    listOf(locs.cosmic_ruins_old, locs.cosmic_ruins_new),
                    varbits.cosmic,
                    objs.cosmic_talisman,
                    objs.cosmic_tiara,
                    inside = CoordGrid(2122, 4833),
                    outside = CoordGrid(2408, 4375),
                ),
            portal = locs.cosmic_portal,
            tiaraXp = 40.0,
        )

    val chaos =
        Altar(
            name = "chaos",
            altar = locs.chaos_altar,
            rune = objs.chaos_rune,
            level = 35,
            xp = 8.5,
            essence = EssenceKind.Pure,
            multiples = listOf(35, 74),
            ruins =
                Ruins(
                    listOf(locs.chaos_ruins_old, locs.chaos_ruins_new),
                    varbits.chaos,
                    objs.chaos_talisman,
                    objs.chaos_tiara,
                    inside = CoordGrid(2281, 4837),
                    outside = CoordGrid(3060, 3589),
                ),
            portal = locs.chaos_portal,
            tiaraXp = 42.5,
        )

    val astral =
        Altar(
            name = "astral",
            altar = locs.astral_altar,
            rune = objs.astral_rune,
            level = 40,
            xp = 8.7,
            essence = EssenceKind.Pure,
            multiples = listOf(40, 82),
        )

    val nature =
        Altar(
            name = "nature",
            altar = locs.nature_altar,
            rune = objs.nature_rune,
            level = 44,
            xp = 9.0,
            essence = EssenceKind.Pure,
            multiples = listOf(44, 91),
            ruins =
                Ruins(
                    listOf(locs.nature_ruins_old, locs.nature_ruins_new),
                    varbits.nature,
                    objs.nature_talisman,
                    objs.nature_tiara,
                    inside = CoordGrid(2400, 4835),
                    outside = CoordGrid(2869, 3017),
                ),
            portal = locs.nature_portal,
            tiaraXp = 45.0,
        )

    val law =
        Altar(
            name = "law",
            altar = locs.law_altar,
            rune = objs.law_rune,
            level = 54,
            xp = 9.5,
            essence = EssenceKind.Pure,
            multiples = listOf(54, 95),
            ruins =
                Ruins(
                    listOf(locs.law_ruins_old, locs.law_ruins_new),
                    varbits.law,
                    objs.law_talisman,
                    objs.law_tiara,
                    inside = CoordGrid(2464, 4818),
                    outside = CoordGrid(2858, 3379),
                ),
            portal = locs.law_portal,
            tiaraXp = 47.5,
        )

    val death =
        Altar(
            name = "death",
            altar = locs.death_altar,
            rune = objs.death_rune,
            level = 65,
            xp = 10.0,
            essence = EssenceKind.Pure,
            multiples = listOf(65, 99),
            ruins =
                Ruins(
                    listOf(locs.death_ruins_old, locs.death_ruins_new),
                    varbits.death,
                    objs.death_talisman,
                    objs.death_tiara,
                    inside = CoordGrid(2208, 4830),
                    outside = CoordGrid(1860, 4637),
                ),
            portal = locs.death_portal,
            tiaraXp = 50.0,
        )

    val blood =
        Altar(
            name = "blood",
            altar = locs.blood_altar,
            rune = objs.blood_rune,
            level = 77,
            xp = 10.5,
            essence = EssenceKind.Pure,
            multiples = listOf(77),
            ruins =
                Ruins(
                    listOf(locs.blood_ruins_old, locs.blood_ruins_new),
                    varbits.blood,
                    objs.blood_talisman,
                    objs.blood_tiara,
                    inside = CoordGrid(3225, 4832),
                    outside = CoordGrid(3561, 9779),
                ),
            portal = locs.blood_portal,
            tiaraXp = 52.5,
        )

    val wrath =
        Altar(
            name = "wrath",
            altar = locs.wrath_altar,
            rune = objs.wrath_rune,
            level = 95,
            xp = 8.0,
            essence = EssenceKind.Pure,
            multiples = listOf(95),
            ruins =
                Ruins(
                    listOf(locs.wrath_ruins_old, locs.wrath_ruins_new),
                    varbits.wrath,
                    objs.wrath_talisman,
                    objs.wrath_tiara,
                    inside = CoordGrid(2335, 4826, 1),
                    outside = CoordGrid(2446, 2823),
                ),
            portal = locs.wrath_portal,
            tiaraXp = 52.5,
        )

    /** Kourend's blood altar: 23.8 experience per dark essence fragment. */
    val kourendBlood =
        Altar(
            name = "blood",
            altar = locs.kourend_blood_altar,
            rune = objs.blood_rune,
            level = 77,
            xp = 23.8,
            essence = EssenceKind.Dark,
            multiples = listOf(77),
        )

    /** Kourend's soul altar: 29.7 experience per dark essence fragment. */
    val soul =
        Altar(
            name = "soul",
            altar = locs.soul_altar,
            rune = objs.soul_rune,
            level = 90,
            xp = 29.7,
            essence = EssenceKind.Dark,
            multiples = listOf(90),
        )

    val all: List<Altar> =
        listOf(
            air,
            mind,
            water,
            earth,
            fire,
            body,
            cosmic,
            chaos,
            astral,
            nature,
            law,
            death,
            blood,
            wrath,
            kourendBlood,
            soul,
        )

    /** The four elemental altars accept the elemental talisman and tiara. */
    val elemental: List<Altar> = listOf(air, water, earth, fire)

    /** The catalytic talisman and tiara open every other set of ruins. */
    val catalytic: List<Altar> = listOf(mind, body, cosmic, chaos, nature, law, death, blood, wrath)

    /**
     * Combination runes, per the OSRS Wiki ([Combination
     * runes](https://oldschool.runescape.wiki/w/Runecraft#Combination_runes)). Experience is per
     * rune made and is lower at the lower-levelled altar of the pair.
     */
    val combos: List<ComboRune> =
        listOf(
            ComboRune(objs.mist_rune, air.altar, objs.water_rune, objs.water_talisman, 6, 8.0),
            ComboRune(objs.mist_rune, water.altar, objs.air_rune, objs.air_talisman, 6, 8.5),
            ComboRune(objs.dust_rune, air.altar, objs.earth_rune, objs.earth_talisman, 10, 8.3),
            ComboRune(objs.dust_rune, earth.altar, objs.air_rune, objs.air_talisman, 10, 9.0),
            ComboRune(objs.mud_rune, water.altar, objs.earth_rune, objs.earth_talisman, 13, 9.3),
            ComboRune(objs.mud_rune, earth.altar, objs.water_rune, objs.water_talisman, 13, 9.5),
            ComboRune(objs.smoke_rune, air.altar, objs.fire_rune, objs.fire_talisman, 15, 8.5),
            ComboRune(objs.smoke_rune, fire.altar, objs.air_rune, objs.air_talisman, 15, 9.5),
            ComboRune(objs.steam_rune, water.altar, objs.fire_rune, objs.fire_talisman, 19, 9.3),
            ComboRune(objs.steam_rune, fire.altar, objs.water_rune, objs.water_talisman, 19, 10.0),
            ComboRune(objs.lava_rune, earth.altar, objs.fire_rune, objs.fire_talisman, 23, 10.0),
            ComboRune(objs.lava_rune, fire.altar, objs.earth_rune, objs.earth_talisman, 23, 10.5),
        )
}
