package org.runerealm.api.combat

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.runerealm.api.combat.scripts.NvPCombatScript
import org.runerealm.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.runerealm.api.combat.weapon.scripts.WeaponAttackTypesScript
import org.runerealm.api.combat.weapon.styles.AttackStyles
import org.runerealm.api.combat.weapon.types.AttackTypes
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.spotanims
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varns
import org.runerealm.api.hit.plugin.PlayerHitScript
import org.runerealm.api.npc.NpcCombatStyle
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.api.toxins.isPoisoned
import org.runerealm.game.entity.npc.NpcMode
import org.runerealm.game.type.npc.UnpackedNpcType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.game.type.util.ParamMap
import org.runerealm.map.CoordGrid

class NvPCombatTest {
    @Test
    fun GameTestState.`magic npc attacks from a distance without walking up`() = runNvPTest {
        val npc = spawnNpc(START, magicNpc)
        advance()
        npc.apPlayer2(player)
        advanceUntil({ npc.vars[varns.lastattack] > 0 }, timeoutTicks = 3)
        assertEquals(START, npc.coords)
        assertEquals(NpcMode.ApPlayer2, npc.mode)
    }

    @Test
    fun GameTestState.`ranged npc attacks from a distance without walking up`() = runNvPTest {
        val npc = spawnNpc(START, rangedNpc)
        advance()
        npc.apPlayer2(player)
        advanceUntil({ npc.vars[varns.lastattack] > 0 }, timeoutTicks = 3)
        assertEquals(START, npc.coords)
    }

    @Test
    fun GameTestState.`magic npc hits land after the projectile and deal damage`() = runNvPTest {
        val npc = spawnNpc(START, magicNpc)
        advance()
        npc.apPlayer2(player)
        // Over a number of casts at least one should land (max hit 5).
        advanceUntil({ player.stats[stats.hitpoints] < 99 }, timeoutTicks = 200)
        assertTrue(99 - player.stats[stats.hitpoints] <= 5 * 50)
    }

    @Test
    fun GameTestState.`melee npc in ap mode walks up to its target`() = runNvPTest {
        val npc = spawnNpc(START, meleeNpc)
        advance()
        npc.apPlayer2(player)
        // The melee npc walks up and, once in reach, switches to op mode and attacks.
        advanceUntil({ npc.vars[varns.lastattack] > 0 }, timeoutTicks = 15)
        assertNotEquals(START, npc.coords)
        assertEquals(NpcMode.OpPlayer2, npc.mode)
    }

    @Test
    fun GameTestState.`poisonous npc can poison its target`() = runNvPTest {
        val npc = spawnNpc(PLAYER_COORDS.translateX(1), poisonousNpc)
        advance()
        npc.opPlayer2(player)
        advanceUntil({ player.isPoisoned() }, timeoutTicks = 400)
        assertTrue(player.isPoisoned())
    }

    @Test
    fun GameTestState.`npc style param maps to combat style`() = runNvPTest {
        assertEquals(NpcCombatStyle.Magic, NpcCombatStyle[2])
        assertEquals(NpcCombatStyle.Ranged, NpcCombatStyle[1])
        assertEquals(NpcCombatStyle.Melee, NpcCombatStyle[0])
        assertEquals(NpcCombatStyle.Melee, NpcCombatStyle[99])
    }

    private fun GameTestState.runNvPTest(testBody: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            NvPTestDependencies::class,
            NvPTestModule,
            NvPCombatScript::class,
            PlayerHitScript::class,
            WeaponAttackStylesScript::class,
            WeaponAttackTypesScript::class,
        ) {
            allocZoneCollision(START)
            player.placeAt(PLAYER_COORDS)
            player.stats[stats.hitpoints] = 99
            // Combat checks treat a `lastcombat` clock of 0 as "in combat" for the first few
            // cycles of a fresh test world, so let those cycles pass first.
            advance(ticks = 10)
            testBody()
        }

    class NvPTestDependencies @Inject constructor(val attackStyles: AttackStyles)

    private object NvPTestModule : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private companion object {
        private val START = CoordGrid(0, 50, 50, 10, 10)
        private val PLAYER_COORDS = START.translateX(5)

        private fun npc(name: String, attackRange: Int, params: Map<Int, Any>): UnpackedNpcType =
            npcTypeFactory.create {
                this.name = name
                size = 1
                hitpoints = 50
                defaultMode = NpcMode.None
                op[1] = "Attack"
                this.attackRange = attackRange
                maxRange = 20
                val primitive = params.mapValues { (_, v) -> if (v is SpotanimType) v.id else v }
                paramMap = ParamMap(primitive, params)
            }

        private val magicNpc =
            npc(
                "Dark wizard",
                attackRange = 7,
                params =
                    mapOf(
                        params.npc_attack_style.id to NpcCombatStyle.Magic.id,
                        params.npc_magic_maxhit.id to 5,
                        params.proj_travel.id to spotanims.waterstrike_travel,
                        params.proj_impact.id to spotanims.waterstrike_impact,
                    ),
            )

        private val rangedNpc =
            npc(
                "Archer",
                attackRange = 7,
                params = mapOf(params.npc_attack_style.id to NpcCombatStyle.Ranged.id),
            )

        private val meleeNpc = npc("Brute", attackRange = 1, params = emptyMap())

        private val poisonousNpc =
            npc("Scorpion", attackRange = 1, params = mapOf(params.npc_poison_damage.id to 3))
    }
}
