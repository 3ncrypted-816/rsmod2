package org.runerealm.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.stats
import org.runerealm.api.freeze.isFrozen
import org.runerealm.api.player.back
import org.runerealm.api.player.righthand
import org.runerealm.api.player.stat.stat
import org.runerealm.api.random.GameRandom
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.api.spells.attack.SpellAttackManager
import org.runerealm.api.spells.attack.SpellAttackRegistry
import org.runerealm.api.spells.attack.SpellAttackRepository
import org.runerealm.api.spells.attack.attack
import org.runerealm.api.spells.charge.activateCharge
import org.runerealm.api.spells.runes.MAGIC_RUNE_SCRIPTS
import org.runerealm.api.teleblock.isTeleblocked
import org.runerealm.api.teleblock.remainingTeleblockCycles
import org.runerealm.api.testing.GameTestState
import org.runerealm.api.testing.factory.npcTypeFactory
import org.runerealm.api.testing.scope.GameTestScope
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.inv.InvObj
import org.runerealm.game.type.npc.UnpackedNpcType
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.util.ParamMapBuilder
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript

class StandardCombatSpellsTest {
    private val start = CoordGrid(3200, 3200, 0)

    @Test
    fun GameTestState.`bind freezes the target for 8 ticks`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 20, target = dummy)
            player.give(objs.nature_rune to 2, objs.water_rune to 3, objs.earth_rune to 3)
            random.next = 0 // Accuracy roll: hit.
            spells.cast(this, objs.spell_bind, npc)
            advance(ticks = 1)
            assertTrue(npc.isFrozen())
            assertEquals(0, player.inv.count(objs.nature_rune))
            advance(ticks = 8)
            assertFalse(npc.isFrozen())
        }

    @Test
    fun GameTestState.`bind cannot be cast on a frozen target`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 79, target = dummy)
            player.give(objs.nature_rune to 6, objs.water_rune to 8, objs.earth_rune to 8)
            random.next = 0
            spells.cast(this, objs.spell_bind, npc)
            advance(ticks = 1)
            spells.cast(this, objs.spell_entangle, npc)
            advance(ticks = 1)
            assertEquals(4, player.inv.count(objs.nature_rune))
        }

    @Test
    fun GameTestState.`confuse lowers attack by 5 percent once`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 3, target = dummy)
            player.give(objs.body_rune to 2, objs.water_rune to 6, objs.earth_rune to 4)
            random.next = 0
            spells.cast(this, objs.spell_confuse, npc)
            advance(ticks = 1)
            assertTrue(npc.attackLvl < 100)

            spells.cast(this, objs.spell_confuse, npc)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(objs.body_rune))
        }

    @Test
    fun GameTestState.`stun lowers a player's attack by 10 percent`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            setup(magic = 80, target = null)
            val target = registerPlayer(start.translateX(3))
            allocZoneCollision(target.coords)
            target.setBaseLevel(stats.attack, 70)
            target.setCurrentLevel(stats.attack, 70)
            player.give(objs.soul_rune to 1, objs.water_rune to 12, objs.earth_rune to 12)
            random.next = 0
            spells.cast(this, objs.spell_stun, target)
            advance(ticks = 1)
            assertEquals(63, target.stat(stats.attack))
        }

    @Test
    fun GameTestState.`crumble undead only affects undead`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 39, target = dummy)
            player.give(objs.chaos_rune to 2, objs.earth_rune to 4, objs.air_rune to 4)
            spells.cast(this, objs.spell_crumble_undead, npc)
            advance(ticks = 1)
            assertEquals(2, player.inv.count(objs.chaos_rune))

            val zombie = spawnNpc(start.translateX(-3), undead)
            random.next = 0
            spells.cast(this, objs.spell_crumble_undead, zombie)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(objs.chaos_rune))
        }

    @Test
    fun GameTestState.`iban blast needs iban's staff`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 50, target = dummy)
            player.give(objs.death_rune to 1, objs.fire_rune to 5)
            spells.cast(this, objs.spell_iban_blast, npc)
            advance(ticks = 1)
            assertEquals(1, player.inv.count(objs.death_rune))
        }

    @Test
    fun GameTestState.`magic dart and god spell max hits`() = runGameTest {
        assertEquals(19, StandardCombatSpells.magicDartMaxHit(99))
        assertEquals(15, StandardCombatSpells.magicDartMaxHit(50))
        player.setBaseLevel(stats.magic, 80)
        player.setCurrentLevel(stats.magic, 80)
        val effect = StandardCombatSpells.GodSpellEffect.Saradomin
        assertEquals(20, StandardCombatSpells.godSpellMaxHit(player, effect))
        player.activateCharge()
        assertEquals(20, StandardCombatSpells.godSpellMaxHit(player, effect))
        player.back = InvObj(std_objs.saradomin_cape, 1)
        assertEquals(30, StandardCombatSpells.godSpellMaxHit(player, effect))
    }

    @Test
    fun GameTestState.`tele block blocks the target and cannot be cast on npcs`() =
        runInjectedGameTest(Spells::class, null, *spellTestScripts()) { spells ->
            val npc = setup(magic = 85, target = dummy)
            player.give(objs.law_rune to 2, objs.death_rune to 2, objs.chaos_rune to 2)
            spells.cast(this, objs.spell_tele_block, npc)
            advance(ticks = 1)

            val target = registerPlayer(start.translateX(3))
            allocZoneCollision(target.coords)
            random.next = 0
            spells.cast(this, objs.spell_tele_block, target)
            advance(ticks = 1)
            assertTrue(target.isTeleblocked())
            assertTrue(target.remainingTeleblockCycles() > 400)
            assertEquals(1, player.inv.count(objs.law_rune))
        }

    class Spells
    @Inject
    constructor(
        private val registry: MagicSpellRegistry,
        manager: SpellAttackManager,
        objTypes: ObjTypeList,
        random: GameRandom,
    ) {
        private val attacks = SpellAttackRegistry()

        init {
            val repository = SpellAttackRepository(attacks)
            with(StandardCombatSpells(objTypes, random)) { repository.register(manager) }
        }

        fun cast(scope: GameTestScope, spell: ObjType, target: Npc) {
            val attack = spellAttack(scope.player, spell)
            val handler = checkNotNull(attacks[spell])
            scope.run { player.withProtectedAccess { handler.attack(this, target, attack) } }
        }

        fun cast(scope: GameTestScope, spell: ObjType, target: Player) {
            val attack = spellAttack(scope.player, spell)
            val handler = checkNotNull(attacks[spell])
            scope.run { player.withProtectedAccess { handler.attack(this, target, attack) } }
        }

        private fun spellAttack(player: Player, spell: ObjType): CombatAttack.Spell =
            CombatAttack.Spell(player.righthand, checkNotNull(registry.getObjSpell(spell)), false)
    }

    private fun GameTestScope.setup(magic: Int, target: UnpackedNpcType?): Npc {
        player.teleport(start)
        player.setBaseLevel(stats.magic, magic)
        player.setCurrentLevel(stats.magic, magic)
        return spawnNpc(start.translateX(3), target ?: dummy)
    }

    private fun Player.give(vararg objs: Pair<ObjType, Int>) {
        objs.forEachIndexed { slot, (obj, count) -> inv[slot] = InvObj(obj, count) }
    }

    private companion object {
        val dummy: UnpackedNpcType =
            npcTypeFactory.create {
                name = "Dummy"
                hitpoints = 100
                attack = 100
                strength = 100
                defence = 1
            }

        val undead: UnpackedNpcType =
            npcTypeFactory.create {
                name = "Zombie"
                hitpoints = 100
                paramMap = ParamMapBuilder().apply { this[params.undead] = 1 }.toParamMap()
            }
    }
}

private fun spellTestScripts(
    vararg scripts: KClass<out PluginScript>
): Array<KClass<out PluginScript>> =
    (MAGIC_RUNE_SCRIPTS + WeaponAttackStylesScript::class + scripts).toTypedArray()
