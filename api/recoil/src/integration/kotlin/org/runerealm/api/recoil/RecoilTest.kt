package org.runerealm.api.recoil

import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.stats
import org.runerealm.api.hit.plugin.NpcHitScript
import org.runerealm.api.hit.plugin.PlayerHitScript
import org.runerealm.api.inv.HeldUOpScript
import org.runerealm.api.player.hit.queueHit
import org.runerealm.api.player.ring
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.npc.NpcMode
import org.runerealm.game.hit.HitType
import org.runerealm.game.inv.InvObj
import org.runerealm.map.CoordGrid

class RecoilTest {
    @Test
    fun GameTestState.`ring of recoil deals 10 percent plus 1 back to the attacker`() =
        runRecoilTest { npc ->
            player.ring = InvObj(recoil_objs.ring_of_recoil)
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 25)
            advance(ticks = 3)
            assertEquals(97, npc.hitpoints)
            assertEquals(RECOIL_RING_CHARGES - 3, player.recoilChargesLeft)
        }

    @Test
    fun GameTestState.`ring of recoil shatters after 40 damage`() = runRecoilTest { npc ->
        player.ring = InvObj(recoil_objs.ring_of_recoil)
        player.recoilDamageDealt = 38
        player.queueHit(npc, delay = 1, HitType.Typeless, damage = 25)
        advance()
        assertMessageSent("Your Ring of Recoil has shattered.")
        assertNull(player.ring)
        assertEquals(RECOIL_RING_CHARGES, player.recoilChargesLeft)
        advance(ticks = 2)
        // Only the 2 remaining charges are dealt.
        assertEquals(98, npc.hitpoints)
    }

    @Test
    fun GameTestState.`no recoil when the attack deals no damage`() = runRecoilTest { npc ->
        player.ring = InvObj(recoil_objs.ring_of_recoil)
        player.queueHit(npc, delay = 1, HitType.Typeless, damage = 0)
        advance(ticks = 3)
        assertEquals(100, npc.hitpoints)
        assertEquals(RECOIL_RING_CHARGES, player.recoilChargesLeft)
    }

    @Test
    fun GameTestState.`charged ring of suffering recoils and reverts once empty`() =
        runRecoilTest { npc ->
            player.ring = InvObj(recoil_objs.suffering_ri)
            player.sufferingCharges = 4
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 50)
            advance(ticks = 3)
            assertEquals(96, npc.hitpoints)
            assertEquals(0, player.sufferingCharges)
            assertEquals(recoil_objs.suffering_i.id, player.ring?.id)
        }

    @Test
    fun GameTestState.`rings of recoil charge a ring of suffering`() = runRecoilTest { _ ->
        player.inv[0] = InvObj(recoil_objs.ring_of_recoil)
        player.inv[1] = InvObj(recoil_objs.suffering)
        player.opHeldU(0, 1)
        advance()
        assertMessageSent("You charge your ring of suffering. It now has 40 charges.")
        assertEquals(RECOIL_RING_CHARGES, player.sufferingCharges)
        assertEquals(recoil_objs.suffering_r.id, player.inv[1]?.id)
        assertNull(player.inv[0])
    }

    private fun GameTestState.runRecoilTest(testBody: GameTestScope.(Npc) -> Unit) =
        runGameTest(
            RecoilScript::class,
            PlayerHitScript::class,
            NpcHitScript::class,
            HeldUOpScript::class,
            testBody = {
                val npc = spawnNpc(START, dummy)
                player.placeAt(START.translateZ(1))
                player.stats[stats.hitpoints] = 99
                player.recoilDamageDealt = 0
                player.sufferingCharges = 0
                testBody(npc)
            },
        )

    private companion object {
        private val START = CoordGrid(0, 50, 50, 0, 0)

        private val dummy =
            npcTypeFactory.create {
                name = "Dummy"
                size = 1
                hitpoints = 100
                defaultMode = NpcMode.None
            }
    }
}
