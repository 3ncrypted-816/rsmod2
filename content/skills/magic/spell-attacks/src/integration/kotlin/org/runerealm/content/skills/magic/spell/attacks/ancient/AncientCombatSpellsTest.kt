package org.runerealm.content.skills.magic.spell.attacks.ancient

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.runerealm.annotations.InternalApi
import org.runerealm.api.area.checker.AreaChecker
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.commons.magic.Spellbook
import org.runerealm.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.runerealm.api.combat.weapon.styles.AttackStyles
import org.runerealm.api.config.refs.areas
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.freeze.isFrozen
import org.runerealm.api.freeze.remainingFreezeCycles
import org.runerealm.api.hunt.Hunt
import org.runerealm.api.player.righthand
import org.runerealm.api.player.stat.hitpoints
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.random.GameRandom
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.api.spells.attack.SpellAttackManager
import org.runerealm.api.spells.attack.SpellAttackRegistry
import org.runerealm.api.spells.attack.SpellAttackRepository
import org.runerealm.api.spells.attack.attack
import org.runerealm.api.spells.runes.MAGIC_RUNE_SCRIPTS
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.api.toxins.isPoisoned
import org.runerealm.api.toxins.nextToxinDamage
import org.runerealm.game.area.AreaIndex
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.npc.UnpackedNpcType
import org.runerealm.game.type.obj.ObjType
import org.runerealm.map.CoordGrid
import org.runerealm.map.square.MapSquareKey

class AncientCombatSpellsTest {
    /** Lumbridge: a single-combat area. */
    private val single = CoordGrid(3200, 3200, 0)

    /** Burthorpe: a multi-combat area. */
    private val multi = CoordGrid(2890, 3530, 0)

    @Test
    fun GameTestState.`ice rush freezes the target for 8 ticks`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 58)
            player.give(objs.water_rune to 2, objs.chaos_rune to 2, objs.death_rune to 2)
            random.next = 0 // Accuracy: hit.
            random.then = 0 // Damage.
            spells.cast(this, objs.spell_ice_rush, npc)
            assertTrue(npc.isFrozen())
            assertEquals(8, npc.remainingFreezeCycles())
            assertEquals(0, player.inv.count(objs.death_rune))
            advance(ticks = 8)
            assertFalse(npc.isFrozen())
        }

    @Test
    fun GameTestState.`ice spells need the ancient spellbook`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 58)
            VarPlayerIntMapSetter.set(player, varbits.spellbook, Spellbook.Standard.varValue)
            player.give(objs.water_rune to 2, objs.chaos_rune to 2, objs.death_rune to 2)
            spells.cast(this, objs.spell_ice_rush, npc)
            assertFalse(npc.isFrozen())
            assertEquals(2, player.inv.count(objs.death_rune))
        }

    @Test
    fun GameTestState.`ice barrage freezes every target in a 3x3 area in multi-combat`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            spells.markMultiway(multi)
            val npc = setup(multi, magic = 94)
            val east = spawnNpc(npc.coords.translateX(1), dummy)
            val north = spawnNpc(npc.coords.translateZ(1), dummy)
            val far = spawnNpc(npc.coords.translateX(3), dummy)
            player.give(objs.water_rune to 6, objs.blood_rune to 2, objs.death_rune to 4)
            repeat(20) { random.then = 0 } // Every accuracy roll hits, every damage roll is 0.
            spells.cast(this, objs.spell_ice_barrage, npc)
            assertTrue(npc.isFrozen())
            assertTrue(east.isFrozen())
            assertTrue(north.isFrozen())
            assertFalse(far.isFrozen())
            assertEquals(32, east.remainingFreezeCycles())
        }

    @Test
    fun GameTestState.`ice barrage only hits its target in single-combat`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 94)
            val east = spawnNpc(npc.coords.translateX(1), dummy)
            player.give(objs.water_rune to 6, objs.blood_rune to 2, objs.death_rune to 4)
            repeat(20) { random.then = 0 }
            spells.cast(this, objs.spell_ice_barrage, npc)
            assertTrue(npc.isFrozen())
            assertFalse(east.isFrozen())
        }

    @Test
    fun GameTestState.`burst and barrage hit at most 9 targets`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            spells.markMultiway(multi)
            val npc = setup(multi, magic = 70)
            val others = buildList {
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        if (dx != 0 || dz != 0) {
                            add(spawnNpc(npc.coords.translate(dx, dz, 0), dummy))
                        }
                    }
                }
                // A tenth target sharing the primary target's tile.
                add(spawnNpc(npc.coords, dummy))
            }
            player.give(objs.water_rune to 4, objs.chaos_rune to 4, objs.death_rune to 2)
            repeat(30) { random.then = 0 }
            spells.cast(this, objs.spell_ice_burst, npc)
            val frozen = (others + npc).count { it.isFrozen() }
            assertEquals(AncientCombatSpells.MAX_TARGETS, frozen)
        }

    @Test
    fun GameTestState.`blood rush heals a quarter of the damage dealt`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 56)
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 50)
            player.give(objs.blood_rune to 1, objs.chaos_rune to 2, objs.death_rune to 2)
            random.next = 0 // Accuracy: hit.
            random.then = 12 // Damage.
            spells.cast(this, objs.spell_blood_rush, npc)
            assertEquals(53, player.hitpoints)
        }

    @Test
    fun GameTestState.`blood healing never goes above base hitpoints`() = runGameTest {
        player.setBaseLevel(stats.hitpoints, 99)
        player.setCurrentLevel(stats.hitpoints, 98)
        AncientCombatSpells.healFromBlood(player, damage = 29)
        assertEquals(99, player.hitpoints)
    }

    @Test
    fun GameTestState.`smoke rush can poison the target`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 50)
            player.give(
                objs.air_rune to 1,
                objs.fire_rune to 1,
                objs.chaos_rune to 2,
                objs.death_rune to 2,
            )
            random.next = 0 // Accuracy: hit.
            random.then = 0 // Damage.
            random.then = 0 // Poison roll: 1/8 succeeds.
            spells.cast(this, objs.spell_smoke_rush, npc)
            assertTrue(npc.isPoisoned())
            assertEquals(2, npc.nextToxinDamage())
        }

    @Test
    fun GameTestState.`smoke rush does not always poison`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 50)
            player.give(
                objs.air_rune to 1,
                objs.fire_rune to 1,
                objs.chaos_rune to 2,
                objs.death_rune to 2,
            )
            random.next = 0
            random.then = 0
            random.then = 1 // Poison roll fails.
            spells.cast(this, objs.spell_smoke_rush, npc)
            assertFalse(npc.isPoisoned())
        }

    @Test
    fun GameTestState.`shadow rush lowers attack by 10 percent`() =
        runInjectedGameTest(Spells::class, StylesModule, *scripts()) { spells ->
            val npc = setup(single, magic = 52)
            player.give(
                objs.air_rune to 1,
                objs.soul_rune to 1,
                objs.chaos_rune to 2,
                objs.death_rune to 2,
            )
            random.next = 0
            random.then = 0
            spells.cast(this, objs.spell_shadow_rush, npc)
            assertEquals(90, npc.attackLvl)
        }

    class Spells
    @Inject
    constructor(
        private val registry: MagicSpellRegistry,
        manager: SpellAttackManager,
        random: GameRandom,
        hunt: Hunt,
        areaChecker: AreaChecker,
        private val areaIndex: AreaIndex,
    ) {
        private val attacks = SpellAttackRegistry()

        init {
            val repository = SpellAttackRepository(attacks)
            with(AncientCombatSpells(random, hunt, areaChecker)) { repository.register(manager) }
        }

        /** Test games do not decode the map, so the multi-combat area is registered by hand. */
        @OptIn(InternalApi::class)
        fun markMultiway(coords: CoordGrid) {
            val area = areas.multiway.id.toShort()
            areaIndex.registerAll(MapSquareKey.from(coords), listOf(area).iterator())
        }

        fun cast(scope: GameTestScope, spell: ObjType, target: Npc) {
            val attack = spellAttack(scope.player, spell)
            val handler = checkNotNull(attacks[spell])
            scope.run { player.withProtectedAccess { handler.attack(this, target, attack) } }
        }

        private fun spellAttack(player: Player, spell: ObjType): CombatAttack.Spell =
            CombatAttack.Spell(player.righthand, checkNotNull(registry.getObjSpell(spell)), false)
    }

    private fun GameTestScope.setup(start: CoordGrid, magic: Int): Npc {
        player.teleport(start)
        VarPlayerIntMapSetter.set(player, varbits.spellbook, Spellbook.Ancients.varValue)
        player.setBaseLevel(stats.magic, magic)
        player.setCurrentLevel(stats.magic, magic)
        return spawnNpc(start.translateX(3), dummy)
    }

    private fun Player.give(vararg objs: Pair<ObjType, Int>) {
        objs.forEachIndexed { slot, (obj, count) -> inv[slot] = InvObj(obj, count) }
    }

    private object StylesModule : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private companion object {
        val dummy: UnpackedNpcType =
            npcTypeFactory.create {
                name = "Dummy"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 1
                op[1] = "Attack"
            }

        fun scripts() = (MAGIC_RUNE_SCRIPTS + WeaponAttackStylesScript::class).toTypedArray()
    }
}
