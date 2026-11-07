package org.runerealm.content.areas.city.lumbridge

import kotlin.reflect.KClass
import org.runerealm.content.areas.city.lumbridge.npcs.Abigaila
import org.runerealm.content.areas.city.lumbridge.npcs.AbigaleAndHewey
import org.runerealm.content.areas.city.lumbridge.npcs.ArthurTheClueHunter
import org.runerealm.content.areas.city.lumbridge.npcs.BarfyBill
import org.runerealm.content.areas.city.lumbridge.npcs.Bartender
import org.runerealm.content.areas.city.lumbridge.npcs.Bob
import org.runerealm.content.areas.city.lumbridge.npcs.CandleSeller
import org.runerealm.content.areas.city.lumbridge.npcs.Cook
import org.runerealm.content.areas.city.lumbridge.npcs.CookingTutor
import org.runerealm.content.areas.city.lumbridge.npcs.CountCheck
import org.runerealm.content.areas.city.lumbridge.npcs.CraftingTutor
import org.runerealm.content.areas.city.lumbridge.npcs.DonieAndGee
import org.runerealm.content.areas.city.lumbridge.npcs.Doomsayer
import org.runerealm.content.areas.city.lumbridge.npcs.DukeHoracio
import org.runerealm.content.areas.city.lumbridge.npcs.FatherAereck
import org.runerealm.content.areas.city.lumbridge.npcs.FatherUrhney
import org.runerealm.content.areas.city.lumbridge.npcs.Fayeth
import org.runerealm.content.areas.city.lumbridge.npcs.FishingTutor
import org.runerealm.content.areas.city.lumbridge.npcs.FredTheFarmer
import org.runerealm.content.areas.city.lumbridge.npcs.GeneralStore
import org.runerealm.content.areas.city.lumbridge.npcs.GillieGroats
import org.runerealm.content.areas.city.lumbridge.npcs.Hans
import org.runerealm.content.areas.city.lumbridge.npcs.HatiusCosaintus
import org.runerealm.content.areas.city.lumbridge.npcs.Larry
import org.runerealm.content.areas.city.lumbridge.npcs.LostCityArcher
import org.runerealm.content.areas.city.lumbridge.npcs.LostCityMonk
import org.runerealm.content.areas.city.lumbridge.npcs.LostCityWarrior
import org.runerealm.content.areas.city.lumbridge.npcs.LostCityWizard
import org.runerealm.content.areas.city.lumbridge.npcs.LumbridgeGuide
import org.runerealm.content.areas.city.lumbridge.npcs.MagicCombatTutor
import org.runerealm.content.areas.city.lumbridge.npcs.MeleeCombatTutor
import org.runerealm.content.areas.city.lumbridge.npcs.MillieMiller
import org.runerealm.content.areas.city.lumbridge.npcs.MiningTutor
import org.runerealm.content.areas.city.lumbridge.npcs.Nigel
import org.runerealm.content.areas.city.lumbridge.npcs.PenguinSheep
import org.runerealm.content.areas.city.lumbridge.npcs.Perdu
import org.runerealm.content.areas.city.lumbridge.npcs.PrayerTutor
import org.runerealm.content.areas.city.lumbridge.npcs.RangedCombatTutor
import org.runerealm.content.areas.city.lumbridge.npcs.SethGroats
import org.runerealm.content.areas.city.lumbridge.npcs.Sigmund
import org.runerealm.content.areas.city.lumbridge.npcs.SmithingApprentice
import org.runerealm.content.areas.city.lumbridge.npcs.ToolLeprechaun
import org.runerealm.content.areas.city.lumbridge.npcs.Vasquen
import org.runerealm.content.areas.city.lumbridge.npcs.Veos
import org.runerealm.content.areas.city.lumbridge.npcs.WoodsmanTutor
import org.runerealm.plugin.scripts.PluginScript

/** Every Lumbridge npc script. */
internal val LUMBRIDGE_NPC_SCRIPTS: Array<KClass<out PluginScript>> =
    arrayOf(
        Abigaila::class,
        AbigaleAndHewey::class,
        ArthurTheClueHunter::class,
        BarfyBill::class,
        Bartender::class,
        Bob::class,
        CandleSeller::class,
        Cook::class,
        CookingTutor::class,
        CountCheck::class,
        CraftingTutor::class,
        DonieAndGee::class,
        Doomsayer::class,
        DukeHoracio::class,
        FatherAereck::class,
        FatherUrhney::class,
        Fayeth::class,
        FishingTutor::class,
        FredTheFarmer::class,
        GeneralStore::class,
        GillieGroats::class,
        Hans::class,
        HatiusCosaintus::class,
        Larry::class,
        LostCityArcher::class,
        LostCityMonk::class,
        LostCityWarrior::class,
        LostCityWizard::class,
        LumbridgeGuide::class,
        MagicCombatTutor::class,
        MeleeCombatTutor::class,
        MillieMiller::class,
        MiningTutor::class,
        Nigel::class,
        PenguinSheep::class,
        Perdu::class,
        PrayerTutor::class,
        RangedCombatTutor::class,
        SethGroats::class,
        Sigmund::class,
        SmithingApprentice::class,
        ToolLeprechaun::class,
        Vasquen::class,
        Veos::class,
        WoodsmanTutor::class,
    )
