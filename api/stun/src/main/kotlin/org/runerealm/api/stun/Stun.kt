package org.runerealm.api.stun

import org.runerealm.api.config.refs.spotanims
import org.runerealm.api.npc.vars.intVarn
import org.runerealm.api.player.output.clearMapFlag
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.type.builders.varn.VarnBuilder
import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.varn.VarnReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.PathingEntity
import org.runerealm.game.entity.Player
import org.runerealm.game.type.varn.VarnType
import org.runerealm.game.type.varp.VarpType

/*
 * [Stun](https://oldschool.runescape.wiki/w/Stun_(status)): a stunned entity cannot move or perform
 * actions (attack, interact, pickpocket again) for the duration of the stun, shown by the circling
 * birds ("stunned") spotanim above its head.
 *
 * A stun is applied as an engine delay: delayed players ignore movement and interaction input and
 * do not process their queues and normal timers, and delayed npcs skip their whole ai cycle. The
 * entity's current route and interaction are dropped, so it does not resume them after the stun.
 */

/** The message players receive when stunned. */
public const val STUN_MESSAGE: String = "You have been stunned."

/** The stun spotanim height used above a player's head. */
public const val STUN_SPOTANIM_HEIGHT: Int = 100

private var Player.stunEnd by intVarp(stun_varps.stun_end)
private var Npc.stunEnd by intVarn(stun_varns.stun_end)

/** @return `true` if the entity is currently stunned. */
public fun PathingEntity.isStunned(): Boolean {
    val end = stunEndClock
    return end != 0 && currentMapClock < end
}

/** @return the number of cycles until the entity's stun ends, or `0` if it is not stunned. */
public fun PathingEntity.remainingStunCycles(): Int =
    if (isStunned()) stunEndClock - currentMapClock else 0

/**
 * Stuns this player for [cycles] game cycles: they cannot move or act until it runs out. Plays the
 * stun spotanim and, when [message] is not `null`, sends it to the player.
 *
 * A stun does not stack with or extend an active stun.
 *
 * @return `true` if the stun was applied.
 */
public fun Player.stun(cycles: Int, message: String? = STUN_MESSAGE): Boolean {
    require(cycles > 0) { "`cycles` must be greater than 0. (cycles=$cycles)" }
    if (isStunned()) {
        return false
    }
    stunEnd = currentMapClock + cycles
    delay(cycles)
    abortRoute()
    clearInteraction()
    clearMapFlag()
    spotanim(spotanims.stunned, height = STUN_SPOTANIM_HEIGHT)
    message?.let(::mes)
    return true
}

/**
 * Stuns this npc for [cycles] game cycles: it does not move, attack or run its ai until the stun
 * runs out.
 *
 * @return `true` if the stun was applied.
 */
public fun Npc.stun(cycles: Int, spotanimHeight: Int = STUN_SPOTANIM_HEIGHT): Boolean {
    require(cycles > 0) { "`cycles` must be greater than 0. (cycles=$cycles)" }
    if (isStunned()) {
        return false
    }
    stunEnd = currentMapClock + cycles
    delay(cycles)
    abortRoute()
    spotanim(spotanims.stunned, height = spotanimHeight)
    return true
}

/**
 * Ends the entity's stun immediately, e.g., on death. The engine delay is only cleared if it was
 * set by the stun.
 */
public fun PathingEntity.clearStun() {
    if (isStunned() && delay == stunEndClock) {
        delay = currentMapClock
    }
    when (this) {
        is Player -> stunEnd = 0
        is Npc -> stunEnd = 0
    }
}

private val PathingEntity.stunEndClock: Int
    get() =
        when (this) {
            is Player -> stunEnd
            is Npc -> stunEnd
        }

internal object stun_varps : VarpReferences() {
    val stun_end: VarpType = find("stun_end")
}

internal object stun_varns : VarnReferences() {
    val stun_end: VarnType = find("npc_stun_end")
}

internal object StunVarpBuilds : VarpBuilder() {
    init {
        build("stun_end") { temporary = true }
    }
}

internal object StunVarnBuilds : VarnBuilder() {
    init {
        build("npc_stun_end")
    }
}
