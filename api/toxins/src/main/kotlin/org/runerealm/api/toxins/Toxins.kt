package org.runerealm.api.toxins

import kotlin.math.min
import org.runerealm.api.config.constants
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.timers
import org.runerealm.api.npc.vars.intVarn
import org.runerealm.api.npc.vars.typePlayerUidVarn
import org.runerealm.api.player.hat
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.vars.intVarp
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.entity.PlayerList

/*
 * Poison and venom ("toxins"), following the OSRS Wiki
 * [Poison](https://oldschool.runescape.wiki/w/Poison) and
 * [Venom](https://oldschool.runescape.wiki/w/Venom) pages.
 *
 * A player's state lives in the client's `poison` varp (102), which the hitpoints orb reads. An
 * npc's state lives in the `npc_toxin` varn with the same encoding:
 * - `0`: healthy.
 * - `1 until VENOM_THRESHOLD`: poisoned. The value is the poison "severity": each poison hit deals
 *   `ceil(severity / 5)` damage and lowers the severity by one, so the damage drops by 1 every 5
 *   hits until the poison wears off.
 * - `VENOM_THRESHOLD` and above: envenomed. Each venom hit deals `(value - 999,997) * 2` damage
 *   (6, 8, 10, ... capped at 20) and raises the value by one.
 * - Negative: poison immunity, in poison intervals (30 ticks). Below `VENOM_IMMUNITY_LIMIT` the
 *   player is immune to venom as well. Each interval raises the value by one.
 *
 * Every non-zero state is driven by the `toxins` timer, which fires every [TOXIN_INTERVAL] ticks.
 * The player's timer is a normal (non-soft) timer, so, as on the wiki, poison neither damages nor
 * wears off while an interface is open, and hits once the interface closes.
 */

/** Ticks between poison/venom hits (and between poison immunity decrements): 18 seconds. */
public const val TOXIN_INTERVAL: Int = 30

/** Toxin values at or above this are venom rather than poison. */
public const val VENOM_THRESHOLD: Int = 1_000_000

/** Toxin values below this mean the player is immune to venom as well as poison. */
public const val VENOM_IMMUNITY_LIMIT: Int = -38

/** Venom stops increasing in damage once it reaches this many hitpoints per hit. */
public const val MAX_VENOM_DAMAGE: Int = 20

/**
 * Poison severity of venom converted to poison, for example on an npc that is immune to venom but
 * not poison (wiki: severity 26, starting at 6 damage).
 */
public const val CONVERTED_VENOM_SEVERITY: Int = 26

private const val POISON_MESSAGE = "You have been poisoned!"

/** Helpers for the shared toxin value encoding. */
public object ToxinValues {
    public fun isPoison(value: Int): Boolean = value in 1 until VENOM_THRESHOLD

    public fun isVenom(value: Int): Boolean = value >= VENOM_THRESHOLD

    /** @return the damage the next poison or venom hit deals for [value], or `0`. */
    public fun damage(value: Int): Int =
        when {
            isVenom(value) -> min(MAX_VENOM_DAMAGE, (value - (VENOM_THRESHOLD - 3)) * 2)
            isPoison(value) -> (value + 4) / 5
            else -> 0
        }

    /** @return the value after one toxin interval has passed. */
    public fun next(value: Int): Int =
        when {
            isVenom(value) -> value + 1
            value > 0 -> value - 1
            value < 0 -> value + 1
            else -> 0
        }

    /** @return the poison severity that starts at [damage] damage per hit. */
    public fun severityForDamage(damage: Int): Int = damage * 5
}

/* Players */

internal var Player.toxinValue: Int by intVarp(toxin_varps.poison)

/** @return `true` if the player is poisoned (not envenomed). */
public fun Player.isPoisoned(): Boolean = ToxinValues.isPoison(toxinValue)

/** @return `true` if the player is envenomed. */
public fun Player.isEnvenomed(): Boolean = ToxinValues.isVenom(toxinValue)

/** @return the damage the player's next poison or venom hit deals, or `0`. */
public fun Player.nextToxinDamage(): Int = ToxinValues.damage(toxinValue)

/** @return the remaining poison immunity, in [TOXIN_INTERVAL]s. */
public fun Player.poisonImmunityIntervals(): Int = if (toxinValue < 0) -toxinValue else 0

/** @return `true` if the player cannot currently be poisoned. */
public fun Player.isPoisonImmune(): Boolean = toxinValue < 0 || isWearingSerpentineHelm()

/** @return `true` if the player cannot currently be envenomed. */
public fun Player.isVenomImmune(): Boolean =
    toxinValue < VENOM_IMMUNITY_LIMIT || isWearingSerpentineHelm()

/**
 * A charged [serpentine helm](https://oldschool.runescape.wiki/w/Serpentine_helm) makes its wearer
 * immune to becoming poisoned or envenomed. It does not cure existing poison or venom.
 */
public fun Player.isWearingSerpentineHelm(): Boolean {
    val hat = hat ?: return false
    return hat.id == toxin_objs.serpentine_helm.id ||
        hat.id == toxin_objs.serpentine_helm_cyan.id ||
        hat.id == toxin_objs.serpentine_helm_red.id
}

/**
 * Poisons this player with the given poison [severity] (the starting damage is `ceil(severity /
 * 5)`; see [ToxinValues.severityForDamage]).
 *
 * Fails if the player is immune, envenomed, or already poisoned at least as severely.
 *
 * @return `true` if the poison was applied.
 */
public fun Player.poison(severity: Int): Boolean {
    if (severity <= 0 || isPoisonImmune() || isEnvenomed()) {
        return false
    }
    val previous = toxinValue
    if (previous >= severity) {
        return false
    }
    setToxinValue(severity, previous)
    mes(POISON_MESSAGE)
    return true
}

/**
 * Envenoms this player. Venom starts at 6 damage and increases by 2 every hit, up to 20. Venom
 * replaces any poison the player has.
 *
 * @return `true` if the venom was applied.
 */
public fun Player.envenom(): Boolean {
    if (isVenomImmune() || isEnvenomed()) {
        return false
    }
    setToxinValue(VENOM_THRESHOLD, toxinValue)
    return true
}

/**
 * Applies an antipoison-style cure (antipoison, superantipoison, antidote+, antidote++):
 * - Venom is converted to poison that starts at the venom's current damage, without immunity.
 * - Otherwise poison is cured and the player becomes immune to poison for [immunityIntervals]
 *   intervals of [TOXIN_INTERVAL] ticks. A longer immunity the player already has is kept.
 */
public fun Player.curePoison(immunityIntervals: Int) {
    val previous = toxinValue
    if (ToxinValues.isVenom(previous)) {
        val damage = ToxinValues.damage(previous)
        setToxinValue(ToxinValues.severityForDamage(damage), previous)
        return
    }
    setToxinValue(immunityValue(previous, immunityIntervals), previous)
}

/**
 * Applies an anti-venom-style cure (anti-venom, anti-venom+): cures both venom and poison, then
 * grants [immunityIntervals] intervals of poison immunity. Immunity values below
 * [VENOM_IMMUNITY_LIMIT] also protect from venom.
 */
public fun Player.cureVenom(immunityIntervals: Int) {
    val previous = toxinValue
    setToxinValue(immunityValue(previous, immunityIntervals), previous)
}

/** Removes any poison, venom and immunity from this player, e.g., on death. */
public fun Player.clearToxins() {
    toxinValue = 0
    clearTimer(timers.toxins)
}

private fun immunityValue(previous: Int, immunityIntervals: Int): Int {
    val current = if (previous < 0) previous else 0
    return min(current, -immunityIntervals)
}

private fun Player.setToxinValue(value: Int, previous: Int) {
    toxinValue = value
    when {
        value == 0 -> clearTimer(timers.toxins)
        // A non-zero previous value means the toxins timer is already running.
        previous == 0 -> timer(timers.toxins, TOXIN_INTERVAL)
    }
}

/* Npcs */

internal var Npc.toxinValue: Int by intVarn(toxin_varns.toxin)
private var Npc.toxinSource by typePlayerUidVarn(toxin_varns.toxin_source)

/** @return `true` if the npc is poisoned (not envenomed). */
public fun Npc.isPoisoned(): Boolean = ToxinValues.isPoison(toxinValue)

/** @return `true` if the npc is envenomed. */
public fun Npc.isEnvenomed(): Boolean = ToxinValues.isVenom(toxinValue)

/** @return the damage the npc's next poison or venom hit deals, or `0`. */
public fun Npc.nextToxinDamage(): Int = ToxinValues.damage(toxinValue)

/** @return `true` if the npc's `poison_immunity` param makes it immune to poison. */
public fun Npc.isPoisonImmune(): Boolean = visType.param(params.poison_immunity) != 0

/**
 * Poisons this npc with the given poison [severity]. [source] is the player credited with the
 * poison damage.
 *
 * Fails if the npc is immune to poison, envenomed, or already poisoned at least as severely.
 *
 * @return `true` if the poison was applied.
 */
public fun Npc.poison(severity: Int, source: Player? = null): Boolean {
    if (severity <= 0 || isPoisonImmune() || isEnvenomed()) {
        return false
    }
    val previous = toxinValue
    if (previous >= severity) {
        return false
    }
    setToxinValue(severity, previous)
    toxinSource = source?.uid
    return true
}

/**
 * Envenoms this npc. Npcs with full venom immunity are unaffected, and npcs with partial venom
 * immunity ("poisons instead") are poisoned with [CONVERTED_VENOM_SEVERITY] instead.
 *
 * @return `true` if venom (or the replacement poison) was applied.
 */
public fun Npc.envenom(source: Player? = null): Boolean {
    when (visType.param(params.venom_immunity)) {
        constants.npc_venom_full_immunity -> return false
        constants.npc_venom_partial_immunity -> return poison(CONVERTED_VENOM_SEVERITY, source)
    }
    if (isEnvenomed()) {
        return false
    }
    setToxinValue(VENOM_THRESHOLD, toxinValue)
    toxinSource = source?.uid
    return true
}

/** Removes any poison or venom from this npc, e.g., on death. */
public fun Npc.clearToxins() {
    toxinValue = 0
    toxinSource = null
    clearTimer(timers.toxins)
}

/** @return the player credited with this npc's poison damage, if they are still online. */
public fun Npc.resolveToxinSource(playerList: PlayerList): Player? =
    toxinSource?.resolve(playerList)

private fun Npc.setToxinValue(value: Int, previous: Int) {
    toxinValue = value
    when {
        value == 0 -> clearTimer(timers.toxins)
        previous == 0 -> timer(timers.toxins, TOXIN_INTERVAL)
    }
}
