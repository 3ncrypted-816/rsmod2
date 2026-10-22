package org.runerealm.content.other.consumables

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.stat
import org.runerealm.api.player.stat.statBase
import org.runerealm.api.player.stat.statBoost
import org.runerealm.api.player.stat.statHeal
import org.runerealm.api.player.stat.statSub
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.script.onOpHeld1
import org.runerealm.api.script.onOpHeld2
import org.runerealm.api.script.onPlayerLogin
import org.runerealm.api.script.onPlayerSoftTimer
import org.runerealm.content.other.consumables.configs.SIMPLE_FOOD_HEALS
import org.runerealm.content.other.consumables.configs.consumable_objs
import org.runerealm.content.other.consumables.configs.consumable_seqs
import org.runerealm.content.other.consumables.configs.consumable_timers
import org.runerealm.content.other.consumables.configs.consumable_varbits
import org.runerealm.content.other.consumables.configs.consumable_varps
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.UnpackedObjType
import org.runerealm.game.type.stat.StatType
import org.runerealm.game.type.stat.StatTypeList
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Eating food and drinking potions, following the OSRS Wiki:
 * - Most food delays the next bite and the next attack by 3 ticks. Cooked karambwan can be eaten in
 *   the same tick as other food and delays the next attack by 2 ticks.
 * - Potions have their own 3-tick delay and do not delay attacks.
 */
class ConsumablesScript
@Inject
constructor(private val objTypes: ObjTypeList, private val statTypes: StatTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val foods = MULTI_BITE_FOODS.associateBy { it.type.id }.toMutableMap()
        for (type in objTypes.values) {
            val heal = SIMPLE_FOOD_HEALS[type.internalName] ?: continue
            foods.putIfAbsent(type.id, Food(type, heal))
        }
        for (food in foods.values) {
            val type = objTypes[food.type] ?: continue
            val op = type.eatOp() ?: continue
            if (op == 1) {
                onOpHeld1(food.type) { eat(it.slot, food) }
            } else {
                onOpHeld2(food.type) { eat(it.slot, food) }
            }
        }
        for (potion in POTIONS) {
            for ((index, dose) in potion.doses.withIndex()) {
                onOpHeld1(dose) { drink(it.slot, potion, index) }
            }
        }
        onPlayerSoftTimer(consumable_timers.stamina) { player.tickStamina() }
        onPlayerSoftTimer(consumable_timers.antifire) { player.tickAntifire() }
        onPlayerSoftTimer(consumable_timers.super_antifire) { player.tickSuperAntifire() }
        onPlayerLogin { player.resumeTimedEffects() }
    }

    private fun UnpackedObjType.eatOp(): Int? {
        val index = iop.indexOfFirst { it == "Eat" }
        return if (index in 0..1) index + 1 else null
    }

    private fun ProtectedAccess.eat(slot: Int, food: Food) {
        val delayVar = if (food.combo) consumable_varps.combo_delay else consumable_varps.food_delay
        if (player.vars[delayVar] > mapClock) {
            return
        }
        val obj = player.inv[slot] ?: return
        if (obj.id != food.type.id) {
            return
        }

        val replaced =
            if (food.next != null) {
                invReplaceSlot(player.inv, slot, 1, food.next).success
            } else {
                invDel(player.inv, food.type, count = 1, slot = slot).success
            }
        if (!replaced) {
            return
        }

        player.foodDelay = mapClock + FOOD_DELAY
        if (food.combo) {
            player.comboDelay = mapClock + FOOD_DELAY
        }
        val attackDelay = if (food.combo) COMBO_FOOD_ATTACK_DELAY else FOOD_DELAY
        player.actionDelay = max(player.actionDelay, mapClock) + attackDelay

        anim(consumable_seqs.eat)
        val name = objTypes[food.type]?.name?.lowercase() ?: "food"
        mes(food.message ?: "You eat the $name.")

        val before = player.stat(stats.hitpoints)
        player.restoreStat(stats.hitpoints, food.heal, 0)
        applyEffects(food.effects)
        if (player.stat(stats.hitpoints) > before) {
            mes("It heals some health.")
        }
    }

    private fun ProtectedAccess.drink(slot: Int, potion: Potion, doseIndex: Int) {
        if (player.potionDelay > mapClock) {
            return
        }
        val obj = player.inv[slot] ?: return
        if (obj.id != potion.doses[doseIndex].id) {
            return
        }
        val damage = potion.effects.filterIsInstance<ConsumeEffect.Damage>().sumOf { it.hitpoints }
        if (damage > 0 && player.stat(stats.hitpoints) <= damage) {
            mes("You need more than $damage Hitpoints to survive the effects of this potion.")
            return
        }
        val next = potion.doses.getOrNull(doseIndex + 1) ?: consumable_objs.vial_empty
        if (!invReplaceSlot(player.inv, slot, 1, next).success) {
            return
        }
        player.potionDelay = mapClock + POTION_DELAY

        anim(consumable_seqs.eat)
        mes("You drink some of your ${potion.name}.")
        applyEffects(potion.effects)

        when (val remaining = potion.doses.size - doseIndex - 1) {
            0 -> mes("You have finished your potion.")
            1 -> mes("You have 1 dose of potion left.")
            else -> mes("You have $remaining doses of potion left.")
        }
    }

    private fun ProtectedAccess.applyEffects(effects: List<ConsumeEffect>) {
        for (effect in effects) {
            when (effect) {
                is ConsumeEffect.Boost ->
                    player.statBoost(effect.stat, effect.constant, effect.percent)
                is ConsumeEffect.Restore ->
                    player.restoreStat(effect.stat, effect.constant, effect.percent)
                is ConsumeEffect.RestoreAllExceptHitpoints ->
                    for (stat in statTypes.values) {
                        if (stat.id != stats.hitpoints.id) {
                            player.restoreStat(stat, effect.constant, effect.percent)
                        }
                    }
                is ConsumeEffect.DrainCurrent -> {
                    val current = player.stat(effect.stat)
                    val drain = effect.constant + current * effect.percent / 100
                    player.statSub(effect.stat, drain, 0)
                }
                is ConsumeEffect.RunEnergy -> {
                    val restore = MAX_RUN_ENERGY * effect.percent / 100
                    player.runEnergy = min(MAX_RUN_ENERGY, player.runEnergy + restore)
                }
                is ConsumeEffect.Stamina -> {
                    vars[varbits.stamina_active] = 1
                    vars[varbits.stamina_duration] =
                        min(MAX_EFFECT_UNITS, player.vars[varbits.stamina_duration] + effect.units)
                    player.softTimer(consumable_timers.stamina, STAMINA_TICK)
                }
                is ConsumeEffect.Antifire -> {
                    vars[varbits.antifire] = min(MAX_EFFECT_UNITS, effect.units)
                    player.softTimer(consumable_timers.antifire, ANTIFIRE_TICK)
                }
                is ConsumeEffect.SuperAntifire -> {
                    vars[varbits.super_antifire] = min(MAX_EFFECT_UNITS, effect.units)
                    player.softTimer(consumable_timers.super_antifire, SUPER_ANTIFIRE_TICK)
                }
                is ConsumeEffect.Damage -> player.statSub(stats.hitpoints, effect.hitpoints, 0)
            }
        }
    }

    /** Heals [stat] towards its base level; a boosted stat is left unchanged. */
    private fun Player.restoreStat(stat: StatType, constant: Int, percent: Int) {
        if (stat(stat) < statBase(stat)) {
            statHeal(stat, constant, percent)
        }
    }
}

private var Player.foodDelay by intVarp(consumable_varps.food_delay)
private var Player.potionDelay by intVarp(consumable_varps.potion_delay)
private var Player.comboDelay by intVarp(consumable_varps.combo_delay)

private val varbits = consumable_varbits

private fun Player.tickStamina() {
    val left = this.vars[varbits.stamina_duration] - 1
    if (left > 0) {
        VarPlayerIntMapSetter.set(this, varbits.stamina_duration, left)
        if (left == STAMINA_WARNING_UNITS) {
            mes("Your stamina potion is about to expire.")
        }
        return
    }
    VarPlayerIntMapSetter.set(this, varbits.stamina_duration, 0)
    VarPlayerIntMapSetter.set(this, varbits.stamina_active, 0)
    clearSoftTimer(consumable_timers.stamina)
    mes("Your stamina potion has expired.")
}

private fun Player.tickAntifire() {
    val left = this.vars[varbits.antifire] - 1
    VarPlayerIntMapSetter.set(this, varbits.antifire, maxOf(0, left))
    if (left == ANTIFIRE_WARNING_UNITS) {
        mes("Your antifire potion is about to expire.")
    }
    if (left <= 0) {
        clearSoftTimer(consumable_timers.antifire)
        mes("Your antifire potion has expired.")
    }
}

private fun Player.tickSuperAntifire() {
    val left = this.vars[varbits.super_antifire] - 1
    VarPlayerIntMapSetter.set(this, varbits.super_antifire, maxOf(0, left))
    if (left == SUPER_ANTIFIRE_WARNING_UNITS) {
        mes("Your super antifire potion is about to expire.")
    }
    if (left <= 0) {
        clearSoftTimer(consumable_timers.super_antifire)
        mes("Your super antifire potion has expired.")
    }
}

/** Soft timers do not survive a logout; restart them for effects saved in their varbits. */
private fun Player.resumeTimedEffects() {
    if (this.vars[varbits.stamina_duration] > 0) {
        softTimer(consumable_timers.stamina, STAMINA_TICK)
    } else {
        VarPlayerIntMapSetter.set(this, varbits.stamina_active, 0)
    }
    if (this.vars[varbits.antifire] > 0) {
        softTimer(consumable_timers.antifire, ANTIFIRE_TICK)
    }
    if (this.vars[varbits.super_antifire] > 0) {
        softTimer(consumable_timers.super_antifire, SUPER_ANTIFIRE_TICK)
    }
}

private const val STAMINA_TICK = 10
private const val ANTIFIRE_TICK = 30
private const val SUPER_ANTIFIRE_TICK = 20
private const val MAX_EFFECT_UNITS = 63
private const val STAMINA_WARNING_UNITS = 3
private const val ANTIFIRE_WARNING_UNITS = 1
private const val SUPER_ANTIFIRE_WARNING_UNITS = 1
