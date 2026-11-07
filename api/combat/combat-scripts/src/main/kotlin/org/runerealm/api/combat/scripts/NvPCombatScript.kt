package org.runerealm.api.combat.scripts

import jakarta.inject.Inject
import org.runerealm.api.area.checker.AreaChecker
import org.runerealm.api.combat.NvPCombat
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.commons.types.MeleeAttackType
import org.runerealm.api.combat.commons.types.RangedAttackType
import org.runerealm.api.combat.player.aggressiveNpc
import org.runerealm.api.config.refs.categories
import org.runerealm.api.config.refs.params
import org.runerealm.api.npc.NpcCombatStyle
import org.runerealm.api.npc.access.StandardNpcAccess
import org.runerealm.api.npc.combatStyle
import org.runerealm.api.npc.interact.AiPlayerInteractions
import org.runerealm.api.npc.opPlayer2
import org.runerealm.api.npc.usesDistancedCombat
import org.runerealm.api.player.isInPvnCombat
import org.runerealm.api.player.isInPvpCombat
import org.runerealm.api.script.advanced.onDefaultAiApPlayer2
import org.runerealm.api.script.advanced.onDefaultAiOpPlayer2
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.type.category.isType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

public class NvPCombatScript
@Inject
internal constructor(
    private val combat: NvPCombat,
    private val areaChecker: AreaChecker,
    private val interactions: AiPlayerInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onDefaultAiOpPlayer2 { attemptCombatOp(it.target) }
        onDefaultAiApPlayer2 { attemptCombatAp(it.target) }
    }

    /**
     * Ranged and magic npcs attack once their target is within their `attackRange` (with line of
     * sight). Melee npcs in ap mode switch to op mode, so they walk up and attack from an adjacent
     * tile.
     */
    private fun StandardNpcAccess.attemptCombatAp(target: Player) {
        if (!npc.usesDistancedCombat()) {
            npc.opPlayer2(target, interactions)
            return
        }
        attemptCombatOp(target)
    }

    private fun StandardNpcAccess.attemptCombatOp(target: Player) {
        if (!canAttack(target)) {
            resetMode()
            return
        }
        val attack =
            when (npc.combatStyle) {
                NpcCombatStyle.Melee -> CombatAttack.NpcMelee(npc.resolveMeleeAttackType())
                NpcCombatStyle.Ranged -> CombatAttack.NpcRanged(RangedAttackType.Standard)
                NpcCombatStyle.Magic -> CombatAttack.NpcMagic(combat.magicMaxHit(this, target))
            }
        combat.attack(this, target, attack)
    }

    private fun StandardNpcAccess.canAttack(target: Player): Boolean {
        val singleCombat = !mapMultiway(areaChecker)
        if (singleCombat) {
            if (target.isInPvpCombat()) {
                return false
            }

            if (target.isInPvnCombat()) {
                if (target.aggressiveNpc != null && target.aggressiveNpc != npc.uid) {
                    return false
                }
            }
        }
        return true
    }

    private fun Npc.resolveMeleeAttackType(): MeleeAttackType {
        val category = visType.paramOrNull(params.npc_attack_type)
        return when {
            category.isType(categories.attacktype_stab) -> MeleeAttackType.Stab
            category.isType(categories.attacktype_slash) -> MeleeAttackType.Slash
            else -> MeleeAttackType.Crush
        }
    }
}
