package org.runerealm.content.skills.herblore.configs

import org.runerealm.game.type.obj.ObjType

/**
 * Herblore data, per the OSRS Wiki ([Herblore](https://oldschool.runescape.wiki/w/Herblore)).
 *
 * Levels and experience come from the wiki's "Cleaning herbs & unfinished potions", "Potions",
 * "Herbed tar" tables. Finished potions are made as 3-dose potions; dose-based upgrades (stamina,
 * extended antifire, anti-venom, divine potions) keep the dose count of the base potion.
 */
object HerbloreRecipes {
    private val objs = herblore_objs

    /** A grimy herb, its clean counterpart and the requirements for cleaning it. */
    data class Herb(val grimy: ObjType, val clean: ObjType, val level: Int, val xp: Double)

    val herbs: List<Herb> =
        listOf(
            Herb(objs.grimy_guam, objs.guam, 3, 2.5),
            Herb(objs.grimy_marrentill, objs.marrentill, 5, 3.8),
            Herb(objs.grimy_tarromin, objs.tarromin, 11, 5.0),
            Herb(objs.grimy_harralander, objs.harralander, 20, 6.3),
            Herb(objs.grimy_ranarr, objs.ranarr, 25, 7.5),
            Herb(objs.grimy_toadflax, objs.toadflax, 30, 8.0),
            Herb(objs.grimy_irit, objs.irit, 40, 8.8),
            Herb(objs.grimy_avantoe, objs.avantoe, 48, 10.0),
            Herb(objs.grimy_kwuarm, objs.kwuarm, 54, 11.3),
            Herb(objs.grimy_huasca, objs.huasca, 58, 11.8),
            Herb(objs.grimy_snapdragon, objs.snapdragon, 59, 11.8),
            Herb(objs.grimy_cadantine, objs.cadantine, 65, 12.5),
            Herb(objs.grimy_lantadyme, objs.lantadyme, 67, 13.1),
            Herb(objs.grimy_dwarf_weed, objs.dwarf_weed, 70, 13.8),
            Herb(objs.grimy_torstol, objs.torstol, 75, 15.0),
        )

    /* Potion series, used for recipes, combining doses and decanting. */

    val attack = series("attack", objs.attack4, objs.attack3, objs.attack2, objs.attack1)
    val antipoison =
        series("antipoison", objs.antipoison4, objs.antipoison3, objs.antipoison2, objs.antipoison1)
    val strength =
        series("strength", objs.strength4, objs.strength3, objs.strength2, objs.strength1)
    val serum = series("serum 207", objs.serum4, objs.serum3, objs.serum2, objs.serum1)
    val compost = series("compost", objs.compost4, objs.compost3, objs.compost2, objs.compost1)
    val restore = series("restore", objs.restore4, objs.restore3, objs.restore2, objs.restore1)
    val energy = series("energy", objs.energy4, objs.energy3, objs.energy2, objs.energy1)
    val defence = series("defence", objs.defence4, objs.defence3, objs.defence2, objs.defence1)
    val agility = series("agility", objs.agility4, objs.agility3, objs.agility2, objs.agility1)
    val combat = series("combat", objs.combat4, objs.combat3, objs.combat2, objs.combat1)
    val prayer = series("prayer", objs.prayer4, objs.prayer3, objs.prayer2, objs.prayer1)
    val superAttack =
        series(
            "super attack",
            objs.super_attack4,
            objs.super_attack3,
            objs.super_attack2,
            objs.super_attack1,
        )
    val superantipoison =
        series(
            "superantipoison",
            objs.superantipoison4,
            objs.superantipoison3,
            objs.superantipoison2,
            objs.superantipoison1,
        )
    val fishing = series("fishing", objs.fishing4, objs.fishing3, objs.fishing2, objs.fishing1)
    val superEnergy =
        series(
            "super energy",
            objs.super_energy4,
            objs.super_energy3,
            objs.super_energy2,
            objs.super_energy1,
        )
    val hunter = series("hunter", objs.hunter4, objs.hunter3, objs.hunter2, objs.hunter1)
    val goading = series("goading", objs.goading4, objs.goading3, objs.goading2, objs.goading1)
    val superStrength =
        series(
            "super strength",
            objs.super_strength4,
            objs.super_strength3,
            objs.super_strength2,
            objs.super_strength1,
        )
    val prayerRegen =
        series(
            "prayer regeneration",
            objs.prayer_regen4,
            objs.prayer_regen3,
            objs.prayer_regen2,
            objs.prayer_regen1,
        )
    val superRestore =
        series(
            "super restore",
            objs.super_restore4,
            objs.super_restore3,
            objs.super_restore2,
            objs.super_restore1,
        )
    val superDefence =
        series(
            "super defence",
            objs.super_defence4,
            objs.super_defence3,
            objs.super_defence2,
            objs.super_defence1,
        )
    val antidotePlus =
        series(
            "antidote+",
            objs.antidote_plus4,
            objs.antidote_plus3,
            objs.antidote_plus2,
            objs.antidote_plus1,
        )
    val antifire =
        series("antifire", objs.antifire4, objs.antifire3, objs.antifire2, objs.antifire1)
    val ranging = series("ranging", objs.ranging4, objs.ranging3, objs.ranging2, objs.ranging1)
    val magic = series("magic", objs.magic4, objs.magic3, objs.magic2, objs.magic1)
    val stamina = series("stamina", objs.stamina4, objs.stamina3, objs.stamina2, objs.stamina1)
    val zamorakBrew =
        series(
            "zamorak brew",
            objs.zamorak_brew4,
            objs.zamorak_brew3,
            objs.zamorak_brew2,
            objs.zamorak_brew1,
        )
    val antidotePlusPlus =
        series(
            "antidote++",
            objs.antidote_plus_plus4,
            objs.antidote_plus_plus3,
            objs.antidote_plus_plus2,
            objs.antidote_plus_plus1,
        )
    val bastion = series("bastion", objs.bastion4, objs.bastion3, objs.bastion2, objs.bastion1)
    val battlemage =
        series("battlemage", objs.battlemage4, objs.battlemage3, objs.battlemage2, objs.battlemage1)
    val saradominBrew =
        series(
            "saradomin brew",
            objs.saradomin_brew4,
            objs.saradomin_brew3,
            objs.saradomin_brew2,
            objs.saradomin_brew1,
        )
    val surge = series("surge", objs.surge4, objs.surge3, objs.surge2, objs.surge1)
    val extendedAntifire =
        series(
            "extended antifire",
            objs.extended_antifire4,
            objs.extended_antifire3,
            objs.extended_antifire2,
            objs.extended_antifire1,
        )
    val ancientBrew =
        series(
            "ancient brew",
            objs.ancient_brew4,
            objs.ancient_brew3,
            objs.ancient_brew2,
            objs.ancient_brew1,
        )
    val antiVenom =
        series("anti-venom", objs.anti_venom4, objs.anti_venom3, objs.anti_venom2, objs.anti_venom1)
    val menaphiteRemedy =
        series(
            "menaphite remedy",
            objs.menaphite_remedy4,
            objs.menaphite_remedy3,
            objs.menaphite_remedy2,
            objs.menaphite_remedy1,
        )
    val superCombat =
        series(
            "super combat",
            objs.super_combat4,
            objs.super_combat3,
            objs.super_combat2,
            objs.super_combat1,
        )
    val superAntifire =
        series(
            "super antifire",
            objs.super_antifire4,
            objs.super_antifire3,
            objs.super_antifire2,
            objs.super_antifire1,
        )
    val antiVenomPlus =
        series(
            "anti-venom+",
            objs.anti_venom_plus4,
            objs.anti_venom_plus3,
            objs.anti_venom_plus2,
            objs.anti_venom_plus1,
        )
    val extendedSuperAntifire =
        series(
            "extended super antifire",
            objs.extended_super_antifire4,
            objs.extended_super_antifire3,
            objs.extended_super_antifire2,
            objs.extended_super_antifire1,
        )
    val divineSuperAttack =
        series(
            "divine super attack",
            objs.divine_super_attack4,
            objs.divine_super_attack3,
            objs.divine_super_attack2,
            objs.divine_super_attack1,
        )
    val divineSuperStrength =
        series(
            "divine super strength",
            objs.divine_super_strength4,
            objs.divine_super_strength3,
            objs.divine_super_strength2,
            objs.divine_super_strength1,
        )
    val divineSuperDefence =
        series(
            "divine super defence",
            objs.divine_super_defence4,
            objs.divine_super_defence3,
            objs.divine_super_defence2,
            objs.divine_super_defence1,
        )
    val divineRanging =
        series(
            "divine ranging",
            objs.divine_ranging4,
            objs.divine_ranging3,
            objs.divine_ranging2,
            objs.divine_ranging1,
        )
    val divineMagic =
        series(
            "divine magic",
            objs.divine_magic4,
            objs.divine_magic3,
            objs.divine_magic2,
            objs.divine_magic1,
        )
    val divineSuperCombat =
        series(
            "divine super combat",
            objs.divine_super_combat4,
            objs.divine_super_combat3,
            objs.divine_super_combat2,
            objs.divine_super_combat1,
        )
    val divineBastion =
        series(
            "divine bastion",
            objs.divine_bastion4,
            objs.divine_bastion3,
            objs.divine_bastion2,
            objs.divine_bastion1,
        )
    val divineBattlemage =
        series(
            "divine battlemage",
            objs.divine_battlemage4,
            objs.divine_battlemage3,
            objs.divine_battlemage2,
            objs.divine_battlemage1,
        )

    /** Every potion series that can be combined by dose and decanted. */
    val potionSeries: List<PotionSeries> =
        listOf(
            attack,
            antipoison,
            strength,
            serum,
            compost,
            restore,
            energy,
            defence,
            agility,
            combat,
            prayer,
            superAttack,
            superantipoison,
            fishing,
            superEnergy,
            hunter,
            goading,
            superStrength,
            prayerRegen,
            superRestore,
            superDefence,
            antidotePlus,
            antifire,
            ranging,
            magic,
            stamina,
            zamorakBrew,
            antidotePlusPlus,
            bastion,
            battlemage,
            saradominBrew,
            surge,
            extendedAntifire,
            ancientBrew,
            antiVenom,
            menaphiteRemedy,
            superCombat,
            superAntifire,
            antiVenomPlus,
            extendedSuperAntifire,
            divineSuperAttack,
            divineSuperStrength,
            divineSuperDefence,
            divineRanging,
            divineMagic,
            divineSuperCombat,
            divineBastion,
            divineBattlemage,
        )

    /* Unfinished potions: a herb (or other primary) added to a vial of water, milk or blood. */

    private val unfinished: List<HerbCombo> =
        listOf(
            unf(objs.vial_water, objs.guam, objs.guam_unf, 3),
            unf(objs.vial_water, objs.marrentill, objs.marrentill_unf, 5),
            unf(objs.vial_water, objs.tarromin, objs.tarromin_unf, 12),
            unf(objs.vial_water, objs.harralander, objs.harralander_unf, 22),
            unf(objs.vial_water, objs.ranarr, objs.ranarr_unf, 30),
            unf(objs.vial_water, objs.toadflax, objs.toadflax_unf, 34),
            unf(objs.vial_water, objs.irit, objs.irit_unf, 45),
            unf(objs.vial_water, objs.avantoe, objs.avantoe_unf, 50),
            unf(objs.vial_water, objs.kwuarm, objs.kwuarm_unf, 55),
            unf(objs.vial_water, objs.huasca, objs.huasca_unf, 58),
            unf(objs.vial_water, objs.snapdragon, objs.snapdragon_unf, 63),
            unf(objs.vial_water, objs.cadantine, objs.cadantine_unf, 66),
            unf(objs.vial_water, objs.lantadyme, objs.lantadyme_unf, 69),
            unf(objs.vial_water, objs.dwarf_weed, objs.dwarf_weed_unf, 72),
            unf(objs.vial_water, objs.torstol, objs.torstol_unf, 78),
            unf(objs.coconut_milk, objs.toadflax, objs.antidote_plus_unf, 68, "coconut milk"),
            unf(
                objs.coconut_milk,
                objs.cactus_spine,
                objs.weapon_poison_plus_unf,
                73,
                "coconut milk",
            ),
            unf(objs.coconut_milk, objs.irit, objs.antidote_plus_plus_unf, 79, "coconut milk"),
            unf(objs.vial_blood, objs.cadantine, objs.cadantine_blood_unf, 80, "vial of blood"),
            unf(
                objs.coconut_milk,
                objs.cave_nightshade,
                objs.weapon_poison_plus_plus_unf,
                82,
                "coconut milk",
            ),
        )

    /* Finished potions: a secondary added to an unfinished potion makes a 3-dose potion. */

    private val finished: List<HerbCombo> =
        listOf(
            mix(objs.guam_unf, objs.eye_of_newt, objs.attack3, 3, 25.0),
            mix(objs.marrentill_unf, objs.unicorn_horn_dust, objs.antipoison3, 5, 37.5),
            mix(objs.tarromin_unf, objs.limpwurt_root, objs.strength3, 12, 50.0),
            mix(objs.tarromin_unf, objs.ashes, objs.serum3, 15, 50.0),
            mix(objs.harralander_unf, objs.volcanic_ash, objs.compost3, 22, 60.0),
            mix(objs.harralander_unf, objs.red_spiders_eggs, objs.restore3, 22, 62.5),
            mix(objs.harralander_unf, objs.chocolate_dust, objs.energy3, 26, 67.5),
            mix(objs.ranarr_unf, objs.white_berries, objs.defence3, 30, 75.0),
            mix(objs.toadflax_unf, objs.toads_legs, objs.agility3, 34, 80.0),
            mix(objs.harralander_unf, objs.goat_horn_dust, objs.combat3, 36, 84.0),
            mix(objs.ranarr_unf, objs.snape_grass, objs.prayer3, 38, 87.5),
            mix(objs.irit_unf, objs.eye_of_newt, objs.super_attack3, 45, 100.0),
            mix(objs.irit_unf, objs.unicorn_horn_dust, objs.superantipoison3, 48, 106.3),
            mix(objs.avantoe_unf, objs.snape_grass, objs.fishing3, 50, 112.5),
            mix(objs.avantoe_unf, objs.mort_myre_fungus, objs.super_energy3, 52, 117.5),
            mix(objs.avantoe_unf, objs.kebbit_teeth_dust, objs.hunter3, 53, 120.0),
            mix(objs.harralander_unf, objs.aldarium, objs.goading3, 54, 132.0),
            mix(objs.kwuarm_unf, objs.limpwurt_root, objs.super_strength3, 55, 125.0),
            mix(objs.huasca_unf, objs.aldarium, objs.prayer_regen3, 58, 132.0),
            mix(objs.kwuarm_unf, objs.dragon_scale_dust, objs.weapon_poison, 60, 137.5),
            mix(objs.snapdragon_unf, objs.red_spiders_eggs, objs.super_restore3, 63, 142.5),
            mix(objs.cadantine_unf, objs.white_berries, objs.super_defence3, 66, 150.0),
            mix(objs.antidote_plus_unf, objs.yew_roots, objs.antidote_plus3, 68, 155.0),
            mix(objs.lantadyme_unf, objs.dragon_scale_dust, objs.antifire3, 69, 157.5),
            mix(objs.dwarf_weed_unf, objs.wine_of_zamorak, objs.ranging3, 72, 162.5),
            mix(
                objs.weapon_poison_plus_unf,
                objs.red_spiders_eggs,
                objs.weapon_poison_plus,
                73,
                190.0,
            ),
            mix(objs.lantadyme_unf, objs.potato_cactus, objs.magic3, 76, 172.5),
            mix(objs.torstol_unf, objs.jangerberries, objs.zamorak_brew3, 78, 175.0),
            mix(objs.antidote_plus_plus_unf, objs.magic_roots, objs.antidote_plus_plus3, 79, 177.5),
            mix(objs.cadantine_blood_unf, objs.wine_of_zamorak, objs.bastion3, 80, 155.0),
            mix(objs.cadantine_blood_unf, objs.potato_cactus, objs.battlemage3, 80, 155.0),
            mix(objs.toadflax_unf, objs.crushed_nest, objs.saradomin_brew3, 81, 180.0),
            mix(objs.torstol_unf, objs.demonic_tallow, objs.surge3, 81, 185.0),
            mix(
                objs.weapon_poison_plus_plus_unf,
                objs.poison_ivy_berries,
                objs.weapon_poison_plus_plus,
                82,
                190.0,
            ),
            mix(objs.dwarf_weed_unf, objs.nihil_dust, objs.ancient_brew3, 85, 190.0),
            mix(objs.dwarf_weed_unf, objs.lily_of_the_sands, objs.menaphite_remedy3, 88, 200.0),
        )

    /*
     * Dose-based upgrades: `perDose` secondaries per dose of the base potion, keeping the dose
     * count. Experience is per dose. Super antifire and anti-venom+ need a 4-dose base potion.
     */

    private val superCombatRecipe =
        HerbRecipe(
            product = objs.super_combat4,
            inputs =
                listOf(
                    Ingredient(objs.torstol),
                    Ingredient(objs.super_attack4),
                    Ingredient(objs.super_strength4),
                    Ingredient(objs.super_defence4),
                ),
            level = 90,
            xp = 150.0,
            byproduct = objs.vial,
            byproductCount = 2,
            productCount = 1,
            anim = seqs.mix,
            ticks = MIX_TICKS,
            message = "You mix the torstol into your potions.",
        )

    private val upgrades: List<HerbCombo> =
        upgrade(superEnergy, objs.amylase_crystal, stamina, 77, 25.5) +
            upgrade(antifire, objs.lava_scale_shard, extendedAntifire, 84, 27.5) +
            upgrade(antidotePlusPlus, objs.zulrahs_scales, antiVenom, 87, 30.0, perDose = 5) +
            upgrade(superAntifire, objs.lava_scale_shard, extendedSuperAntifire, 98, 40.0) +
            upgrade(superAttack, objs.crystal_dust, divineSuperAttack, 70, 0.5) +
            upgrade(superStrength, objs.crystal_dust, divineSuperStrength, 70, 0.5) +
            upgrade(superDefence, objs.crystal_dust, divineSuperDefence, 70, 0.5) +
            upgrade(ranging, objs.crystal_dust, divineRanging, 74, 0.5) +
            upgrade(magic, objs.crystal_dust, divineMagic, 78, 0.5) +
            upgrade(bastion, objs.crystal_dust, divineBastion, 86, 0.5) +
            upgrade(battlemage, objs.crystal_dust, divineBattlemage, 86, 0.5) +
            upgrade(superCombat, objs.crystal_dust, divineSuperCombat, 97, 0.5) +
            listOf(
                upgrade4(antifire, objs.crushed_superior_dragon_bones, superAntifire, 92, 130.0),
                upgrade4(antiVenom, objs.torstol, antiVenomPlus, 94, 125.0),
                HerbCombo(objs.torstol, objs.super_attack4, listOf(superCombatRecipe)),
                HerbCombo(objs.torstol, objs.super_strength4, listOf(superCombatRecipe)),
                HerbCombo(objs.torstol, objs.super_defence4, listOf(superCombatRecipe)),
            )

    /* Swamp tar: a clean herb and 15 swamp tar, with a pestle and mortar, make 15 tar. */

    private val tars: List<HerbCombo> =
        listOf(
            tar(objs.guam, objs.guam_tar, 19, 30.0),
            tar(objs.marrentill, objs.marrentill_tar, 31, 42.5),
            tar(objs.tarromin, objs.tarromin_tar, 39, 55.0),
            tar(objs.harralander, objs.harralander_tar, 44, 72.5),
            tar(objs.irit, objs.irit_tar, 55, 85.0),
        )

    /* Crushing with a pestle and mortar (no experience; repeats over every carried item). */

    private val crushing: List<HerbCombo> =
        listOf(
            crush(objs.unicorn_horn, objs.unicorn_horn_dust),
            crush(objs.blue_dragon_scale, objs.dragon_scale_dust),
            crush(objs.chocolate_bar, objs.chocolate_dust),
            crush(objs.bird_nest, objs.crushed_nest),
            crush(objs.desert_goat_horn, objs.goat_horn_dust),
            crush(objs.kebbit_teeth, objs.kebbit_teeth_dust),
            crush(objs.superior_dragon_bones, objs.crushed_superior_dragon_bones),
            crush(objs.lava_scale, objs.lava_scale_shard, count = 3, countMax = 6),
            crush(objs.nihil_shard, objs.nihil_dust),
            crush(objs.gorak_claws, objs.gorak_claw_powder),
        )

    /** Every obj-on-obj Herblore combination, grouped by pair. */
    val combos: List<HerbCombo> =
        (unfinished + finished + upgrades + tars + crushing)
            .groupBy { it.first.id to it.second.id }
            .map { (_, group) -> group.first().copy(recipes = group.flatMap { it.recipes }) }
}

private const val MIX_TICKS = 2
private const val TAR_TICKS = 3
private const val CRUSH_TICKS = 3
const val TAR_AMOUNT = 15

private val objs = herblore_objs
private val seqs = herblore_seqs

private fun series(name: String, vararg doses: ObjType) = PotionSeries(name, doses.toList())

private fun unf(
    base: ObjType,
    primary: ObjType,
    product: ObjType,
    level: Int,
    baseName: String = "vial of water",
) =
    HerbCombo(
        primary,
        base,
        listOf(
            HerbRecipe(
                product = product,
                inputs = listOf(Ingredient(primary), Ingredient(base)),
                level = level,
                xp = 0.0,
                anim = seqs.mix,
                ticks = MIX_TICKS,
                message = "You put the {input} into the $baseName.",
            )
        ),
    )

private fun mix(unf: ObjType, secondary: ObjType, product: ObjType, level: Int, xp: Double) =
    HerbCombo(
        secondary,
        unf,
        listOf(
            HerbRecipe(
                product = product,
                inputs = listOf(Ingredient(secondary), Ingredient(unf)),
                level = level,
                xp = xp,
                anim = seqs.mix,
                ticks = MIX_TICKS,
                message = "You mix the {input} into your potion.",
            )
        ),
    )

private fun upgrade(
    base: PotionSeries,
    secondary: ObjType,
    product: PotionSeries,
    level: Int,
    xpPerDose: Double,
    perDose: Int = 1,
): List<HerbCombo> =
    (1..base.maxDoses).map { doses ->
        HerbCombo(
            secondary,
            base.dose(doses),
            listOf(
                HerbRecipe(
                    product = product.dose(doses),
                    inputs =
                        listOf(
                            Ingredient(secondary, perDose * doses),
                            Ingredient(base.dose(doses)),
                        ),
                    level = level,
                    xp = xpPerDose * doses,
                    anim = seqs.mix,
                    ticks = MIX_TICKS,
                    message = "You mix the {input} into your potion.",
                )
            ),
        )
    }

/** A single 4-dose potion upgraded with one secondary (super antifire, anti-venom+). */
private fun upgrade4(
    base: PotionSeries,
    secondary: ObjType,
    product: PotionSeries,
    level: Int,
    xp: Double,
) =
    HerbCombo(
        secondary,
        base.dose(4),
        listOf(
            HerbRecipe(
                product = product.dose(4),
                inputs = listOf(Ingredient(secondary), Ingredient(base.dose(4))),
                level = level,
                xp = xp,
                anim = seqs.mix,
                ticks = MIX_TICKS,
                message = "You mix the {input} into your potion.",
            )
        ),
    )

private fun tar(herb: ObjType, product: ObjType, level: Int, xp: Double) =
    HerbCombo(
        herb,
        objs.swamp_tar,
        listOf(
            HerbRecipe(
                product = product,
                inputs = listOf(Ingredient(herb), Ingredient(objs.swamp_tar, TAR_AMOUNT)),
                level = level,
                xp = xp,
                productCount = TAR_AMOUNT,
                tools = listOf(objs.pestle_and_mortar),
                anim = seqs.grind,
                ticks = TAR_TICKS,
                message = "You add the {input} to the swamp tar.",
            )
        ),
    )

private fun crush(input: ObjType, product: ObjType, count: Int = 1, countMax: Int = count) =
    HerbCombo(
        objs.pestle_and_mortar,
        input,
        listOf(
            HerbRecipe(
                product = product,
                inputs = listOf(Ingredient(input)),
                level = 1,
                xp = 0.0,
                productCount = count,
                productCountMax = countMax,
                tools = listOf(objs.pestle_and_mortar),
                anim = seqs.grind,
                auto = true,
                ticks = CRUSH_TICKS,
                message = "You grind the {input} into {product}.",
            )
        ),
    )
