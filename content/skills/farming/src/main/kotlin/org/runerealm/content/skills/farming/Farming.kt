package org.runerealm.content.skills.farming

import jakarta.inject.Inject
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.woodcuttingLvl
import org.runerealm.api.random.GameRandom
import org.runerealm.api.repo.obj.ObjRepository
import org.runerealm.api.script.onOpHeldU
import org.runerealm.api.script.onOpLoc1
import org.runerealm.api.script.onOpLoc2
import org.runerealm.api.script.onOpLocU
import org.runerealm.api.script.onOpNpcU
import org.runerealm.api.script.onPlayerLogin
import org.runerealm.api.script.onPlayerSoftTimer
import org.runerealm.api.stats.xpmod.XpModifiers
import org.runerealm.content.skills.farming.configs.FarmingCrop
import org.runerealm.content.skills.farming.configs.FarmingCrops
import org.runerealm.content.skills.farming.configs.FarmingPatch
import org.runerealm.content.skills.farming.configs.FarmingRegion
import org.runerealm.content.skills.farming.configs.PatchType
import org.runerealm.content.skills.farming.configs.TreeSeed
import org.runerealm.content.skills.farming.configs.TreeSeeds
import org.runerealm.content.skills.farming.configs.farming_npcs
import org.runerealm.content.skills.farming.configs.farming_objs
import org.runerealm.content.skills.farming.configs.farming_seqs
import org.runerealm.content.skills.farming.configs.farming_timers
import org.runerealm.content.skills.woodcutting.scripts.Woodcutting
import org.runerealm.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.runerealm.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLevelReq
import org.runerealm.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.runerealm.content.skills.woodcutting.scripts.Woodcutting.Companion.treeXp
import org.runerealm.game.entity.Player
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.enums.EnumTypeList
import org.runerealm.game.type.loc.LocTypeList
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.UnpackedObjType
import org.runerealm.game.type.obj.isType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

private typealias objs = farming_objs

private typealias seqs = farming_seqs

/**
 * The [Farming](https://oldschool.runescape.wiki/w/Farming) skill: allotment, herb, flower, hops,
 * tree and fruit tree patches.
 *
 * Patch state lives in one server varp per patch (see [PatchState]); the client is told what to
 * draw through the `farming_transmit_*` varbits for the area the player is in (see
 * [FarmingRegion]). Growth runs on the global 5-minute farming clock, both while online and, on
 * login, for the time spent offline ([FarmingGrowth.catchUp]).
 */
class Farming
@Inject
constructor(
    private val random: GameRandom,
    private val objTypes: ObjTypeList,
    private val locTypes: LocTypeList,
    private val enumTypes: EnumTypeList,
    private val objRepo: ObjRepository,
    private val xpMods: XpModifiers,
) : PluginScript() {
    private val wateringCans: List<ObjType> by lazy {
        listOf(
            objs.watering_can_0,
            objs.watering_can_1,
            objs.watering_can_2,
            objs.watering_can_3,
            objs.watering_can_4,
            objs.watering_can_5,
            objs.watering_can_6,
            objs.watering_can_7,
            objs.watering_can_8,
        )
    }

    private val compostBuckets: Map<Int, Compost> by lazy {
        mapOf(
            objs.bucket_compost.id to Compost.Regular,
            objs.bucket_supercompost.id to Compost.Super,
            objs.bucket_ultracompost.id to Compost.Ultra,
        )
    }

    override fun ScriptContext.startup() {
        onPlayerLogin { player.farmingLogin() }
        onPlayerSoftTimer(farming_timers.tick) { player.farmingTick() }

        // Item-on-loc ops on a multiloc are only looked up on the visible variant (they do not
        // fall back to the base loc like click ops do), so register every variant of every patch
        // and resolve the patch from the base loc.
        val useTargets = sortedSetOf<Int>()
        for (patch in FarmingPatch.entries) {
            onOpLoc1(patch.loc) { primaryOp(patch) }
            onOpLoc2(patch.loc) { inspect(patch) }
            useTargets += patch.loc.id
            useTargets += locTypes[patch.loc].multiLoc.map { it.toInt() and 0xFFFF }
        }
        for (target in useTargets) {
            val type = locTypes[target] ?: continue
            onOpLocU(type) { event ->
                val patch = FarmingPatch.forLoc(event.loc.id) ?: return@onOpLocU
                useOn(patch, event.objType)
            }
        }

        for (seed in TreeSeeds.all) {
            onOpHeldU(seed.seed, objs.plantpot_filled) { sowSeedling(seed) }
            for (can in wateringCans) {
                onOpHeldU(can, seed.seedling) { waterSeedling(seed, it.first) }
            }
        }

        val notable = FarmingCrops.all.mapNotNull { it.produce }.distinctBy { it.id }
        for (produce in notable) {
            onOpNpcU(farming_npcs.tool_leprechaun, produce) { noteProduce(produce) }
        }
    }

    /* Login, growth and client sync */

    private fun Player.farmingLogin() {
        FarmingGrowth.catchUp(this, FarmingClock.currentTick(), random)
        softTimer(farming_timers.tick, 1)
    }

    private fun Player.farmingTick() {
        if (FarmingGrowth.catchUp(this, FarmingClock.currentTick(), random)) {
            growSeedlings()
        }
        refreshPatches()
    }

    private fun Player.refreshPatches() {
        FarmingGrowth.transmit(this, FarmingRegion.at(coords))
    }

    /** Watered seedlings in the inventory grow into saplings on the next farming tick. */
    private fun Player.growSeedlings() {
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            val seed = TreeSeeds.all.firstOrNull { it.watered.id == obj.id } ?: continue
            inv[slot] = InvObj(seed.sapling, obj.count, obj.vars)
        }
    }

    private fun ProtectedAccess.state(patch: FarmingPatch): PatchState =
        FarmingGrowth.read(player, patch)

    private fun ProtectedAccess.update(patch: FarmingPatch, state: PatchState) {
        FarmingGrowth.write(player, patch, state)
        player.refreshPatches()
    }

    /* Op handlers */

    private suspend fun ProtectedAccess.primaryOp(patch: FarmingPatch) {
        val state = state(patch)
        val crop = state.crop
        if (crop == null) {
            if (state.weeding < PatchState.WEEDED) {
                rake(patch)
            } else {
                mes("This patch doesn't need weeding.")
            }
            return
        }
        when (state.status) {
            CropStatus.Growing -> inspect(patch)
            CropStatus.Diseased -> if (crop.type.isTree) prune(patch) else cure(patch)
            CropStatus.Dead,
            CropStatus.Stump -> clear(patch)
            CropStatus.CheckHealth -> checkHealth(patch, crop)
            CropStatus.Grown ->
                when (crop.type) {
                    PatchType.Allotment,
                    PatchType.Herb,
                    PatchType.Hops -> harvest(patch, crop)
                    PatchType.Flower -> pickFlower(patch, crop)
                    PatchType.Tree -> chop(patch, crop)
                    PatchType.FruitTree ->
                        if (state.lives > 0) pickFruit(patch, crop) else chopFruitTree(patch, crop)
                }
        }
    }

    private suspend fun ProtectedAccess.useOn(patch: FarmingPatch, obj: UnpackedObjType) {
        val compost = compostBuckets[obj.id]
        when {
            obj.isType(objs.rake) -> rake(patch)
            obj.isType(objs.spade) -> dig(patch)
            compost != null -> compost(patch, obj, compost)
            wateringCans.any { obj.isType(it) } -> water(patch, obj)
            obj.isType(objs.plant_cure) -> cure(patch)
            obj.isType(objs.secateurs) || obj.isType(objs.magic_secateurs) -> prune(patch)
            obj.isType(objs.plantpot_empty) -> fillPlantPot(patch)
            FarmingCrops.isPlantable(obj) -> plant(patch, obj)
            else -> mes("Nothing interesting happens.")
        }
    }

    private fun ProtectedAccess.inspect(patch: FarmingPatch) {
        val state = state(patch)
        val label = patch.type.label
        val article = if (label.first() in "aeiou") "an" else "a"
        mes("This is $article $label.")
        val compost = state.compost
        if (compost == Compost.None) {
            mes("The soil has not been treated.")
        } else {
            mes("The soil has been treated with ${compost.label}.")
        }
        val crop = state.crop
        val status =
            when {
                crop == null && state.weeding < PatchState.WEEDED -> "The patch needs weeding."
                crop == null -> "The patch is empty and weeded."
                state.status == CropStatus.Diseased ->
                    "The patch is diseased and needs attending to before it dies."
                state.status == CropStatus.Dead ->
                    "The patch has become infected by disease and has died."
                state.status == CropStatus.Stump ->
                    "The patch has the remains of a tree stump in it."
                state.status == CropStatus.Growing ->
                    "The patch has something growing in it. (${crop.name}, stage " +
                        "${state.stage + 1} of ${crop.stages + 1})"
                else -> "The patch is fully grown."
            }
        mes(status)
    }

    /* Weeds */

    private suspend fun ProtectedAccess.rake(patch: FarmingPatch) {
        if (!state(patch).let { it.isEmpty && it.weeding < PatchState.WEEDED }) {
            mes("This patch doesn't need weeding.")
            return
        }
        if (objs.rake !in inv) {
            mes("You need a rake to weed a farming patch.")
            return
        }
        while (true) {
            anim(seqs.rake)
            delay(RAKE_TICKS)
            val state = state(patch)
            if (!state.isEmpty || state.weeding >= PatchState.WEEDED) {
                break
            }
            update(patch, state.withWeeding(state.weeding + 1))
            invAddOrDrop(objRepo, objs.weeds)
            statAdvance(stats.farming, WEED_XP)
            if (state.weeding + 1 >= PatchState.WEEDED) {
                mes("You successfully clear the patch of weeds.")
                break
            }
        }
        resetAnim()
    }

    /* Treating patches */

    private suspend fun ProtectedAccess.compost(
        patch: FarmingPatch,
        bucket: UnpackedObjType,
        compost: Compost,
    ) {
        val state = state(patch)
        val label = patch.type.label
        if (state.isEmpty && !state.isWeeded) {
            mes("This patch needs weeding first.")
            return
        }
        if (!state.isEmpty && state.status != CropStatus.Growing) {
            mes("There's no point treating the $label now.")
            return
        }
        if (state.compost != Compost.None) {
            mes("This $label has already been treated with ${state.compost.label}.")
            return
        }
        anim(seqs.compost)
        delay(COMPOST_TICKS)
        invReplace(inv, bucket, 1, objs.bucket_empty)
        update(patch, state(patch).withCompost(compost))
        statAdvance(stats.farming, compost.xp)
        mes("You treat the $label with ${compost.label}.")
    }

    private suspend fun ProtectedAccess.water(patch: FarmingPatch, can: UnpackedObjType) {
        val state = state(patch)
        val crop = state.crop
        if (!patch.type.waterable || crop == null) {
            mes("This patch doesn't need watering.")
            return
        }
        if (state.status != CropStatus.Growing || state.watered) {
            mes("This patch doesn't need watering.")
            return
        }
        val uses = wateringCans.indexOfFirst { can.isType(it) }
        if (uses <= 0) {
            mes("You need to fill your watering can first.")
            return
        }
        anim(seqs.watering)
        delay(WATER_TICKS)
        invReplace(inv, can, 1, wateringCans[uses - 1])
        update(patch, state(patch).withWatered(true))
        mes("You water the ${patch.type.label}.")
    }

    private suspend fun ProtectedAccess.cure(patch: FarmingPatch) {
        val state = state(patch)
        val crop = state.crop
        if (crop == null || state.status != CropStatus.Diseased) {
            mes("This patch doesn't need curing.")
            return
        }
        if (crop.type.isTree) {
            mes("You need to prune away the diseased leaves with a pair of secateurs.")
            return
        }
        if (objs.plant_cure !in inv) {
            mes("You need some plant cure to cure this patch.")
            return
        }
        val label = patch.type.label
        anim(seqs.plant_cure)
        delay(CURE_TICKS)
        invReplace(inv, objs.plant_cure, 1, objs.vial_empty)
        update(patch, state(patch).withStatus(CropStatus.Growing))
        mes("You treat the $label with the plant cure.")
        mes("It is restored to health.")
    }

    private suspend fun ProtectedAccess.prune(patch: FarmingPatch) {
        val state = state(patch)
        val crop = state.crop
        if (crop == null || state.status != CropStatus.Diseased || !crop.type.isTree) {
            mes("This patch doesn't need pruning.")
            return
        }
        if (!hasTool(objs.secateurs) && !hasTool(objs.magic_secateurs)) {
            mes("You need a pair of secateurs to prune the diseased leaves.")
            return
        }
        while (true) {
            anim(seqs.prune)
            delay(PRUNE_TICKS)
            if (state(patch).status != CropStatus.Diseased) {
                return
            }
            // 75% chance to prune successfully (Disease (Farming)).
            if (random.of(4) != 0) {
                update(patch, state(patch).withStatus(CropStatus.Growing))
                mes("You have successfully removed all the diseased leaves.")
                resetAnim()
                return
            }
        }
    }

    /* Planting */

    private suspend fun ProtectedAccess.plant(patch: FarmingPatch, seed: UnpackedObjType) {
        val crop = FarmingCrops.forSeed(patch.type, seed)
        val label = patch.type.label
        if (crop == null) {
            mes("You can't plant ${seed.name.lowercase()} in this $label.")
            return
        }
        val state = state(patch)
        if (!state.isEmpty) {
            mes("You can only plant ${crop.seedName(objTypes)} in an empty patch.")
            return
        }
        if (!state.isWeeded) {
            mes("This patch needs weeding first.")
            return
        }
        if (statBase(stats.farming) < crop.level) {
            mes("You must be a Level ${crop.level} Farmer to plant those.")
            return
        }
        val tree = crop.type.isTree
        if (tree && objs.gardening_trowel !in inv) {
            mes("You need a gardening trowel to do that.")
            return
        }
        if (!tree && objs.dibber !in inv) {
            mes("You need a seed dibber to plant seeds.")
            return
        }
        if (invTotal(inv, crop.seed) < crop.seedCount) {
            mes("You need ${crop.seedCount} ${crop.seedName(objTypes)} to plant a $label.")
            return
        }
        anim(if (tree) seqs.trowel else seqs.dibbing)
        delay(PLANT_TICKS)
        invDel(inv, crop.seed, crop.seedCount)
        if (tree) {
            invAdd(inv, objs.plantpot_empty)
        }
        update(patch, PatchState.planted(crop, state(patch).compost))
        statAdvance(stats.farming, crop.plantXp)
        val planted =
            when {
                tree -> "the ${crop.seedName(objTypes)}"
                crop.seedCount == 1 -> "a ${crop.seedName(objTypes)}"
                else -> "${crop.seedCount} ${crop.seedName(objTypes)}"
            }
        mes("You plant $planted in the $label.")
    }

    /* Harvesting */

    private suspend fun ProtectedAccess.harvest(patch: FarmingPatch, crop: FarmingCrop) {
        val produce = crop.produce ?: return
        val needsSpade = crop.type != PatchType.Herb
        if (needsSpade && objs.spade !in inv) {
            mes("You need a spade to harvest your crops.")
            return
        }
        val label = patch.type.label
        mes("You begin to harvest the $label.")
        while (true) {
            if (inv.isFull()) {
                mes("You have run out of inventory space.")
                break
            }
            anim(if (needsSpade) seqs.dig else seqs.pick)
            delay(HARVEST_TICKS)
            val state = state(patch)
            if (state.crop != crop || state.status != CropStatus.Grown) {
                break
            }
            invAdd(inv, produce)
            statAdvance(stats.farming, crop.harvestXp)
            val saved = random.of(CTS_ROLL) < saveChance(crop)
            val lives = if (saved) state.lives else state.lives - 1
            if (lives <= 0) {
                update(patch, PatchState.CLEAR)
                mes("The $label is now empty.")
                break
            }
            update(patch, state.withLives(lives))
        }
        resetAnim()
    }

    /**
     * Chance (out of 256) to keep a harvest life, from
     * [Variable crop yield](https://oldschool.runescape.wiki/w/Farming#Variable_crop_yield). Magic
     * secateurs raise both chance-to-save constants by 10%.
     */
    private fun ProtectedAccess.saveChance(crop: FarmingCrop): Int {
        val magic = hasTool(objs.magic_secateurs)
        return saveChance(crop, statBase(stats.farming), magic)
    }

    private suspend fun ProtectedAccess.pickFlower(patch: FarmingPatch, crop: FarmingCrop) {
        val produce = crop.produce ?: return
        if (inv.isFull()) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        anim(seqs.pick)
        delay(HARVEST_TICKS)
        if (state(patch).status != CropStatus.Grown) {
            return
        }
        val count = if (produce.id == objs.limpwurt_root.id) LIMPWURT_YIELD else 1
        invAdd(inv, produce, count)
        statAdvance(stats.farming, crop.harvestXp)
        update(patch, PatchState.CLEAR)
        mes("You harvest the ${patch.type.label}.")
    }

    private suspend fun ProtectedAccess.checkHealth(patch: FarmingPatch, crop: FarmingCrop) {
        anim(seqs.pick)
        delay(CHECK_TICKS)
        val state = state(patch)
        if (state.crop != crop || state.status != CropStatus.CheckHealth) {
            return
        }
        val next = state.withStatus(CropStatus.Grown)
        update(patch, if (crop.type == PatchType.FruitTree) next.withLives(MAX_FRUIT) else next)
        statAdvance(stats.farming, crop.checkXp)
        mes("You examine the tree for signs of disease and find that it is in perfect health.")
    }

    private suspend fun ProtectedAccess.pickFruit(patch: FarmingPatch, crop: FarmingCrop) {
        val produce = crop.produce ?: return
        while (true) {
            if (inv.isFull()) {
                mes("You don't have enough inventory space to do that.")
                break
            }
            anim(seqs.pick)
            delay(HARVEST_TICKS)
            val state = state(patch)
            if (state.crop != crop || state.status != CropStatus.Grown || state.lives <= 0) {
                break
            }
            invAdd(inv, produce)
            statAdvance(stats.farming, crop.harvestXp)
            spam("You pick ${objTypes[produce].name.withArticle()}.")
            update(patch, state.withLives(state.lives - 1))
            if (state.lives - 1 <= 0) {
                break
            }
        }
        resetAnim()
    }

    private suspend fun ProtectedAccess.chopFruitTree(patch: FarmingPatch, crop: FarmingCrop) {
        val axe = Woodcutting.findAxe(player, objTypes)
        if (axe == null) {
            mes("You need an axe to chop down this tree.")
            return
        }
        anim(objTypes[axe].axeWoodcuttingAnim)
        delay(CHOP_TICKS)
        val state = state(patch)
        if (state.crop != crop || state.status != CropStatus.Grown) {
            return
        }
        update(patch, state.withStatus(CropStatus.Stump).withLives(0))
        resetAnim()
        mes("You chop down the ${crop.name} tree.")
    }

    /** Farmed trees use the matching regular tree's woodcutting level, rates, logs and xp. */
    private suspend fun ProtectedAccess.chop(patch: FarmingPatch, crop: FarmingCrop) {
        val reference = locTypes[crop.woodcuttingTree ?: return]
        if (player.woodcuttingLvl < reference.treeLevelReq) {
            mes("You need a Woodcutting level of ${reference.treeLevelReq} to chop down this tree.")
            return
        }
        val axe = Woodcutting.findAxe(player, objTypes)
        if (axe == null) {
            mes("You need an axe to chop down this tree.")
            mes("You do not have an axe which you have the woodcutting level to use.")
            return
        }
        val logs = objTypes[reference.treeLogs]
        mes("You swing your axe at the tree.")
        while (true) {
            if (inv.isFull()) {
                mes("Your inventory is too full to hold any more ${logs.name.lowercase()}.")
                break
            }
            anim(objTypes[axe].axeWoodcuttingAnim)
            delay(CHOP_TICKS)
            val state = state(patch)
            if (state.crop != crop || state.status != CropStatus.Grown) {
                break
            }
            val (low, high) = Woodcutting.cutSuccessRates(reference, axe, enumTypes)
            if (!statRandom(stats.woodcutting, low, high, invisibleBoost = 0)) {
                continue
            }
            invAdd(inv, logs)
            statAdvance(stats.woodcutting, reference.treeXp * xpMods.get(player, stats.woodcutting))
            spam("You get some ${logs.name.lowercase()}.")
            // Farmed trees fall with the classic 1/8 chance per log.
            if (random.of(TREE_FELL_ROLL) == 0) {
                update(patch, state.withStatus(CropStatus.Stump))
                mes("You have chopped down the tree.")
                break
            }
        }
        resetAnim()
    }

    /* Clearing */

    private suspend fun ProtectedAccess.dig(patch: FarmingPatch) {
        val state = state(patch)
        val crop = state.crop
        if (crop == null) {
            if (!state.isWeeded) {
                mes("This patch needs weeding first.")
            } else {
                mes("This patch is already clear.")
            }
            return
        }
        when (state.status) {
            CropStatus.Dead,
            CropStatus.Stump -> clear(patch)
            CropStatus.Grown ->
                if (crop.type == PatchType.Allotment || crop.type == PatchType.Hops) {
                    harvest(patch, crop)
                } else {
                    confirmDigUp(patch)
                }
            else -> confirmDigUp(patch)
        }
    }

    private suspend fun ProtectedAccess.confirmDigUp(patch: FarmingPatch) {
        val confirmed =
            choice2(
                "Yes, I want to clear it for new crops.",
                true,
                "No, I want to leave it as it is.",
                false,
                title = "Are you sure you want to dig up this patch?",
            )
        if (confirmed) {
            clear(patch)
        }
    }

    private suspend fun ProtectedAccess.clear(patch: FarmingPatch) {
        if (objs.spade !in inv) {
            mes("You need a spade to do that.")
            return
        }
        val before = state(patch)
        mes("You start digging the farming patch...")
        anim(seqs.dig)
        delay(CLEAR_TICKS)
        if (state(patch) != before) {
            return
        }
        val roots = before.crop?.roots
        if (before.status == CropStatus.Stump && roots != null) {
            invAddOrDrop(objRepo, roots)
        }
        update(patch, PatchState.CLEAR)
        resetAnim()
        mes("You have successfully cleared this patch for new crops.")
    }

    /* Plant pots and seedlings */

    private suspend fun ProtectedAccess.fillPlantPot(patch: FarmingPatch) {
        if (!state(patch).isWeeded) {
            mes("This patch needs to be empty and weeded to fill a plant pot.")
            return
        }
        if (objs.gardening_trowel !in inv) {
            mes("You need a gardening trowel to do that.")
            return
        }
        anim(seqs.fill_plantpot)
        delay(PLANT_TICKS)
        invReplace(inv, objs.plantpot_empty, 1, objs.plantpot_filled)
        mes("You fill the plant pot with soil.")
    }

    private fun ProtectedAccess.sowSeedling(seed: TreeSeed) {
        if (statBase(stats.farming) < seed.level) {
            mes("You must be a Level ${seed.level} Farmer to plant those.")
            return
        }
        if (objs.gardening_trowel !in inv) {
            mes("You need a gardening trowel to do that.")
            return
        }
        invDel(inv, seed.seed, 1)
        invReplace(inv, objs.plantpot_filled, 1, seed.seedling)
        mes("You plant the ${objTypes[seed.seed].name.lowercase()} in the plant pot.")
    }

    private fun ProtectedAccess.waterSeedling(seed: TreeSeed, can: UnpackedObjType) {
        val uses = wateringCans.indexOfFirst { can.isType(it) }
        if (uses <= 0) {
            mes("You need to fill your watering can first.")
            return
        }
        invReplace(inv, can, 1, wateringCans[uses - 1])
        invReplace(inv, seed.seedling, 1, seed.watered)
        mes("You water the seedling.")
    }

    /* Tool leprechaun */

    private fun ProtectedAccess.noteProduce(produce: ObjType) {
        val cert = ocCert(produce)
        val count = invTotal(inv, produce)
        if (cert.id == produce.id || count <= 0) {
            mes("The leprechaun can't note that for you.")
            return
        }
        invDel(inv, produce, count)
        invAdd(inv, cert, count)
        mes("The leprechaun exchanges your items for banknotes.")
    }

    private fun ProtectedAccess.hasTool(obj: ObjType): Boolean = obj in inv || obj in worn

    companion object {
        private const val RAKE_TICKS = 4
        private const val COMPOST_TICKS = 2
        private const val WATER_TICKS = 2
        private const val CURE_TICKS = 3
        private const val PRUNE_TICKS = 3
        private const val PLANT_TICKS = 3
        private const val HARVEST_TICKS = 2
        private const val CHECK_TICKS = 2
        private const val CHOP_TICKS = 4
        private const val CLEAR_TICKS = 3
        private const val WEED_XP = 4.0
        private const val CTS_ROLL = 256
        private const val TREE_FELL_ROLL = 8
        private const val LIMPWURT_YIELD = 3
        private const val MAX_FRUIT = FarmingGrowth.MAX_FRUIT

        /** Chance (out of 256) to keep a harvest life at Farming [level]. */
        fun saveChance(crop: FarmingCrop, level: Int, magicSecateurs: Boolean): Int {
            val boost = if (magicSecateurs) 1.1 else 1.0
            val low = (crop.ctsLow * boost).toInt()
            val high = (crop.ctsHigh * boost).toInt()
            val lvl = level.coerceIn(1, 99)
            val scaled = (low * (99 - lvl)) / 98.0 + (high * (lvl - 1)) / 98.0 + 0.5
            return 1 + scaled.toInt()
        }

        private fun FarmingCrop.seedName(objTypes: ObjTypeList): String {
            val name = objTypes[seed].name.lowercase()
            return if (seedCount > 1 && !name.endsWith("s")) "${name}s" else name
        }

        private fun String.withArticle(): String {
            val lower = lowercase()
            return if (lower.first() in "aeiou") "an $lower" else "a $lower"
        }
    }
}
