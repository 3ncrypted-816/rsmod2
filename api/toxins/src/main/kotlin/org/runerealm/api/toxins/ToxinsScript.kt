package org.runerealm.api.toxins

import jakarta.inject.Inject
import org.runerealm.api.config.refs.hitmark_groups
import org.runerealm.api.config.refs.timers
import org.runerealm.api.npc.events.NpcHitEvents
import org.runerealm.api.npc.hit.modifier.NpcHitModifier
import org.runerealm.api.npc.hit.queueHit
import org.runerealm.api.player.events.PlayerHitEvents
import org.runerealm.api.player.hit.modifier.NoopPlayerHitModifier
import org.runerealm.api.player.hit.queueHit
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.random.GameRandom
import org.runerealm.api.script.onEvent
import org.runerealm.api.script.onNpcTimer
import org.runerealm.api.script.onPlayerLogin
import org.runerealm.api.script.onPlayerTimer
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.PlayerList
import org.runerealm.game.hit.Hit
import org.runerealm.game.hit.HitType
import org.runerealm.game.type.hitmark.HitmarkTypeGroup
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Deals poison and venom damage on the `toxins` timer and applies poison from poisoned weapons. See
 * `Toxins.kt` for the mechanics.
 */
public class ToxinsScript
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val playerList: PlayerList,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerTimer(timers.toxins) { tickToxins() }
        onNpcTimer(timers.toxins) { npc.tickToxins() }
        onPlayerLogin { player.resumeToxins() }
        onEvent<NpcHitEvents.AnyImpact> { applyWeaponPoison(npc, hit) }
        onEvent<PlayerHitEvents.Impact> { applyWeaponPoison(player, hit, damage) }
    }

    private fun ProtectedAccess.tickToxins() {
        val value = player.toxinValue
        val damage = ToxinValues.damage(value)
        if (damage > 0) {
            player.queueHit(
                delay = 1,
                type = HitType.Typeless,
                damage = damage,
                hitmark = hitmarkFor(value),
                modifier = NoopPlayerHitModifier,
            )
        }
        val next = ToxinValues.next(value)
        player.toxinValue = next
        if (next == 0) {
            player.clearTimer(timers.toxins)
        }
    }

    private fun Npc.tickToxins() {
        val value = toxinValue
        if (hitpoints <= 0) {
            return
        }
        val damage = ToxinValues.damage(value)
        if (damage > 0) {
            val hitmark = hitmarkFor(value)
            val source = resolveToxinSource(playerList)
            if (source != null) {
                queueHit(source, 1, HitType.Typeless, damage, NoopNpcHitModifier, hitmark)
            } else {
                queueHit(1, HitType.Typeless, damage, NoopNpcHitModifier, hitmark)
            }
        }
        val next = ToxinValues.next(value)
        toxinValue = next
        if (next <= 0) {
            clearToxins()
        }
    }

    private fun Player.resumeToxins() {
        if (toxinValue != 0) {
            timer(timers.toxins, TOXIN_INTERVAL)
        }
    }

    private fun applyWeaponPoison(target: Npc, hit: Hit) {
        if (!hit.isFromPlayer || hit.damage <= 0) {
            return
        }
        val severity = WeaponPoison.severity(hit, objTypes)
        if (severity <= 0 || random.of(maxExclusive = WeaponPoison.chance(hit)) != 0) {
            return
        }
        target.poison(severity, hit.resolvePlayerSource(playerList))
    }

    private fun applyWeaponPoison(target: Player, hit: Hit, damage: Int) {
        if (!hit.isFromPlayer || damage <= 0) {
            return
        }
        val severity = WeaponPoison.severity(hit, objTypes)
        if (severity <= 0 || random.of(maxExclusive = WeaponPoison.chance(hit)) != 0) {
            return
        }
        target.poison(severity)
    }

    private fun hitmarkFor(value: Int): HitmarkTypeGroup =
        if (ToxinValues.isVenom(value)) hitmark_groups.venom else hitmark_groups.poison_damage

    private companion object {
        /** Poison damage is not reduced by npc hit modifiers such as flat armour. */
        val NoopNpcHitModifier = NpcHitModifier {}
    }
}
