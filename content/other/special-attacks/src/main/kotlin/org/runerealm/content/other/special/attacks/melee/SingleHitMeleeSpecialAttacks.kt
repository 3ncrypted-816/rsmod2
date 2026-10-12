package org.runerealm.content.other.special.attacks.melee

import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.config.constants
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.specials.SpecialAttackManager
import org.runerealm.api.specials.SpecialAttackMap
import org.runerealm.api.specials.SpecialAttackRepository
import org.runerealm.api.specials.combat.MeleeSpecialAttack
import org.runerealm.content.other.special.attacks.configs.special_objs
import org.runerealm.content.other.special.attacks.configs.special_seqs
import org.runerealm.content.other.special.attacks.configs.special_spots
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.PathingEntity
import org.runerealm.game.entity.Player
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType

/**
 * Melee special attacks that consist of a single hit with modified accuracy and/or max hit, and no
 * secondary effect.
 * - Dragon scimitar "Sever": 25% increased accuracy. (The PvP effect that disables the target's
 *   protection prayers is not implemented; there is no shared mechanism to block prayers yet.)
 * - Dragon mace "Shatter": 25% increased accuracy and 50% increased max hit.
 */
class SingleHitMeleeSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sever =
            SingleHit(
                manager = manager,
                seq = special_seqs.dragon_scimitar,
                spot = special_spots.dragon_scimitar,
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.0,
            )
        registerMelee(special_objs.dragon_scimitar, sever)
        registerMelee(special_objs.dragon_scimitar_or, sever)

        val shatter =
            SingleHit(
                manager = manager,
                seq = special_seqs.dragon_mace,
                spot = special_spots.dragon_mace,
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 1.5,
            )
        registerMelee(special_objs.dragon_mace, shatter)
    }

    private class SingleHit(
        private val manager: SpecialAttackManager,
        private val seq: SeqType,
        private val spot: SpotanimType,
        private val accuracyMultiplier: Double,
        private val maxHitMultiplier: Double,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            hit(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            hit(target, attack)
            return true
        }

        private fun ProtectedAccess.hit(target: PathingEntity, attack: CombatAttack.Melee) {
            anim(seq)
            spotanim(spot = spot, slot = constants.spotanim_slot_combat, height = 96)

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = accuracyMultiplier,
                    maxHitMultiplier = maxHitMultiplier,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}
