package org.runerealm.content.other.special.attacks

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.commons.CombatStance
import org.runerealm.api.combat.commons.styles.MeleeAttackStyle
import org.runerealm.api.combat.commons.types.MeleeAttackType
import org.runerealm.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.runerealm.api.combat.weapon.styles.AttackStyles
import org.runerealm.api.combat.weapon.types.AttackTypes
import org.runerealm.api.config.refs.stats
import org.runerealm.api.hit.plugin.NpcHitScript
import org.runerealm.api.specials.SpecialAttack
import org.runerealm.api.specials.SpecialAttackModule as ApiSpecialAttackModule
import org.runerealm.api.specials.SpecialAttackRegistry
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.content.other.special.attacks.configs.special_objs
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.npc.NpcMode
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.obj.ObjType
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class CombatSpecialAttacksTest {
    @Test
    fun GameTestState.`spec weapons are registered with their energy requirements`() =
        runSpecialsTest { deps ->
            fun assertEnergy(obj: ObjType, expected: Int) {
                val special = deps.registry[InvObj(obj)]
                assertNotNull(special)
                val energy =
                    when (special) {
                        is SpecialAttack.Melee -> special.energyInHundreds
                        is SpecialAttack.Ranged -> special.energyInHundreds
                        else -> error("Unexpected special type for $obj: $special")
                    }
                assertEquals(expected, energy)
            }

            assertEnergy(special_objs.dragon_dagger, 250)
            assertEnergy(special_objs.dragon_dagger_p, 250)
            assertEnergy(special_objs.dragon_dagger_p_plus, 250)
            assertEnergy(special_objs.dragon_dagger_p_plus_plus, 250)
            assertEnergy(special_objs.abyssal_whip, 500)
            assertEnergy(special_objs.volcanic_abyssal_whip, 500)
            assertEnergy(special_objs.frozen_abyssal_whip, 500)
            assertEnergy(special_objs.dragon_scimitar, 550)
            assertEnergy(special_objs.dragon_scimitar_or, 550)
            assertEnergy(special_objs.dragon_mace, 250)
            assertEnergy(special_objs.armadyl_godsword, 500)
            assertEnergy(special_objs.armadyl_godsword_or, 500)
            assertEnergy(special_objs.bandos_godsword, 500)
            assertEnergy(special_objs.bandos_godsword_or, 500)
            assertEnergy(special_objs.saradomin_godsword, 500)
            assertEnergy(special_objs.saradomin_godsword_or, 500)
            assertEnergy(special_objs.zamorak_godsword, 500)
            assertEnergy(special_objs.zamorak_godsword_or, 500)
            assertEnergy(special_objs.dragon_warhammer, 500)
            assertEnergy(special_objs.dragon_warhammer_cr, 500)
            assertEnergy(special_objs.dragon_claws, 500)
            assertEnergy(special_objs.dragon_claws_cr, 500)
            assertEnergy(special_objs.magic_shortbow, 550)
            assertEnergy(special_objs.magic_shortbow_i, 500)
        }

    @Test
    fun GameTestState.`dragon dagger hits twice`() = runSpecialsTest { deps ->
        val npc = spawnDummy()
        random.next = HIT_ROLL
        random.then = 10
        random.then = HIT_ROLL
        random.then = 7
        useMeleeSpecial(deps, special_objs.dragon_dagger, npc, MeleeAttackType.Stab)
        advance(ticks = 1)
        assertEquals(DUMMY_HITPOINTS - 17, npc.hitpoints)
    }

    @Test
    fun GameTestState.`dragon warhammer drains defence by 30 percent on hit`() =
        runSpecialsTest { deps ->
            val npc = spawnDummy()
            random.next = HIT_ROLL
            random.then = 5
            useMeleeSpecial(deps, special_objs.dragon_warhammer, npc, MeleeAttackType.Crush)
            assertEquals(DUMMY_DEFENCE - (DUMMY_DEFENCE * 30) / 100, npc.defenceLvl)
        }

    @Test
    fun GameTestState.`dragon warhammer does not drain defence on miss`() =
        runSpecialsTest { deps ->
            val npc = spawnDummy()
            random.next = MISS_ROLL
            useMeleeSpecial(deps, special_objs.dragon_warhammer, npc, MeleeAttackType.Crush)
            assertEquals(DUMMY_DEFENCE, npc.defenceLvl)
        }

    @Test
    fun GameTestState.`bandos godsword drains defence then strength by damage dealt`() =
        runSpecialsTest { deps ->
            val npc = spawnDummy()
            random.next = HIT_ROLL
            random.then = DUMMY_DEFENCE + 7
            useMeleeSpecial(deps, special_objs.bandos_godsword, npc, MeleeAttackType.Slash)
            assertEquals(0, npc.defenceLvl)
            assertEquals(DUMMY_STRENGTH - 7, npc.strengthLvl)
        }

    @Test
    fun GameTestState.`dragon claws cascade from the second hit`() = runSpecialsTest { deps ->
        val npc = spawnDummy()
        random.next = MISS_ROLL
        random.then = HIT_ROLL
        random.then = 20
        useMeleeSpecial(deps, special_objs.dragon_claws, npc, MeleeAttackType.Slash)
        advance(ticks = 1)
        assertEquals(DUMMY_HITPOINTS - 20, npc.hitpoints)
        advance(ticks = 1)
        // 0, 20, 10, 11
        assertEquals(DUMMY_HITPOINTS - 41, npc.hitpoints)
    }

    private fun GameTestScope.spawnDummy(): Npc {
        val npc = spawnNpc(CoordGrid(0, 50, 50, 0, 0), dummy)
        player.placeAt(npc.coords.translateZ(1))
        player.stats[stats.attack] = 99
        player.stats[stats.strength] = 99
        return npc
    }

    private fun GameTestScope.useMeleeSpecial(
        deps: SpecialsTestDependencies,
        weapon: ObjType,
        target: Npc,
        type: MeleeAttackType,
    ) {
        val special = deps.registry[InvObj(weapon)]
        check(special is SpecialAttack.Melee) { "Expected melee special for $weapon: $special" }
        val attack =
            CombatAttack.Melee(
                InvObj(weapon),
                type,
                MeleeAttackStyle.Accurate,
                CombatStance.Stance1,
            )
        player.withProtectedAccess { special.attack(this, target, attack) }
    }

    private fun GameTestState.runSpecialsTest(
        testBody: GameTestScope.(SpecialsTestDependencies) -> Unit
    ) =
        runInjectedGameTest(
            SpecialsTestDependencies::class,
            SpecialsTestModule,
            specialAttackScript,
            NpcHitScript::class,
            WeaponAttackStylesScript::class,
            testBody = testBody,
        )

    class SpecialsTestDependencies @Inject constructor(val registry: SpecialAttackRegistry)

    private object SpecialsTestModule : AbstractModule() {
        override fun configure() {
            install(ApiSpecialAttackModule())
            install(SpecialAttackModule())
            bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private companion object {
        /** A roll of `0` always passes the accuracy check (`hitChance > roll`). */
        private const val HIT_ROLL = 0

        /** The highest possible roll, which fails any accuracy check below 100%. */
        private const val MISS_ROLL = 9_999

        private const val DUMMY_HITPOINTS = 200
        private const val DUMMY_DEFENCE = 50
        private const val DUMMY_STRENGTH = 60

        /** `SpecialAttackScript` is internal to `api:specials`; it registers every special map. */
        @Suppress("UNCHECKED_CAST")
        private val specialAttackScript: KClass<out PluginScript> =
            Class.forName("org.runerealm.api.specials.scripts.SpecialAttackScript").kotlin
                as KClass<out PluginScript>

        private val dummy =
            npcTypeFactory.create {
                name = "Dummy"
                size = 1
                hitpoints = DUMMY_HITPOINTS
                defence = DUMMY_DEFENCE
                strength = DUMMY_STRENGTH
                defaultMode = NpcMode.None
            }
    }
}
