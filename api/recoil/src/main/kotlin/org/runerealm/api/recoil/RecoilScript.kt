package org.runerealm.api.recoil

import jakarta.inject.Inject
import kotlin.math.min
import org.runerealm.api.npc.hit.modifier.NpcHitModifier
import org.runerealm.api.npc.hit.queueHit
import org.runerealm.api.player.events.PlayerHitEvents
import org.runerealm.api.player.hit.modifier.NoopPlayerHitModifier
import org.runerealm.api.player.hit.queueHit
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.ring
import org.runerealm.api.script.onEvent
import org.runerealm.api.script.onOpHeldU
import org.runerealm.api.script.onOpWorn2
import org.runerealm.game.entity.NpcList
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.PlayerList
import org.runerealm.game.hit.Hit
import org.runerealm.game.hit.HitType
import org.runerealm.game.inv.InvObj
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/** Deals recoil damage and handles recoil ring charges. See `Recoil.kt` for the mechanics. */
public class RecoilScript
@Inject
constructor(
    private val npcList: NpcList,
    private val playerList: PlayerList,
    private val npcHitModifier: NpcHitModifier,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayerHitEvents.Impact> { player.recoil(hit, damage) }

        onOpWorn2(recoil_objs.ring_of_recoil) { checkRecoilRing() }

        for (ring in SufferingRing.entries) {
            onOpHeldU(recoil_objs.ring_of_recoil, ring.uncharged) {
                chargeSuffering(it.firstSlot, it.secondSlot, ring)
            }
            onOpHeldU(recoil_objs.ring_of_recoil, ring.charged) {
                chargeSuffering(it.firstSlot, it.secondSlot, ring)
            }
        }
    }

    private fun Player.recoil(hit: Hit, damage: Int) {
        if (damage <= 0 || !(hit.isFromNpc || hit.isFromPlayer)) {
            return
        }
        val ring = ring ?: return
        if (ring.id == recoil_objs.ring_of_recoil.id) {
            val recoil = min(recoilDamage(damage), recoilChargesLeft)
            if (recoil <= 0) {
                return
            }
            dealRecoil(hit, recoil)
            recoilDamageDealt += recoil
            if (recoilChargesLeft <= 0) {
                shatterRecoilRing()
            }
            return
        }
        val suffering = SufferingRing.byCharged(ring.id) ?: return
        val recoil = min(recoilDamage(damage), sufferingCharges)
        if (recoil <= 0) {
            return
        }
        dealRecoil(hit, recoil)
        sufferingCharges -= recoil
        if (sufferingCharges <= 0) {
            this.ring = InvObj(suffering.uncharged)
        }
    }

    private fun Player.dealRecoil(hit: Hit, recoil: Int) {
        if (hit.isFromNpc) {
            val npc = hit.resolveNpcSource(npcList) ?: return
            if (npc.hitpoints <= 0) {
                return
            }
            npc.queueHit(this, 1, HitType.Typeless, recoil, npcHitModifier)
            return
        }
        val source = hit.resolvePlayerSource(playerList) ?: return
        // Recoil damage has no source, so it never triggers the attacker's own recoil.
        source.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = recoil,
            modifier = NoopPlayerHitModifier,
            strongQueue = false,
        )
    }

    private fun Player.shatterRecoilRing() {
        ring = null
        recoilDamageDealt = 0
        mes(SHATTER_MESSAGE)
    }

    private fun ProtectedAccess.checkRecoilRing() {
        val left = player.recoilChargesLeft
        mes("You can inflict $left more points of damage before a ring will shatter.")
    }

    private fun ProtectedAccess.chargeSuffering(recoilSlot: Int, ringSlot: Int, ring: SufferingRing) {
        if (player.sufferingCharges >= SUFFERING_MAX_CHARGES) {
            mes("Your ring of suffering cannot hold any more charges.")
            return
        }
        val recoil = player.inv[recoilSlot] ?: return
        if (recoil.id != recoil_objs.ring_of_recoil.id) {
            return
        }
        if (!invDel(player.inv, recoil_objs.ring_of_recoil, count = 1, slot = recoilSlot).success) {
            return
        }
        player.addSufferingCharges(RECOIL_RING_CHARGES)
        val current = player.inv[ringSlot]
        if (current != null && current.id == ring.uncharged.id) {
            invReplaceSlot(player.inv, ringSlot, 1, ring.charged)
        }
        mes("You charge your ring of suffering. It now has ${player.sufferingCharges} charges.")
    }

    private companion object {
        const val SHATTER_MESSAGE = "Your Ring of Recoil has shattered."
    }
}
