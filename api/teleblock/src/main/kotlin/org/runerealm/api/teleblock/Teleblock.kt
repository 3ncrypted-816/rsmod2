package org.runerealm.api.teleblock

import org.runerealm.api.area.checker.WildernessArea
import org.runerealm.api.config.refs.timers
import org.runerealm.api.config.refs.varps
import org.runerealm.api.player.vars.intVarp
import org.runerealm.game.entity.Player

/*
 * Tele Block, following the OSRS Wiki (https://oldschool.runescape.wiki/w/Tele_Block):
 * - A Tele Block lasts 5 minutes (500 cycles), or half that if the target had Protect from Magic
 *   active when it landed.
 * - While Tele Blocked, the player cannot use most forms of teleportation.
 * - Once Tele Blocked, the player is immune to further attempts until the current one expires.
 * - Leaving the Wilderness (by any means, including death) or logging out removes it.
 */

/** The default Tele Block duration: 5 minutes. */
public const val TELEBLOCK_CYCLES: Int = 500

/** The Tele Block duration when the target had Protect from Magic active: 2 minutes 30 seconds. */
public const val TELEBLOCK_PROTECTED_CYCLES: Int = 250

/** How often the Tele Block timer checks whether the player has left the Wilderness. */
internal const val TELEBLOCK_CHECK_INTERVAL: Int = 1

private var Player.teleblockEnd by intVarp(varps.teleblock_end)

/** @return `true` if the player is currently Tele Blocked. */
public fun Player.isTeleblocked(): Boolean {
    val end = teleblockEnd
    // `0` means "never set"; a player that has not been processed yet has a map clock of `-1`.
    return end != 0 && currentMapClock < end
}

/** @return the number of cycles left on the player's Tele Block, or `0` if there is none. */
public fun Player.remainingTeleblockCycles(): Int =
    if (isTeleblocked()) teleblockEnd - currentMapClock else 0

/**
 * Tele Blocks the player for [cycles] game cycles.
 *
 * @return `false` (and changes nothing) if the player is already Tele Blocked.
 */
public fun Player.teleblock(cycles: Int): Boolean {
    require(cycles > 0) { "`cycles` must be greater than 0. (cycles=$cycles)" }
    if (isTeleblocked()) {
        return false
    }
    teleblockEnd = currentMapClock + cycles
    softTimer(timers.teleblock, TELEBLOCK_CHECK_INTERVAL)
    return true
}

/** Removes the player's Tele Block, if any. */
public fun Player.clearTeleblock() {
    teleblockEnd = 0
    clearSoftTimer(timers.teleblock)
}

/**
 * Called by [TeleblockScript] on each timer cycle: the Tele Block ends early once the player is no
 * longer in the Wilderness.
 *
 * @return `true` if the Tele Block is still active.
 */
internal fun Player.processTeleblock(): Boolean {
    if (!isTeleblocked()) {
        clearTeleblock()
        return false
    }
    if (!WildernessArea.contains(coords)) {
        clearTeleblock()
        return false
    }
    return true
}

/**
 * The message shown when a Tele Blocked player tries to teleport, e.g. "A Tele Block spell has been
 * cast on you. It will expire in 4 minutes, 30 seconds."
 */
public fun Player.teleblockedMessage(): String =
    "A Tele Block spell has been cast on you. It will expire in " +
        "${formatTeleblockTime(remainingTeleblockCycles())}."

/** Formats a number of game cycles (0.6 seconds each) as e.g. "4 minutes, 30 seconds". */
public fun formatTeleblockTime(cycles: Int): String {
    val totalSeconds = (cycles * 6 + 9) / 10
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val parts = buildList {
        if (minutes > 0) add(if (minutes == 1) "1 minute" else "$minutes minutes")
        if (seconds > 0 || minutes == 0) add(if (seconds == 1) "1 second" else "$seconds seconds")
    }
    return parts.joinToString(", ")
}
