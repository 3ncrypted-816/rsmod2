package org.runerealm.api.stun

import org.junit.jupiter.api.Test
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.game.entity.npc.NpcMode
import org.runerealm.map.CoordGrid

class StunTest {
    @Test
    fun GameTestState.`stunned player cannot move until the stun ends`() = runGameTest {
        player.placeAt(START)
        advance()
        assertTrue(player.stun(cycles = 4))
        assertMessageSent(STUN_MESSAGE)
        assertTrue(player.isStunned())
        assertEquals(4, player.remainingStunCycles())

        // A stun does not stack with an active stun.
        assertFalse(player.stun(cycles = 10))

        player.moveGameClick(START.translateX(3))
        advance()
        assertEquals(START, player.coords)

        advance(ticks = 3)
        assertFalse(player.isStunned())
        player.moveGameClick(START.translateX(3))
        advance()
        assertNotEquals(START, player.coords)
    }

    @Test
    fun GameTestState.`stunned npc does not move until the stun ends`() = runGameTest {
        val npc = spawnNpc(START, dummy)
        advance()
        assertTrue(npc.stun(cycles = 3))
        npc.walk(START.translateX(3))
        advance(ticks = 2)
        assertEquals(START, npc.coords)
        assertTrue(npc.isStunned())

        advance(ticks = 2)
        assertFalse(npc.isStunned())
        npc.walk(START.translateX(3))
        advance()
        assertNotEquals(START, npc.coords)
    }

    @Test
    fun GameTestState.`clearing a stun lets the player act again`() = runGameTest {
        player.placeAt(START)
        advance()
        player.stun(cycles = 10)
        player.clearStun()
        assertFalse(player.isStunned())
        assertFalse(player.isDelayed)
    }

    private companion object {
        private val START = CoordGrid(0, 50, 50, 0, 0)

        private val dummy =
            npcTypeFactory.create {
                name = "Dummy"
                size = 1
                hitpoints = 10
                defaultMode = NpcMode.None
            }
    }
}
