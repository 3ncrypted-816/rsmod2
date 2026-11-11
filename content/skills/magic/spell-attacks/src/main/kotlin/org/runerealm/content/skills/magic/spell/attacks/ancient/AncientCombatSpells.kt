package org.runerealm.content.skills.magic.spell.attacks.ancient

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import org.runerealm.api.area.checker.AreaChecker
import org.runerealm.api.area.checker.WildernessArea
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.manager.MagicRuneManager
import org.runerealm.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.runerealm.api.config.refs.areas
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.projanims
import org.runerealm.api.config.refs.stats
import org.runerealm.api.freeze.freeze
import org.runerealm.api.hunt.Hunt
import org.runerealm.api.npc.isValidTarget
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.baseHitpointsLvl
import org.runerealm.api.player.stat.hitpoints
import org.runerealm.api.player.stat.statHeal
import org.runerealm.api.random.GameRandom
import org.runerealm.api.spells.attack.SpellAttack
import org.runerealm.api.spells.attack.SpellAttackManager
import org.runerealm.api.spells.attack.SpellAttackMap
import org.runerealm.api.spells.attack.SpellAttackRepository
import org.runerealm.api.toxins.poison
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.api.type.refs.synth.SynthReferences
import org.runerealm.content.skills.magic.spell.attacks.standard.StandardCombatSpells.StatDrains
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.PathingEntity
import org.runerealm.game.entity.Player
import org.runerealm.game.type.hunt.HuntVis
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.game.type.synth.SynthType

/**
 * The [Ancient Magicks](https://oldschool.runescape.wiki/w/Ancient_Magicks) combat spells: the
 * Smoke, Shadow, Blood and Ice spells, each in a rush, burst, blitz and barrage tier.
 *
 * Runes, levels and cast experience come from the spell objs in the cache. Per the wiki:
 * - Base max hits are 13/17/23/27 (Smoke), 14/18/24/28 (Shadow), 15/21/25/29 (Blood) and
 *   16/22/26/30 (Ice) for rush/burst/blitz/barrage.
 * - **Smoke** spells have a 1/8 chance on hit to poison the target: severity 10 (2 damage) for rush
 *   and burst, 20 (4 damage) for blitz and barrage.
 * - **Shadow** spells lower the target's Attack by 10% (rush, burst) or 15% (blitz, barrage),
 *   unless it is already lowered.
 * - **Blood** spells heal the caster by 25% of the damage they deal (up to the caster's base
 *   Hitpoints).
 * - **Ice** spells freeze the target for 8, 16, 24 or 32 ticks (4.8 to 19.2 seconds), using the
 *   shared [freeze] mechanic. They can still damage an already frozen target.
 * - **Burst** and **barrage** spells also hit everything attackable in the 3x3 area around the
 *   primary target, up to 9 targets in total, but only in multi-combat areas. Each target gets its
 *   own accuracy and damage roll and the spell's effect.
 *
 * Effects are applied when the spell is cast (like [Bind][org.runerealm.api.freeze.freeze] in the
 * standard book), not when the projectile lands. The ancient sceptre bonuses are not modelled.
 */
class AncientCombatSpells
@Inject
constructor(
    private val random: GameRandom,
    private val hunt: Hunt,
    private val areaChecker: AreaChecker,
) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (spell in ANCIENT_COMBAT_SPELLS) {
            register(spell.obj, AncientSpellAttack(manager, spell))
        }
    }

    private inner class AncientSpellAttack(
        private val manager: SpellAttackManager,
        private val spell: AncientSpell,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            val fx = spell.fx()
            anim(if (spell.tier.multiTarget) ancient_seqs.cast_area else ancient_seqs.cast)
            fx.castSound?.let { soundSynth(it) }

            val primaryDelay = hitDelays(target, fx.travel)
            var totalDamage = hitTarget(target, attack, castResult, fx, primaryDelay)

            if (spell.tier.multiTarget && isMultiway(target)) {
                for (other in secondaryTargets(target)) {
                    val delays = hitDelays(other, travel = null)
                    totalDamage += hitTarget(other, attack, castResult, fx, delays)
                }
            }

            if (spell.element == AncientElement.Blood && totalDamage > 0) {
                healFromBlood(player, totalDamage)
            }
            manager.continueCombatIfAutocast(this, target)
        }

        /** @return the damage dealt to [target] (0 on a splash). */
        private fun ProtectedAccess.hitTarget(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            castResult: MagicRuneManager.CastResult,
            fx: AncientFx,
            delays: Delays,
        ): Int {
            val obj = attack.spell.obj
            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, delays.client, null, SOUND_RADIUS)
                manager.queueSplashHit(this, target, obj, delays.client, delays.server)
                return 0
            }
            val damage = manager.rollMaxHit(this, target, attack, castResult, spell.maxHit)
            applyEffect(target)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = delays.client,
                castSound = null,
                soundRadius = SOUND_RADIUS,
                hitSpot = fx.impact,
                hitSpotHeight = 0,
                hitSound = fx.hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, obj, damage, delays.client, delays.server)
            return min(damage, target.currentHitpoints())
        }

        private fun ProtectedAccess.applyEffect(target: PathingEntity) {
            when (spell.element) {
                AncientElement.Smoke -> {
                    if (random.of(maxExclusive = SMOKE_POISON_CHANCE) != 0) {
                        return
                    }
                    val severity = spell.tier.smokePoisonSeverity
                    when (target) {
                        is Npc -> target.poison(severity, player)
                        is Player -> target.poison(severity)
                    }
                }
                AncientElement.Shadow -> {
                    if (!StatDrains.isLowered(target, stats.attack)) {
                        StatDrains.drainPercent(target, stats.attack, spell.tier.shadowDrainPercent)
                    }
                }
                AncientElement.Blood -> Unit
                AncientElement.Ice -> {
                    val source = player.takeIf { target is Player }
                    target.freeze(spell.tier.iceFreezeCycles, random, source)
                }
            }
        }

        private fun ProtectedAccess.hitDelays(
            target: PathingEntity,
            travel: SpotanimType?,
        ): Delays {
            if (travel != null) {
                val proj = manager.spawnProjectile(this, target, travel, projanims.magic_spell)
                val (server, client) = proj.durations
                return Delays(server = server, client = client)
            }
            // Spells without a projectile use the standard magic hit delay:
            // 1 + floor((1 + distance) / 3) ticks.
            val distance = player.coords.chebyshevDistance(target.coords)
            val ticks = 1 + (1 + distance) / 3
            return Delays(server = ticks, client = (ticks - 1) * CLIENT_CYCLES_PER_TICK)
        }

        private fun ProtectedAccess.isMultiway(target: PathingEntity): Boolean =
            areaChecker.inArea(areas.multiway, target.coords)

        private fun ProtectedAccess.secondaryTargets(primary: PathingEntity): List<PathingEntity> {
            val found = mutableListOf<PathingEntity>()
            val center = primary.coords
            for (npc in hunt.findNpcs(center, AREA_RADIUS, HuntVis.Off)) {
                if (found.size >= MAX_TARGETS - 1) {
                    return found
                }
                if (npc !== primary && npc.isAttackable()) {
                    found += npc
                }
            }
            for (other in hunt.findPlayers(center, AREA_RADIUS, HuntVis.Off)) {
                if (found.size >= MAX_TARGETS - 1) {
                    return found
                }
                if (other !== primary && other !== player && canHitInWilderness(player, other)) {
                    found += other
                }
            }
            return found
        }
    }

    private class Delays(val server: Int, val client: Int)

    companion object {
        /** Burst and barrage spells hit at most this many targets (the primary included). */
        const val MAX_TARGETS: Int = 9

        /** Burst and barrage spells hit a 3x3 area: 1 tile around the primary target. */
        const val AREA_RADIUS: Int = 1

        /** Smoke spells poison on 1 in this many successful hits. */
        const val SMOKE_POISON_CHANCE: Int = 8

        /** Blood spells heal this percentage of the damage dealt. */
        const val BLOOD_HEAL_PERCENT: Int = 25

        private const val SOUND_RADIUS = 8
        private const val CLIENT_CYCLES_PER_TICK = 30

        /** Heals [player] by 25% of [damage] dealt, never above their base Hitpoints. */
        fun healFromBlood(player: Player, damage: Int) {
            val heal = damage * BLOOD_HEAL_PERCENT / 100
            if (heal <= 0 || player.hitpoints >= player.baseHitpointsLvl) {
                return
            }
            player.statHeal(stats.hitpoints, heal, 0)
        }

        private fun Npc.isAttackable(): Boolean =
            isValidTarget() && visType.op.any { it.equals("Attack", ignoreCase = true) }

        /**
         * Secondary player targets follow the normal Wilderness rule: both players are in the
         * Wilderness and their combat level difference is within the lower Wilderness level.
         */
        private fun canHitInWilderness(caster: Player, other: Player): Boolean {
            if (!WildernessArea.contains(caster.coords) || !WildernessArea.contains(other.coords)) {
                return false
            }
            val levels =
                min(WildernessArea.level(caster.coords), WildernessArea.level(other.coords))
            return abs(caster.combatLevel - other.combatLevel) <= levels
        }

        private fun PathingEntity.currentHitpoints(): Int =
            when (this) {
                is Npc -> hitpoints
                is Player -> hitpoints
            }
    }
}

enum class AncientElement {
    Smoke,
    Shadow,
    Blood,
    Ice,
}

enum class AncientTier(
    val multiTarget: Boolean,
    val smokePoisonSeverity: Int,
    val shadowDrainPercent: Int,
    val iceFreezeCycles: Int,
) {
    Rush(multiTarget = false, 10, 10, 8),
    Burst(multiTarget = true, 10, 10, 16),
    Blitz(multiTarget = false, 20, 15, 24),
    Barrage(multiTarget = true, 20, 15, 32),
}

class AncientSpell(
    val obj: ObjType,
    val element: AncientElement,
    val tier: AncientTier,
    val maxHit: Int,
) {
    internal fun fx(): AncientFx = AncientFx.of(element, tier)
}

/** The graphics and sounds of each ancient spell, by the cache's names. */
internal class AncientFx(
    val travel: SpotanimType?,
    val impact: SpotanimType,
    val castSound: SynthType?,
    val hitSound: SynthType?,
) {
    companion object {
        fun of(element: AncientElement, tier: AncientTier): AncientFx {
            val s = ancient_spots
            val f = ancient_synths
            return when (element) {
                AncientElement.Smoke ->
                    when (tier) {
                        AncientTier.Rush ->
                            AncientFx(
                                s.smoke_rush_travel,
                                s.smoke_rush_impact,
                                f.smoke_cast,
                                f.smoke_rush_impact,
                            )
                        AncientTier.Burst ->
                            AncientFx(
                                null,
                                s.smoke_burst_impact,
                                f.smoke_cast,
                                f.smoke_burst_impact,
                            )
                        AncientTier.Blitz ->
                            AncientFx(
                                s.smoke_blitz_travel,
                                s.smoke_blitz_impact,
                                f.smoke_cast,
                                f.smoke_blitz_impact,
                            )
                        AncientTier.Barrage ->
                            AncientFx(
                                s.smoke_barrage_travel,
                                s.smoke_barrage_impact,
                                f.smoke_cast,
                                f.smoke_barrage_impact,
                            )
                    }
                AncientElement.Shadow ->
                    when (tier) {
                        AncientTier.Rush ->
                            AncientFx(
                                s.shadow_rush_travel,
                                s.shadow_rush_impact,
                                f.shadow_cast,
                                f.shadow_rush_impact,
                            )
                        AncientTier.Burst ->
                            AncientFx(
                                null,
                                s.shadow_burst_impact,
                                f.shadow_cast,
                                f.shadow_burst_impact,
                            )
                        AncientTier.Blitz ->
                            AncientFx(
                                s.shadow_blitz_travel,
                                s.shadow_blitz_impact,
                                f.shadow_cast,
                                f.shadow_blitz_impact,
                            )
                        AncientTier.Barrage ->
                            AncientFx(
                                null,
                                s.shadow_barrage_impact,
                                f.shadow_cast,
                                f.shadow_barrage_impact,
                            )
                    }
                AncientElement.Blood ->
                    when (tier) {
                        AncientTier.Rush ->
                            AncientFx(
                                s.blood_rush_travel,
                                s.blood_rush_impact,
                                f.blood_cast,
                                f.blood_rush_impact,
                            )
                        AncientTier.Burst ->
                            AncientFx(
                                null,
                                s.blood_burst_impact,
                                f.blood_cast,
                                f.blood_burst_impact,
                            )
                        AncientTier.Blitz ->
                            AncientFx(
                                s.blood_blitz_travel,
                                s.blood_blitz_impact,
                                f.blood_cast,
                                f.blood_blitz_impact,
                            )
                        AncientTier.Barrage ->
                            AncientFx(
                                null,
                                s.blood_barrage_impact,
                                f.blood_cast,
                                f.blood_barrage_impact,
                            )
                    }
                AncientElement.Ice ->
                    when (tier) {
                        AncientTier.Rush ->
                            AncientFx(
                                s.ice_rush_travel,
                                s.ice_rush_impact,
                                f.ice_cast,
                                f.ice_rush_impact,
                            )
                        AncientTier.Burst ->
                            AncientFx(null, s.ice_burst_impact, f.ice_cast, f.ice_burst_impact)
                        AncientTier.Blitz ->
                            AncientFx(null, s.ice_blitz_impact, f.ice_cast, f.ice_blitz_impact)
                        AncientTier.Barrage ->
                            AncientFx(
                                s.ice_barrage_travel,
                                s.ice_barrage_impact,
                                f.ice_cast,
                                f.ice_barrage_impact,
                            )
                    }
            }
        }
    }
}

/** Every ancient combat spell with its wiki base max hit. */
val ANCIENT_COMBAT_SPELLS: List<AncientSpell> =
    listOf(
        AncientSpell(objs.spell_smoke_rush, AncientElement.Smoke, AncientTier.Rush, 13),
        AncientSpell(objs.spell_shadow_rush, AncientElement.Shadow, AncientTier.Rush, 14),
        AncientSpell(objs.spell_blood_rush, AncientElement.Blood, AncientTier.Rush, 15),
        AncientSpell(objs.spell_ice_rush, AncientElement.Ice, AncientTier.Rush, 16),
        AncientSpell(objs.spell_smoke_burst, AncientElement.Smoke, AncientTier.Burst, 17),
        AncientSpell(objs.spell_shadow_burst, AncientElement.Shadow, AncientTier.Burst, 18),
        AncientSpell(objs.spell_blood_burst, AncientElement.Blood, AncientTier.Burst, 21),
        AncientSpell(objs.spell_ice_burst, AncientElement.Ice, AncientTier.Burst, 22),
        AncientSpell(objs.spell_smoke_blitz, AncientElement.Smoke, AncientTier.Blitz, 23),
        AncientSpell(objs.spell_shadow_blitz, AncientElement.Shadow, AncientTier.Blitz, 24),
        AncientSpell(objs.spell_blood_blitz, AncientElement.Blood, AncientTier.Blitz, 25),
        AncientSpell(objs.spell_ice_blitz, AncientElement.Ice, AncientTier.Blitz, 26),
        AncientSpell(objs.spell_smoke_barrage, AncientElement.Smoke, AncientTier.Barrage, 27),
        AncientSpell(objs.spell_shadow_barrage, AncientElement.Shadow, AncientTier.Barrage, 28),
        AncientSpell(objs.spell_blood_barrage, AncientElement.Blood, AncientTier.Barrage, 29),
        AncientSpell(objs.spell_ice_barrage, AncientElement.Ice, AncientTier.Barrage, 30),
    )

internal typealias ancient_seqs = AncientSpellSeqs

internal typealias ancient_spots = AncientSpellSpots

internal typealias ancient_synths = AncientSpellSynths

object AncientSpellSeqs : SeqReferences() {
    /** Rush and blitz spells. */
    val cast: SeqType = find("zaros_casting")
    /** Burst and barrage spells. */
    val cast_area: SeqType = find("zaros_vertical_casting")
}

object AncientSpellSpots : SpotanimReferences() {
    val smoke_rush_travel = find("smoke_rush_travel")
    val smoke_rush_impact = find("smoke_rush_impact")
    val smoke_burst_impact = find("smoke_burst_impact")
    val smoke_blitz_travel = find("smoke_blitz_travel")
    val smoke_blitz_impact = find("smoke_blitz_impact")
    val smoke_barrage_travel = find("smoke_barrage_travel")
    val smoke_barrage_impact = find("smoke_barrage_impact")
    val shadow_rush_travel = find("shadow_rush_travel")
    val shadow_rush_impact = find("shadow_rush_impact")
    val shadow_burst_impact = find("shadow_burst_impact")
    val shadow_blitz_travel = find("shadow_blitz_travel")
    val shadow_blitz_impact = find("shadow_blitz_impact")
    val shadow_barrage_impact = find("shadow_barrage_impact")
    val blood_rush_travel = find("blood_rush_travel")
    val blood_rush_impact = find("blood_rush_impact")
    val blood_burst_impact = find("spell_blood_burst_impact")
    val blood_blitz_travel = find("blood_blitz_travel")
    val blood_blitz_impact = find("blood_blitz_impact")
    val blood_barrage_impact = find("spell_blood_barrage_impact")
    val ice_rush_travel = find("ice_rush_travel")
    val ice_rush_impact = find("ice_rush_impact")
    val ice_burst_impact = find("ice_burst_impact")
    val ice_blitz_impact = find("ice_blitz_impact")
    val ice_barrage_travel = find("ice_barrage_travel")
    val ice_barrage_impact = find("ice_barrage_impact")
}

object AncientSpellSynths : SynthReferences() {
    val smoke_cast = find("smoke_cast")
    val smoke_rush_impact = find("smoke_rush_impact")
    val smoke_burst_impact = find("smoke_burst_impact")
    val smoke_blitz_impact = find("smoke_blitz_impact")
    val smoke_barrage_impact = find("smoke_barrage_impact")
    val shadow_cast = find("shadow_cast")
    val shadow_rush_impact = find("shadow_rush_impact")
    val shadow_burst_impact = find("shadow_burst_impact")
    val shadow_blitz_impact = find("shadow_blitz_impact")
    val shadow_barrage_impact = find("shadow_barrage_impact")
    val blood_cast = find("blood_cast")
    val blood_rush_impact = find("blood_rush_impact")
    val blood_burst_impact = find("blood_burst_impact")
    val blood_blitz_impact = find("blood_blitz_impact")
    val blood_barrage_impact = find("blood_barrage_impact")
    val ice_cast = find("ice_cast")
    val ice_rush_impact = find("ice_rush_impact")
    val ice_burst_impact = find("ice_burst_impact")
    val ice_blitz_impact = find("ice_blitz_impact")
    val ice_barrage_impact = find("ice_barrage_impact")
}
