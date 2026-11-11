package org.runerealm.content.skills.farming.configs

import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.api.type.refs.npc.NpcReferences
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.timer.TimerReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.api.type.refs.varp.VarpReferences

typealias farming_objs = FarmingObjs

typealias farming_locs = FarmingLocs

typealias farming_varps = FarmingVarps

typealias farming_varbits = FarmingVarBits

typealias farming_seqs = FarmingSeqs

typealias farming_timers = FarmingTimers

typealias farming_npcs = FarmingNpcs

object FarmingObjs : ObjReferences() {
    val rake = find("rake")
    val dibber = find("dibber")
    val spade = find("spade")
    val gardening_trowel = find("gardening_trowel")
    val secateurs = find("secateurs")
    val magic_secateurs = find("fairy_enchanted_secateurs")
    val plant_cure = find("plant_cure")
    val vial_empty = find("vial_empty")
    val bucket_empty = find("bucket_empty")
    val bucket_compost = find("bucket_compost")
    val bucket_supercompost = find("bucket_supercompost")
    val bucket_ultracompost = find("bucket_ultracompost")
    val weeds = find("weeds")
    val plantpot_empty = find("plantpot_empty")
    val plantpot_filled = find("plantpot_compost")

    val watering_can_0 = find("watering_can_0")
    val watering_can_1 = find("watering_can_1")
    val watering_can_2 = find("watering_can_2")
    val watering_can_3 = find("watering_can_3")
    val watering_can_4 = find("watering_can_4")
    val watering_can_5 = find("watering_can_5")
    val watering_can_6 = find("watering_can_6")
    val watering_can_7 = find("watering_can_7")
    val watering_can_8 = find("watering_can_8")

    // Allotment seeds and produce.
    val potato_seed = find("potato_seed")
    val onion_seed = find("onion_seed")
    val cabbage_seed = find("cabbage_seed")
    val tomato_seed = find("tomato_seed")
    val sweetcorn_seed = find("sweetcorn_seed")
    val strawberry_seed = find("strawberry_seed")
    val watermelon_seed = find("watermelon_seed")
    val potato = find("potato")
    val onion = find("onion")
    val cabbage = find("cabbage")
    val tomato = find("tomato")
    val sweetcorn = find("sweetcorn")
    val strawberry = find("strawberry")
    val watermelon = find("watermelon")

    // Herb seeds and grimy herbs.
    val guam_seed = find("guam_seed")
    val marrentill_seed = find("marrentill_seed")
    val tarromin_seed = find("tarromin_seed")
    val harralander_seed = find("harralander_seed")
    val ranarr_seed = find("ranarr_seed")
    val toadflax_seed = find("toadflax_seed")
    val irit_seed = find("irit_seed")
    val avantoe_seed = find("avantoe_seed")
    val kwuarm_seed = find("kwuarm_seed")
    val snapdragon_seed = find("snapdragon_seed")
    val cadantine_seed = find("cadantine_seed")
    val lantadyme_seed = find("lantadyme_seed")
    val dwarf_weed_seed = find("dwarf_weed_seed")
    val torstol_seed = find("torstol_seed")
    val grimy_guam = find("unidentified_guam")
    val grimy_marrentill = find("unidentified_marentill")
    val grimy_tarromin = find("unidentified_tarromin")
    val grimy_harralander = find("unidentified_harralander")
    val grimy_ranarr = find("unidentified_ranarr")
    val grimy_toadflax = find("unidentified_toadflax")
    val grimy_irit = find("unidentified_irit")
    val grimy_avantoe = find("unidentified_avantoe")
    val grimy_kwuarm = find("unidentified_kwuarm")
    val grimy_snapdragon = find("unidentified_snapdragon")
    val grimy_cadantine = find("unidentified_cadantine")
    val grimy_lantadyme = find("unidentified_lantadyme")
    val grimy_dwarf_weed = find("unidentified_dwarf_weed")
    val grimy_torstol = find("unidentified_torstol")

    // Flower seeds and flowers.
    val marigold_seed = find("marigold_seed")
    val rosemary_seed = find("rosemary_seed")
    val nasturtium_seed = find("nasturtium_seed")
    val woad_seed = find("woad_seed")
    val limpwurt_seed = find("limpwurt_seed")
    val white_lily_seed = find("white_lily_seed")
    val marigolds = find("marigold")
    val rosemary = find("rosemary")
    val nasturtiums = find("nasturtium")
    val woad_leaf = find("woadleaf")
    val limpwurt_root = find("limpwurt_root")
    val white_lily = find("whitelilly")

    // Hops seeds and produce.
    val barley_seed = find("barley_seed")
    val hammerstone_seed = find("hammerstone_hop_seed")
    val asgarnian_seed = find("asgarnian_hop_seed")
    val jute_seed = find("jute_seed")
    val yanillian_seed = find("yanillian_hop_seed")
    val krandorian_seed = find("krandorian_hop_seed")
    val wildblood_seed = find("wildblood_hop_seed")
    val barley = find("barley")
    val hammerstone_hops = find("hammerstone_hops")
    val asgarnian_hops = find("asgarnian_hops")
    val jute_fibre = find("jute_fibre")
    val yanillian_hops = find("yanillian_hops")
    val krandorian_hops = find("krandorian_hops")
    val wildblood_hops = find("wildblood_hops")

    // Tree seeds, seedlings and saplings.
    val acorn = find("acorn")
    val willow_seed = find("willow_seed")
    val maple_seed = find("maple_seed")
    val yew_seed = find("yew_seed")
    val magic_seed = find("magic_tree_seed")
    val oak_seedling = find("plantpot_acorn")
    val willow_seedling = find("plantpot_willow_seed")
    val maple_seedling = find("plantpot_maple_seed")
    val yew_seedling = find("plantpot_yew_seed")
    val magic_seedling = find("plantpot_magic_tree_seed")
    val oak_seedling_watered = find("plantpot_acorn_watered")
    val willow_seedling_watered = find("plantpot_willow_seed_watered")
    val maple_seedling_watered = find("plantpot_maple_seed_watered")
    val yew_seedling_watered = find("plantpot_yew_seed_watered")
    val magic_seedling_watered = find("plantpot_magic_tree_seed_watered")
    val oak_sapling = find("plantpot_oak_sapling")
    val willow_sapling = find("plantpot_willow_sapling")
    val maple_sapling = find("plantpot_maple_sapling")
    val yew_sapling = find("plantpot_yew_sapling")
    val magic_sapling = find("plantpot_magic_tree_sapling")
    val oak_logs = find("oak_logs")
    val willow_logs = find("willow_logs")
    val maple_logs = find("maple_logs")
    val yew_logs = find("yew_logs")
    val magic_logs = find("magic_logs")
    val oak_roots = find("oak_roots")
    val willow_roots = find("willow_roots")
    val maple_roots = find("maple_roots")
    val yew_roots = find("yew_roots")
    val magic_roots = find("magic_roots")

    // Fruit tree seeds, seedlings and saplings.
    val apple_seed = find("apple_tree_seed")
    val banana_seed = find("banana_tree_seed")
    val orange_seed = find("orange_tree_seed")
    val curry_seed = find("curry_tree_seed")
    val pineapple_seed = find("pineapple_tree_seed")
    val papaya_seed = find("papaya_tree_seed")
    val palm_seed = find("palm_tree_seed")
    val dragonfruit_seed = find("dragonfruit_tree_seed")
    val apple_seedling = find("plantpot_apple_seed")
    val banana_seedling = find("plantpot_banana_seed")
    val orange_seedling = find("plantpot_orange_seed")
    val curry_seedling = find("plantpot_curry_seed")
    val pineapple_seedling = find("plantpot_pineapple_seed")
    val papaya_seedling = find("plantpot_papaya_seed")
    val palm_seedling = find("plantpot_palm_seed")
    val dragonfruit_seedling = find("plantpot_dragonfruit_seed")
    val apple_seedling_watered = find("plantpot_apple_seed_watered")
    val banana_seedling_watered = find("plantpot_banana_seed_watered")
    val orange_seedling_watered = find("plantpot_orange_seed_watered")
    val curry_seedling_watered = find("plantpot_curry_seed_watered")
    val pineapple_seedling_watered = find("plantpot_pineapple_seed_watered")
    val papaya_seedling_watered = find("plantpot_papaya_seed_watered")
    val palm_seedling_watered = find("plantpot_palm_seed_watered")
    val dragonfruit_seedling_watered = find("plantpot_dragonfruit_seed_watered")
    val apple_sapling = find("plantpot_apple_sapling")
    val banana_sapling = find("plantpot_banana_sapling")
    val orange_sapling = find("plantpot_orange_sapling")
    val curry_sapling = find("plantpot_curry_sapling")
    val pineapple_sapling = find("plantpot_pineapple_sapling")
    val papaya_sapling = find("plantpot_papaya_sapling")
    val palm_sapling = find("plantpot_palm_sapling")
    val dragonfruit_sapling = find("plantpot_dragonfruit_sapling")
    val cooking_apple = find("cooking_apple")
    val banana = find("banana")
    val orange = find("orange")
    val curry_leaf = find("curry_leaf")
    val pineapple = find("pineapple")
    val papaya = find("papaya")
    val coconut = find("coconut")
    val dragonfruit = find("dragonfruit")
}

object FarmingLocs : LocReferences() {
    val falador_allotment_nw = find("farming_veg_patch_1")
    val falador_allotment_se = find("farming_veg_patch_2")
    val catherby_allotment_n = find("farming_veg_patch_3")
    val catherby_allotment_s = find("farming_veg_patch_4")
    val ardougne_allotment_n = find("farming_veg_patch_5")
    val ardougne_allotment_s = find("farming_veg_patch_6")
    val morytania_allotment_nw = find("farming_veg_patch_7")
    val morytania_allotment_se = find("farming_veg_patch_8")

    val falador_herb = find("farming_herb_patch_1")
    val catherby_herb = find("farming_herb_patch_2")
    val ardougne_herb = find("farming_herb_patch_3")
    val morytania_herb = find("farming_herb_patch_4")

    val falador_flower = find("farming_flower_patch_1")
    val catherby_flower = find("farming_flower_patch_2")
    val ardougne_flower = find("farming_flower_patch_3")
    val morytania_flower = find("farming_flower_patch_4")

    val taverley_tree = find("farming_tree_patch_1")
    val falador_tree = find("farming_tree_patch_2")
    val varrock_tree = find("farming_tree_patch_3")
    val lumbridge_tree = find("farming_tree_patch_4")
    val gnome_tree = find("farming_tree_patch_5")

    val gnome_fruit_tree = find("farming_fruit_tree_patch_1")
    val village_fruit_tree = find("farming_fruit_tree_patch_2")
    val brimhaven_fruit_tree = find("farming_fruit_tree_patch_3")
    val catherby_fruit_tree = find("farming_fruit_tree_patch_4")

    val yanille_hops = find("farming_hops_patch_1")
    val entrana_hops = find("farming_hops_patch_2")
    val lumbridge_hops = find("farming_hops_patch_3")
    val seers_hops = find("farming_hops_patch_4")

    // Regular woodcutting trees whose axe rates, level requirement, logs and xp farmed trees
    // share.
    val oak_tree_ref = find("oaktree")
    val willow_tree_ref = find("willowtree")
    val maple_tree_ref = find("mapletree")
    val yew_tree_ref = find("yewtree")
    val magic_tree_ref = find("magictree")
}

object FarmingVarps : VarpReferences() {
    /** The 5-minute farming tick (minutes since the epoch / 5) that growth was last applied for. */
    val last_cycle = find("farming_last_cycle")

    val falador_allotment_nw = find("farming_patch_falador_allotment_nw")
    val falador_allotment_se = find("farming_patch_falador_allotment_se")
    val falador_flower = find("farming_patch_falador_flower")
    val falador_herb = find("farming_patch_falador_herb")
    val catherby_allotment_n = find("farming_patch_catherby_allotment_n")
    val catherby_allotment_s = find("farming_patch_catherby_allotment_s")
    val catherby_flower = find("farming_patch_catherby_flower")
    val catherby_herb = find("farming_patch_catherby_herb")
    val ardougne_allotment_n = find("farming_patch_ardougne_allotment_n")
    val ardougne_allotment_s = find("farming_patch_ardougne_allotment_s")
    val ardougne_flower = find("farming_patch_ardougne_flower")
    val ardougne_herb = find("farming_patch_ardougne_herb")
    val morytania_allotment_nw = find("farming_patch_morytania_allotment_nw")
    val morytania_allotment_se = find("farming_patch_morytania_allotment_se")
    val morytania_flower = find("farming_patch_morytania_flower")
    val morytania_herb = find("farming_patch_morytania_herb")
    val taverley_tree = find("farming_patch_taverley_tree")
    val falador_tree = find("farming_patch_falador_tree")
    val varrock_tree = find("farming_patch_varrock_tree")
    val lumbridge_tree = find("farming_patch_lumbridge_tree")
    val gnome_tree = find("farming_patch_gnome_tree")
    val gnome_fruit_tree = find("farming_patch_gnome_fruit_tree")
    val village_fruit_tree = find("farming_patch_village_fruit_tree")
    val brimhaven_fruit_tree = find("farming_patch_brimhaven_fruit_tree")
    val catherby_fruit_tree = find("farming_patch_catherby_fruit_tree")
    val yanille_hops = find("farming_patch_yanille_hops")
    val entrana_hops = find("farming_patch_entrana_hops")
    val lumbridge_hops = find("farming_patch_lumbridge_hops")
    val seers_hops = find("farming_patch_seers_hops")

    /** The client varp that holds `farming_transmit_a` to `farming_transmit_d`. */
    val transmit_abcd = find("farming_state_temp_3")
}

object FarmingVarBits : VarBitReferences() {
    val transmit_a = find("farming_transmit_a")
    val transmit_b = find("farming_transmit_b")
    val transmit_c = find("farming_transmit_c")
    val transmit_d = find("farming_transmit_d")
}

object FarmingSeqs : SeqReferences() {
    val rake = find("farming_raking")
    val dig = find("human_dig")
    val dibbing = find("farming_seed_dibbing")
    val trowel = find("farming_trowel_digging")
    val compost = find("farming_pour_water")
    val watering = find("farming_watering")
    val plant_cure = find("farming_plant_cure")
    val pick = find("picking_low")
    val prune = find("pruning_low")
    val fill_plantpot = find("farming_filling_plantpot")
}

object FarmingTimers : TimerReferences() {
    val tick = find("farming_tick")
}

object FarmingNpcs : NpcReferences() {
    val tool_leprechaun = find("farming_tools_leprechaun")
}

internal object FarmingVarpBuilder : VarpBuilder() {
    init {
        build("farming_last_cycle")
        for (name in FarmingPatchVarpNames) {
            build(name)
        }
    }
}

internal val FarmingPatchVarpNames: List<String> =
    listOf(
        "farming_patch_falador_allotment_nw",
        "farming_patch_falador_allotment_se",
        "farming_patch_falador_flower",
        "farming_patch_falador_herb",
        "farming_patch_catherby_allotment_n",
        "farming_patch_catherby_allotment_s",
        "farming_patch_catherby_flower",
        "farming_patch_catherby_herb",
        "farming_patch_ardougne_allotment_n",
        "farming_patch_ardougne_allotment_s",
        "farming_patch_ardougne_flower",
        "farming_patch_ardougne_herb",
        "farming_patch_morytania_allotment_nw",
        "farming_patch_morytania_allotment_se",
        "farming_patch_morytania_flower",
        "farming_patch_morytania_herb",
        "farming_patch_taverley_tree",
        "farming_patch_falador_tree",
        "farming_patch_varrock_tree",
        "farming_patch_lumbridge_tree",
        "farming_patch_gnome_tree",
        "farming_patch_gnome_fruit_tree",
        "farming_patch_village_fruit_tree",
        "farming_patch_brimhaven_fruit_tree",
        "farming_patch_catherby_fruit_tree",
        "farming_patch_yanille_hops",
        "farming_patch_entrana_hops",
        "farming_patch_lumbridge_hops",
        "farming_patch_seers_hops",
    )
