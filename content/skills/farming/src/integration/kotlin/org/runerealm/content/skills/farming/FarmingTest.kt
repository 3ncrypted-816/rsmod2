package org.runerealm.content.skills.farming

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.runerealm.api.config.refs.stats
import org.runerealm.api.inv.LocUOpScript
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.skills.farming.configs.FarmingCrop
import org.runerealm.content.skills.farming.configs.FarmingCrops
import org.runerealm.content.skills.farming.configs.FarmingPatch
import org.runerealm.content.skills.farming.configs.farming_objs
import org.runerealm.content.skills.farming.configs.farming_varbits
import org.runerealm.content.skills.farming.configs.farming_varps
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.player.SessionStateEvent
import org.runerealm.game.inv.InvObj
import org.runerealm.game.loc.BoundLocInfo
import org.runerealm.game.type.obj.ObjType
import org.runerealm.map.CoordGrid

/** Runs on one thread: the tests pin and fast-forward the shared [FarmingClock]. */
@Execution(ExecutionMode.SAME_THREAD)
class FarmingTest {
    @AfterEach
    fun releaseClock() {
        FarmingClock.fixedTick = null
    }

    @Test
    fun GameTestState.`raking a weedy allotment clears it and gives weeds and xp`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val patch = setUpPatch(FarmingPatch.FaladorAllotmentNw, FALADOR_ALLOTMENT)
            give(farming_objs.rake)
            advance()
            assertEquals(0, player.vars[farming_varbits.transmit_a])

            player.opLoc1(patch)
            advanceUntil({ state(FarmingPatch.FaladorAllotmentNw).isWeeded }, timeoutTicks = 40)

            assertEquals(3, player.count(farming_objs.weeds))
            assertEquals(fineXp(12.0), player.statMap.getFineXP(stats.farming))
            advance()
            assertEquals(3, player.vars[farming_varbits.transmit_a])
        }

    @Test
    fun GameTestState.`planting potatoes then fast-forwarding grows and harvests them`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val allotment = FarmingPatch.FaladorAllotmentNw
            val patch = setUpPatch(allotment, FALADOR_ALLOTMENT)
            setState(allotment, PatchState.CLEAR)
            give(farming_objs.dibber)
            give(farming_objs.spade)
            give(farming_objs.potato_seed, count = 3)
            advance()

            player.opLocU(patch, invSlot = 2)
            advanceUntil({ !state(allotment).isEmpty }, timeoutTicks = 20)
            val planted = state(allotment)
            assertEquals("potato", planted.crop?.name)
            assertEquals(0, planted.stage)
            assertEquals(0, player.count(farming_objs.potato_seed))
            assertEquals(fineXp(8.0), player.statMap.getFineXP(stats.farming))
            advance()
            assertEquals(6, player.vars[farming_varbits.transmit_a])

            // Four 10-minute allotment cycles; no disease on the three rolls after the seedling.
            repeat(3) { random.next = 127 }
            FarmingClock.fixedTick = START_TICK + 8
            advance()
            val grown = state(allotment)
            assertEquals(CropStatus.Grown, grown.status)
            assertEquals(FarmingGrowth.BASE_LIVES, grown.lives)
            assertEquals(10, player.vars[farming_varbits.transmit_a])

            // Every chance-to-save roll fails, so the three lives give three potatoes.
            repeat(3) { random.next = 255 }
            player.opLoc1(patch)
            advanceUntil({ state(allotment).isEmpty }, timeoutTicks = 40)
            assertEquals(3, player.count(farming_objs.potato))
            assertEquals(fineXp(8.0 + 3 * 9.0), player.statMap.getFineXP(stats.farming))
            assertTrue(state(allotment).isWeeded)
            advance()
            assertEquals(3, player.vars[farming_varbits.transmit_a])
        }

    @Test
    fun GameTestState.`picking herbs gives grimy herbs and harvest xp`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val herbPatch = FarmingPatch.FaladorHerb
            val patch = setUpPatch(herbPatch, FALADOR_HERB)
            val guam = checkNotNull(FarmingCrops.forSeed(herbPatch.type, farming_objs.guam_seed))
            setState(herbPatch, grown(guam).withLives(2))
            advance()
            assertEquals(guam.base + guam.stages, player.vars[farming_varbits.transmit_d])

            repeat(2) { random.next = 255 }
            player.opLoc1(patch)
            advanceUntil({ state(herbPatch).isEmpty }, timeoutTicks = 40)

            assertEquals(2, player.count(farming_objs.grimy_guam))
            assertEquals(fineXp(2 * 12.5), player.statMap.getFineXP(stats.farming))
            assertMessageSentWithin("The herb patch is now empty.")
        }

    @Test
    fun GameTestState.`checking the health of a grown tree gives xp and makes it choppable`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val treePatch = FarmingPatch.LumbridgeTree
            val patch = setUpPatch(treePatch, LUMBRIDGE_TREE)
            val oak = checkNotNull(FarmingCrops.forSeed(treePatch.type, farming_objs.oak_sapling))
            setState(
                treePatch,
                PatchState.planted(oak, Compost.None)
                    .withStage(oak.stages)
                    .withStatus(CropStatus.CheckHealth),
            )
            advance()
            assertEquals(oak.base + oak.stages, player.vars[farming_varbits.transmit_a])

            player.opLoc1(patch)
            advanceUntil({ state(treePatch).status == CropStatus.Grown }, timeoutTicks = 20)

            assertEquals(fineXp(467.3), player.statMap.getFineXP(stats.farming))
            advance()
            assertEquals(oak.base + oak.stages + 1, player.vars[farming_varbits.transmit_a])
        }

    @Test
    fun GameTestState.`diseased herbs die unless cured with plant cure`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val herbPatch = FarmingPatch.FaladorHerb
            val patch = setUpPatch(herbPatch, FALADOR_HERB)
            val guam = checkNotNull(FarmingCrops.forSeed(herbPatch.type, farming_objs.guam_seed))
            setState(herbPatch, PatchState.planted(guam, Compost.None).withStage(1))
            give(farming_objs.plant_cure)
            advance()

            // One 20-minute herb cycle with a disease roll of 0.
            random.next = 0
            FarmingClock.fixedTick = START_TICK + 4
            advance()
            assertEquals(CropStatus.Diseased, state(herbPatch).status)
            assertEquals(guam.diseasedBase, player.vars[farming_varbits.transmit_d])

            player.opLoc1(patch)
            advanceUntil({ state(herbPatch).status == CropStatus.Growing }, timeoutTicks = 20)
            assertEquals(1, player.count(farming_objs.vial_empty))
            assertEquals(0, player.count(farming_objs.plant_cure))
            advance()
            assertEquals(guam.base + 1, player.vars[farming_varbits.transmit_d])

            // Diseased again, and left alone for another cycle: the herbs die.
            random.next = 0
            FarmingClock.fixedTick = START_TICK + 8
            advance()
            assertEquals(CropStatus.Diseased, state(herbPatch).status)
            FarmingClock.fixedTick = START_TICK + 12
            advance()
            assertEquals(CropStatus.Dead, state(herbPatch).status)
            assertEquals(170, player.vars[farming_varbits.transmit_d])
        }

    @Test
    fun GameTestState.`crops keep growing while the player is offline`() =
        runGameTest(Farming::class, LocUOpScript::class) {
            val allotment = FarmingPatch.FaladorAllotmentSe
            val potato =
                checkNotNull(FarmingCrops.forSeed(allotment.type, farming_objs.potato_seed))
            pinClock()
            // Logged out four hours ago with freshly planted potatoes and a raked flower patch.
            VarPlayerIntMapSetter.set(player, farming_varps.last_cycle, (START_TICK - 48).toInt())
            setState(allotment, PatchState.planted(potato, Compost.Super))
            setState(FarmingPatch.FaladorFlower, PatchState.CLEAR)
            repeat(3) { random.next = 127 }

            // Logging back in applies every farming tick missed while offline.
            eventBus.publish(SessionStateEvent.Login(player))

            val state = state(allotment)
            assertEquals(CropStatus.Grown, state.status)
            assertEquals(potato.stages, state.stage)
            // Supercompost adds two harvest lives.
            assertEquals(FarmingGrowth.BASE_LIVES + 2, state.lives)
            // The raked flower patch grew back over with weeds.
            assertEquals(0, state(FarmingPatch.FaladorFlower).weeding)
            assertEquals(START_TICK.toInt(), player.vars[farming_varps.last_cycle])
        }

    @Test
    fun GameTestState.`chance to save follows the wiki formula`() =
        runGameTest {
            val guam = checkNotNull(FarmingCrops.forSeed(FarmingPatch.FaladorHerb.type, farming_objs.guam_seed))
            // Wiki: 10.2% at level 1 and 31.6% at level 99 for guam.
            assertEquals(26, Farming.saveChance(guam, level = 1, magicSecateurs = false))
            assertEquals(81, Farming.saveChance(guam, level = 99, magicSecateurs = false))
            assertEquals(89, Farming.saveChance(guam, level = 99, magicSecateurs = true))
        }

    /** Pins the farming clock and marks every farming tick up to now as already applied. */
    private fun GameTestScope.pinClock(target: Player = player) {
        FarmingClock.fixedTick = START_TICK
        VarPlayerIntMapSetter.set(target, farming_varps.last_cycle, START_TICK.toInt())
    }

    private fun GameTestScope.setUpPatch(patch: FarmingPatch, coords: CoordGrid): BoundLocInfo {
        pinClock()
        val loc = placeMapLoc(coords, patch.loc)
        player.teleport(coords.translateX(-1))
        player.clearInv()
        return loc
    }

    private fun GameTestScope.give(type: ObjType, count: Int = 1) {
        val slot = player.inv.indices.first { player.inv[it] == null }
        player.inv[slot] = InvObj(type, count)
    }

    private fun GameTestScope.state(patch: FarmingPatch): PatchState =
        FarmingGrowth.read(player, patch)

    private fun GameTestScope.setState(patch: FarmingPatch, state: PatchState) {
        FarmingGrowth.write(player, patch, state)
    }

    private fun grown(crop: FarmingCrop): PatchState =
        PatchState.planted(crop, Compost.None).withStage(crop.stages).withStatus(CropStatus.Grown)

    private fun fineXp(xp: Double): Int = (xp * 10).toInt()

    /** Messages only last for the tick they are sent in, so check each tick for a while. */
    private fun GameTestScope.assertMessageSentWithin(text: String, ticks: Int = 0) {
        runCatching { assertMessageSent(text) }.getOrElse {
            if (ticks <= 0) throw it
            advance()
            assertMessageSentWithin(text, ticks - 1)
        }
    }

    private companion object {
        // A fixed farming tick that is a multiple of every cycle length (8 * 160-minute cycles).
        const val START_TICK: Long = 32L * 200_000L

        val FALADOR_ALLOTMENT = CoordGrid(3050, 3307, 0)
        val FALADOR_HERB = CoordGrid(3058, 3311, 0)
        val LUMBRIDGE_TREE = CoordGrid(3192, 3230, 0)
    }
}
