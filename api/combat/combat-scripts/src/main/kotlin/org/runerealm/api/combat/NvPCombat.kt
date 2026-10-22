package org.runerealm.api.combat

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.commons.npc.attackRate
import org.runerealm.api.combat.commons.player.combatPlayDefendAnim
import org.runerealm.api.combat.commons.player.queueCombatRetaliate
import org.runerealm.api.combat.formulas.AccuracyFormulae
import org.runerealm.api.combat.formulas.MaxHitFormulae
import org.runerealm.api.combat.npc.attackingPlayer
import org.runerealm.api.combat.npc.lastAttack
import org.runerealm.api.combat.player.aggressiveNpc
import org.runerealm.api.combat.player.lastCombat
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.projanims
import org.runerealm.api.config.refs.spotanims
import org.runerealm.api.npc.access.StandardNpcAccess
import org.runerealm.api.npc.isInCombat
import org.runerealm.api.player.hit.queueHit
import org.runerealm.api.player.isValidTarget
import org.runerealm.api.player.output.soundSynth
import org.runerealm.api.repo.world.WorldRepository
import org.runerealm.api.toxins.ToxinValues
import org.runerealm.api.toxins.poison
import org.runerealm.game.entity.Player
import org.runerealm.game.hit.HitType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.proj.ProjAnimType

/**
 * Npc attacks against players.
 *
 * Melee npcs attack from an adjacent tile. Ranged and magic npcs (`npc_attack_style` param) attack
 * from up to their type's `attackRange` away, firing the `proj_travel` spotanim as a projectile
 * (shaped by the `proj_type` projanim) and, on a hit, playing `proj_impact` on the target once the
 * projectile lands. The hit is queued for the projectile's flight time.
 *
 * Poisonous npcs (`npc_poison_damage` param, the wiki "Poisonous: Yes (N)" value) have a chance to
 * poison their target with every attack, even when the attack misses or is blocked by a protection
 * prayer ([Poison](https://oldschool.runescape.wiki/w/Poison)).
 */
internal class NvPCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val objTypes: ObjTypeList,
    private val worldRepo: WorldRepository,
) {
    fun attack(access: StandardNpcAccess, target: Player, attack: CombatAttack.NpcAttack) {
        when (attack) {
            is CombatAttack.NpcMelee -> access.attackMelee(target, attack)
            is CombatAttack.NpcRanged -> access.attackRanged(target)
            is CombatAttack.NpcMagic -> access.attackMagic(target, attack)
        }
    }

    private fun StandardNpcAccess.attackMelee(target: Player, attack: CombatAttack.NpcMelee) {
        if (!startAttack(target)) {
            return
        }
        playAttackFx(target)

        val successfulHit = accuracy.rollMeleeAccuracy(npc, target, attack.type, random)
        val damage =
            if (successfulHit) {
                val maxHit = maxHits.getMeleeMaxHit(npc, target, attack.type)
                random.of(0..maxHit)
            } else {
                0
            }

        setAttackVars(target)

        // Note: Retaliation must be queued _before_ the hit. If queued after, every hit would
        // trigger the "speed-up" death mechanic, since the hit queues would no longer be the
        // last entries in the queue list at the time of processing.
        target.queueCombatRetaliate(npc)

        target.queueHit(npc, 1, HitType.Melee, damage)
        target.combatPlayDefendAnim(objTypes)
        rollPoison(target)
    }

    private fun StandardNpcAccess.attackRanged(target: Player) {
        if (!startAttack(target)) {
            return
        }
        playAttackFx(target)
        val (serverDelay, clientDelay) = spawnProjectile(target, projanims.arrow)

        val successfulHit = accuracy.rollRangedAccuracy(npc, target, random)
        val damage =
            if (successfulHit) {
                random.of(0..maxHits.getRangedMaxHit(npc, target))
            } else {
                0
            }

        setAttackVars(target)
        target.queueCombatRetaliate(npc, delay = serverDelay)
        target.queueHit(npc, serverDelay, HitType.Ranged, damage)
        playImpact(target, clientDelay)
        rollPoison(target)
    }

    private fun StandardNpcAccess.attackMagic(target: Player, attack: CombatAttack.NpcMagic) {
        if (!startAttack(target)) {
            return
        }
        playAttackFx(target)
        val (serverDelay, clientDelay) = spawnProjectile(target, projanims.magic_spell)

        setAttackVars(target)
        target.queueCombatRetaliate(npc, delay = serverDelay)

        val successfulHit = accuracy.rollMagicAccuracy(npc, target, random)
        if (!successfulHit) {
            // A missed spell "splashes": no hitsplat, just the splash spotanim.
            target.spotanim(spotanims.failedspell_impact, delay = clientDelay, height = 124)
            rollPoison(target)
            return
        }
        val damage = random.of(0..attack.maxHit)
        target.queueHit(npc, serverDelay, HitType.Magic, damage)
        playImpact(target, clientDelay)
        rollPoison(target)
    }

    /** @return the npc's magic max hit against [target]: its fixed spell max hit, if set. */
    fun magicMaxHit(access: StandardNpcAccess, target: Player): Int {
        val fixed = access.npc.visType.paramOrNull(params.npc_magic_maxhit)
        return fixed ?: maxHits.getMagicMaxHit(access.npc, target)
    }

    /**
     * Checks that the attack can happen this cycle and, if so, sets the npc's next attack delay.
     *
     * @return `true` if the npc should attack now.
     */
    private fun StandardNpcAccess.startAttack(target: Player): Boolean {
        if (!canAttack(target)) {
            resetMode()
            return false
        }

        // Note: We do not need to explicitly call `opplayer2` because npcs will automatically
        // repeat their last interaction until it is canceled (e.g., by changing their `npcmode`).
        if (actionDelay > mapClock) {
            return false
        }

        if (!npc.isInCombat()) {
            resetMode()
            return false
        }

        actionDelay = mapClock + npc.attackRate()
        return true
    }

    private fun StandardNpcAccess.playAttackFx(target: Player) {
        val attackAnim = npc.visType.paramOrNull(params.attack_anim)
        val attackSound = npc.visType.paramOrNull(params.attack_sound)
        attackAnim?.let(::anim)
        attackSound?.let(target::soundSynth)
        val launch = npc.visType.paramOrNull(params.proj_launch)
        if (launch != null) {
            npc.spotanim(launch, height = 92)
        }
    }

    private fun StandardNpcAccess.spawnProjectile(
        target: Player,
        defaultProjanim: ProjAnimType,
    ): Pair<Int, Int> {
        val travel = npc.visType.paramOrNull(params.proj_travel) ?: return 1 to 0
        val projanim = npc.visType.paramOrNull(params.proj_type) ?: defaultProjanim
        val proj = worldRepo.projAnim(npc, target, travel, projanim)
        val (serverDelay, clientDelay) = proj.durations
        return serverDelay to clientDelay
    }

    private fun StandardNpcAccess.playImpact(target: Player, clientDelay: Int) {
        val impact = npc.visType.paramOrNull(params.proj_impact) ?: return
        target.spotanim(impact, delay = clientDelay, height = 124)
    }

    private fun StandardNpcAccess.rollPoison(target: Player) {
        val poisonDamage = npc.visType.param(params.npc_poison_damage)
        if (poisonDamage <= 0) {
            return
        }
        if (random.of(maxExclusive = NPC_POISON_CHANCE) != 0) {
            return
        }
        target.poison(ToxinValues.severityForDamage(poisonDamage))
    }

    private fun canAttack(target: Player): Boolean {
        return target.isValidTarget()
    }

    private fun StandardNpcAccess.setAttackVars(target: Player) {
        npc.lastAttack = mapClock
        npc.attackingPlayer = target.uid
        target.lastCombat = mapClock
        target.aggressiveNpc = npc.uid
    }

    private companion object {
        /**
         * The 1-in-N chance of a poisonous npc poisoning its target per attack. The wiki does not
         * list npc poison chances; this matches the 1/4 melee weapon poison chance.
         */
        const val NPC_POISON_CHANCE = 4
    }
}
