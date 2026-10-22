package org.runerealm.api.toxins

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.stats
import org.runerealm.api.hit.plugin.NpcHitScript
import org.runerealm.api.hit.plugin.PlayerHitScript
import org.runerealm.api.player.hat
import org.runerealm.api.player.stat.hitpoints
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.game.entity.npc.NpcMode
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.util.ParamMap
import org.runerealm.map.CoordGrid

class ToxinsTest {
    @Test
    fun `poison damage drops by one every five hits`() {
        // Weapon poison(p): severity 20, so 5 hits of 4, 5 of 3, 5 of 2 and 5 of 1.
        val damages = mutableListOf<Int>()
        var value = 20
        while (value > 0) {
            damages += ToxinValues.damage(value)
            value = ToxinValues.next(value)
        }
        val expected = List(5) { 4 } + List(5) { 3 } + List(5) { 2 } + List(5) { 1 }
        Assertions.assertEquals(expected, damages)
    }

    @Test
    fun `venom starts at 6 and rises by 2 up to 20`() {
        val damages = mutableListOf<Int>()
        var value = VENOM_THRESHOLD
        repeat(10) {
            damages += ToxinValues.damage(value)
            value = ToxinValues.next(value)
        }
        Assertions.assertEquals(listOf(6, 8, 10, 12, 14, 16, 18, 20, 20, 20), damages)
    }

    @Test
    fun GameTestState.`poisoned player takes green hits every 30 ticks`() = runToxinsTest {
        player.stats[stats.hitpoints] = 99
        assertTrue(player.poison(severity = 20))
        assertMessageSent("You have been poisoned!")
        assertTrue(player.isPoisoned())

        advance(ticks = TOXIN_INTERVAL - 1)
        assertEquals(99, player.hitpoints)

        advanceUntil({ player.hitpoints < 99 }, timeoutTicks = 3)
        assertEquals(95, player.hitpoints)
        assertEquals(19, player.vars[toxin_varps.poison])

        // A weaker poison does not replace a stronger one.
        assertFalse(player.poison(severity = 10))
    }

    @Test
    fun GameTestState.`venom replaces poison and deals increasing damage`() = runToxinsTest {
        player.stats[stats.hitpoints] = 99
        player.poison(severity = 30)
        assertTrue(player.envenom())
        assertTrue(player.isEnvenomed())
        assertFalse(player.isPoisoned())
        // Envenomed players cannot be poisoned on top.
        assertFalse(player.poison(severity = 30))

        advance(ticks = TOXIN_INTERVAL)
        advanceUntil({ player.hitpoints < 99 }, timeoutTicks = 3)
        assertEquals(93, player.hitpoints)
        assertEquals(8, player.nextToxinDamage())
    }

    @Test
    fun GameTestState.`antipoison cures poison and grants immunity`() = runToxinsTest {
        player.poison(severity = 20)
        player.curePoison(immunityIntervals = 5)
        assertFalse(player.isPoisoned())
        assertTrue(player.isPoisonImmune())
        assertEquals(5, player.poisonImmunityIntervals())
        assertFalse(player.poison(severity = 20))
        // Antipoison does not protect from venom.
        assertFalse(player.isVenomImmune())

        // Immunity wears off one interval at a time.
        advance(ticks = TOXIN_INTERVAL * 5 + 1)
        assertFalse(player.isPoisonImmune())
        assertTrue(player.poison(severity = 20))
    }

    @Test
    fun GameTestState.`antipoison converts venom to poison at the same damage`() = runToxinsTest {
        player.envenom()
        player.setVarp(toxin_varps.poison, VENOM_THRESHOLD + 2) // Next hit deals 10.
        player.curePoison(immunityIntervals = 20)
        assertTrue(player.isPoisoned())
        assertEquals(10, player.nextToxinDamage())
        assertFalse(player.isPoisonImmune())
    }

    @Test
    fun GameTestState.`anti-venom cures venom and grants venom immunity`() = runToxinsTest {
        player.envenom()
        player.cureVenom(immunityIntervals = 41)
        assertFalse(player.isEnvenomed())
        assertTrue(player.isVenomImmune())
        assertFalse(player.envenom())

        // Anti-venom's venom immunity lasts 3 intervals (36-54 seconds); poison immunity remains.
        advance(ticks = TOXIN_INTERVAL * 3 + 1)
        assertFalse(player.isVenomImmune())
        assertTrue(player.isPoisonImmune())
    }

    @Test
    fun GameTestState.`serpentine helm prevents poison and venom`() = runToxinsTest {
        player.hat = InvObj(toxin_objs.serpentine_helm)
        assertFalse(player.poison(severity = 20))
        assertFalse(player.envenom())
    }

    @Test
    fun GameTestState.`poisoned npc takes poison damage`() = runToxinsTest {
        val npc = spawnNpc(START, dummy)
        advance()
        assertTrue(npc.poison(severity = 30, source = player))
        advance(ticks = TOXIN_INTERVAL)
        advanceUntil({ npc.hitpoints < 50 }, timeoutTicks = 3)
        assertEquals(44, npc.hitpoints)
    }

    @Test
    fun GameTestState.`poison immune npcs cannot be poisoned`() = runToxinsTest {
        val npc = spawnNpc(START, immuneDummy)
        assertFalse(npc.poison(severity = 30))
        assertFalse(npc.isPoisoned())
    }

    @Test
    fun GameTestState.`weapon poison severity follows the poison strength`() = runToxinsTest {
        fun severity(name: String): Int = WeaponPoison.severity(firstObjType { it.name == name })
        assertEquals(20, severity("Bronze dagger(p)"))
        assertEquals(25, severity("Bronze dagger(p+)"))
        assertEquals(30, severity("Bronze dagger(p++)"))
        assertEquals(0, severity("Bronze dagger"))
    }

    private fun GameTestState.runToxinsTest(testBody: GameTestScope.() -> Unit) =
        runGameTest(
            ToxinsScript::class,
            PlayerHitScript::class,
            NpcHitScript::class,
            testBody = {
                player.placeAt(START.translateZ(2))
                testBody()
            },
        )

    private companion object {
        private val START = CoordGrid(0, 50, 50, 0, 0)

        private val dummy =
            npcTypeFactory.create {
                name = "Dummy"
                size = 1
                hitpoints = 50
                defaultMode = NpcMode.None
            }

        private val immuneDummy =
            npcTypeFactory.create {
                name = "Immune dummy"
                size = 1
                hitpoints = 50
                defaultMode = NpcMode.None
                val id = params.poison_immunity.id
                paramMap = ParamMap(mapOf(id to 1), mapOf(id to 1))
            }
    }
}
