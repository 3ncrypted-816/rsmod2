package org.runerealm.api.spells.charge

import org.runerealm.api.config.refs.timers
import org.runerealm.api.config.refs.varps
import org.runerealm.api.player.vars.intVarp
import org.runerealm.game.entity.Player

/*
 * The Charge spell, following the OSRS Wiki (https://oldschool.runescape.wiki/w/Charge):
 * - For 7 minutes (700 ticks), god spells deal up to 30 damage instead of 20, as long as the
 *   matching god cape is worn.
 * - Charge can only be cast once every 60 seconds.
 */

/** How long a Charge lasts: 7 minutes. */
public const val CHARGE_DURATION_CYCLES: Int = 700

/** Charge can only be recast this many cycles after the previous cast: 60 seconds. */
public const val CHARGE_RECAST_CYCLES: Int = 100

private var Player.chargeEnd by intVarp(varps.charge_spell_end)

/** @return `true` while the player's Charge spell is active. */
public fun Player.isChargeActive(): Boolean {
    val end = chargeEnd
    return end != 0 && currentMapClock < end
}

/** @return `true` if the player cast Charge too recently to cast it again. */
public fun Player.isChargeOnCooldown(): Boolean {
    val end = chargeEnd
    if (end == 0) {
        return false
    }
    val castClock = end - CHARGE_DURATION_CYCLES
    return currentMapClock < castClock + CHARGE_RECAST_CYCLES
}

/** Starts (or restarts) the player's Charge spell. */
public fun Player.activateCharge() {
    chargeEnd = currentMapClock + CHARGE_DURATION_CYCLES
    softTimer(timers.charge_spell, CHARGE_DURATION_CYCLES)
}

/** Ends the player's Charge spell. */
public fun Player.clearCharge() {
    chargeEnd = 0
    clearSoftTimer(timers.charge_spell)
}
