package org.runerealm.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import kotlin.math.max
import org.runerealm.api.combat.commons.CombatAttack
import org.runerealm.api.combat.manager.MagicRuneManager
import org.runerealm.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.runerealm.api.config.refs.categories
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.projanims
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.freeze.freeze
import org.runerealm.api.freeze.isFreezeImmune
import org.runerealm.api.freeze.isFrozen
import org.runerealm.api.player.back
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.magicLvl
import org.runerealm.api.player.stat.stat
import org.runerealm.api.player.stat.statBase
import org.runerealm.api.player.stat.statSub
import org.runerealm.api.random.GameRandom
import org.runerealm.api.spells.attack.SpellAttack
import org.runerealm.api.spells.attack.SpellAttackManager
import org.runerealm.api.spells.attack.SpellAttackMap
import org.runerealm.api.spells.attack.SpellAttackRepository
import org.runerealm.api.spells.charge.isChargeActive
import org.runerealm.api.teleblock.TELEBLOCK_CYCLES
import org.runerealm.api.teleblock.TELEBLOCK_PROTECTED_CYCLES
import org.runerealm.api.teleblock.formatTeleblockTime
import org.runerealm.api.teleblock.isTeleblocked
import org.runerealm.api.teleblock.teleblock
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.api.type.refs.synth.SynthReferences
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.PathingEntity
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.seq.SeqType
import org.runerealm.game.type.spot.SpotanimType
import org.runerealm.game.type.stat.StatType
import org.runerealm.game.type.synth.SynthType

/**
 * The standard spellbook's non-elemental combat spells, following the OSRS Wiki:
 * - [Bind](https://oldschool.runescape.wiki/w/Bind), Snare and Entangle hold the target in place
 *   for 8, 16 and 24 ticks (Snare and Entangle can also hit up to 3 and 5).
 * - [Confuse](https://oldschool.runescape.wiki/w/Confuse), Weaken, Curse, Vulnerability, Enfeeble
 *   and Stun lower a stat by 5% or 10%. They cannot be cast on a target whose stat is already
 *   lowered.
 * - [Crumble Undead](https://oldschool.runescape.wiki/w/Crumble_Undead) only affects undead.
 * - [Magic Dart](https://oldschool.runescape.wiki/w/Magic_Dart) hits up to 10 + Magic level / 10.
 * - The god spells hit up to 20, or 30 with an active Charge and the matching god cape.
 * - [Iban Blast](https://oldschool.runescape.wiki/w/Iban_Blast) hits up to 25.
 * - [Tele Block](https://oldschool.runescape.wiki/w/Tele_Block) can only be cast on players.
 *
 * Staff and weapon requirements (Iban's staff, slayer's staff, god staves) are part of each spell's
 * cache requirements and are checked by [MagicRuneManager].
 */
class StandardCombatSpells
@Inject
constructor(private val objTypes: ObjTypeList, private val random: GameRandom) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        registerBinds(manager)
        registerCurses(manager)
        registerDamageSpells(manager)
        register(objs.spell_tele_block, TeleBlockAttack(manager, objTypes))
    }

    private fun SpellAttackRepository.registerBinds(manager: SpellAttackManager) {
        register(
            objs.spell_bind,
            BindAttack(manager, objTypes, random, cycles = 8, maxHit = 0, std_spots.bind_impact),
        )
        register(
            objs.spell_snare,
            BindAttack(manager, objTypes, random, cycles = 16, maxHit = 3, std_spots.snare_impact),
        )
        register(
            objs.spell_entangle,
            BindAttack(
                manager,
                objTypes,
                random,
                cycles = 24,
                maxHit = 5,
                std_spots.entangle_impact,
            ),
        )
    }

    private fun SpellAttackRepository.registerCurses(manager: SpellAttackManager) {
        val s = std_seqs
        val g = std_spots
        val f = std_synths
        register(
            objs.spell_confuse,
            CurseAttack(
                manager,
                objTypes,
                stats.attack,
                5,
                s.confuse_staff,
                s.confuse,
                Fx(
                    g.confuse_casting,
                    g.confuse_travel,
                    g.confuse_impact,
                    f.confuse_cast,
                    f.confuse_hit,
                ),
            ),
        )
        register(
            objs.spell_weaken,
            CurseAttack(
                manager,
                objTypes,
                stats.strength,
                5,
                s.weaken_staff,
                s.weaken,
                Fx(g.weaken_casting, g.weaken_travel, g.weaken_impact, f.weaken, f.weaken),
            ),
        )
        register(
            objs.spell_curse,
            CurseAttack(
                manager,
                objTypes,
                stats.defence,
                5,
                s.curse_staff,
                s.curse,
                Fx(g.curse_casting, g.curse_travel, g.curse_impact, f.curse_cast, f.curse_hit),
            ),
        )
        register(
            objs.spell_vulnerability,
            CurseAttack(
                manager,
                objTypes,
                stats.defence,
                10,
                s.curse_staff,
                s.curse,
                Fx(
                    g.vulnerability_casting,
                    g.vulnerability_travel,
                    g.vulnerability_impact,
                    f.vulnerability,
                    f.vulnerability,
                ),
            ),
        )
        register(
            objs.spell_enfeeble,
            CurseAttack(
                manager,
                objTypes,
                stats.strength,
                10,
                s.enfeeble_staff,
                s.enfeeble,
                Fx(
                    g.enfeeble_casting,
                    g.enfeeble_travel,
                    g.enfeeble_impact,
                    f.enfeeble_cast,
                    f.enfeeble_hit,
                ),
            ),
        )
        register(
            objs.spell_stun,
            CurseAttack(
                manager,
                objTypes,
                stats.attack,
                10,
                s.stun_staff,
                s.stun,
                Fx(g.stun_casting, g.stun_travel, g.stun_impact, f.stun, f.stun),
            ),
        )
    }

    private fun SpellAttackRepository.registerDamageSpells(manager: SpellAttackManager) {
        val s = std_seqs
        val g = std_spots
        val f = std_synths
        register(
            objs.spell_crumble_undead,
            ProjectileAttack(
                manager,
                objTypes,
                s.crumble_staff,
                s.crumble,
                Fx(
                    g.crumble_casting,
                    g.crumble_travel,
                    g.crumble_impact,
                    f.crumble_cast,
                    f.crumble_hit,
                ),
                canTarget = { target -> target is Npc && target.visType.param(params.undead) != 0 },
                targetMessage = CRUMBLE_UNDEAD_MESSAGE,
                getMaxHit = { CRUMBLE_UNDEAD_MAX_HIT },
            ),
        )
        register(
            objs.spell_magic_dart,
            ProjectileAttack(
                manager,
                objTypes,
                s.magic_dart,
                s.magic_dart,
                Fx(null, g.magic_dart_travel, g.magic_dart_impact, null, f.magic_dart_hit),
                getMaxHit = { player -> magicDartMaxHit(player.magicLvl) },
            ),
        )
        register(
            objs.spell_iban_blast,
            ProjectileAttack(
                manager,
                objTypes,
                s.iban_blast,
                s.iban_blast,
                Fx(g.iban_casting, g.iban_travel, g.iban_impact, null, null),
                getMaxHit = { IBAN_BLAST_MAX_HIT },
            ),
        )
        register(
            objs.spell_saradomin_strike,
            GodSpellAttack(manager, objTypes, g.saradomin_strike, GodSpellEffect.Saradomin),
        )
        register(
            objs.spell_claws_of_guthix,
            GodSpellAttack(manager, objTypes, g.claws_of_guthix, GodSpellEffect.Guthix),
        )
        register(
            objs.spell_flames_of_zamorak,
            GodSpellAttack(manager, objTypes, g.flames_of_zamorak, GodSpellEffect.Zamorak),
        )
    }

    /** Casting, travel and impact graphics plus sounds of a spell. */
    private class Fx(
        val casting: SpotanimType?,
        val travel: SpotanimType,
        val impact: SpotanimType,
        val castSound: SynthType?,
        val hitSound: SynthType?,
    )

    private abstract class StandardSpellAttack(
        protected val manager: SpellAttackManager,
        private val objTypes: ObjTypeList,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        abstract fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell)

        protected fun ProtectedAccess.castAnim(
            attack: CombatAttack.Spell,
            staff: SeqType,
            unarmed: SeqType,
        ) {
            val weapon = objTypes.getOrNull(attack.weapon)
            val seq =
                if (weapon != null && weapon.isCategoryType(categories.staff)) staff else unarmed
            anim(seq)
        }

        /** Stops the cast with [message], without using any runes. */
        protected fun ProtectedAccess.reject(message: String) {
            mes(message)
            manager.stopCombat(this)
        }
    }

    /** Bind, Snare and Entangle, using the shared freeze mechanic. */
    private class BindAttack(
        manager: SpellAttackManager,
        objTypes: ObjTypeList,
        private val random: GameRandom,
        private val cycles: Int,
        private val maxHit: Int,
        private val impact: SpotanimType,
    ) : StandardSpellAttack(manager, objTypes) {
        override fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            if (target.isFrozen()) {
                reject(ALREADY_HELD_MESSAGE)
                return
            }
            if (target.isFreezeImmune()) {
                reject(IMMUNE_MESSAGE)
                return
            }
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            castAnim(attack, std_seqs.entangle_staff, std_seqs.entangle)
            spotanim(std_spots.entangle_casting, height = 92)
            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    std_spots.entangle_travel,
                    projanims.magic_spell,
                )
            val (serverDelay, clientDelay) = proj.durations
            val spell = attack.spell.obj

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, std_synths.bind_cast, 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val source = player.takeIf { target is Player }
            target.freeze(cycles, random, source)
            val damage =
                if (maxHit > 0) manager.rollMaxHit(this, target, attack, castResult, maxHit) else 0
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = std_synths.bind_cast,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 0,
                hitSound = std_synths.bind_impact,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    /** Confuse, Weaken, Curse, Vulnerability, Enfeeble and Stun. */
    private class CurseAttack(
        manager: SpellAttackManager,
        objTypes: ObjTypeList,
        private val stat: StatType,
        private val percent: Int,
        private val staffAnim: SeqType,
        private val unarmedAnim: SeqType,
        private val fx: Fx,
    ) : StandardSpellAttack(manager, objTypes) {
        override fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            if (StatDrains.isLowered(target, stat)) {
                reject("Your foe's ${statName(stat)} has already been weakened.")
                return
            }
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            castAnim(attack, staffAnim, unarmedAnim)
            fx.casting?.let { spotanim(it, height = 92) }
            val proj = manager.spawnProjectile(this, target, fx.travel, projanims.magic_spell)
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, fx.castSound, 8)
                manager.queueSplashHit(this, target, attack.spell.obj, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            StatDrains.drainPercent(target, stat, percent)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = fx.castSound,
                soundRadius = 8,
                hitSpot = fx.impact,
                hitSpotHeight = 124,
                hitSound = fx.hitSound,
            )
            manager.continueCombatIfAutocast(this, target)
        }
    }

    /** Projectile spells that deal damage: Crumble Undead, Magic Dart and Iban Blast. */
    private class ProjectileAttack(
        manager: SpellAttackManager,
        objTypes: ObjTypeList,
        private val staffAnim: SeqType,
        private val unarmedAnim: SeqType,
        private val fx: Fx,
        private val canTarget: (PathingEntity) -> Boolean = { true },
        private val targetMessage: String? = null,
        private val getMaxHit: (Player) -> Int,
    ) : StandardSpellAttack(manager, objTypes) {
        override fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            if (!canTarget(target)) {
                reject(checkNotNull(targetMessage))
                return
            }
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            castAnim(attack, staffAnim, unarmedAnim)
            fx.casting?.let { spotanim(it, height = 92) }
            val proj = manager.spawnProjectile(this, target, fx.travel, projanims.magic_spell)
            val (serverDelay, clientDelay) = proj.durations
            val spell = attack.spell.obj

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, fx.castSound, 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val baseMaxHit = getMaxHit(player)
            val damage = manager.rollMaxHit(this, target, attack, castResult, baseMaxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = fx.castSound,
                soundRadius = 8,
                hitSpot = fx.impact,
                hitSpotHeight = 124,
                hitSound = fx.hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    /**
     * Saradomin Strike, Claws of Guthix and Flames of Zamorak: no projectile, a strike on impact.
     */
    private class GodSpellAttack(
        manager: SpellAttackManager,
        objTypes: ObjTypeList,
        private val impact: SpotanimType,
        private val effect: GodSpellEffect,
    ) : StandardSpellAttack(manager, objTypes) {
        override fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            anim(std_seqs.god_spell)
            val spell = attack.spell.obj

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, GOD_SPELL_CLIENT_DELAY, null, 8)
                manager.queueSplashHit(this, target, spell, GOD_SPELL_CLIENT_DELAY)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val baseMaxHit = godSpellMaxHit(player, effect)
            val damage = manager.rollMaxHit(this, target, attack, castResult, baseMaxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = GOD_SPELL_CLIENT_DELAY,
                castSound = null,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 0,
                hitSound = null,
            )
            if (damage > 0) {
                effect.apply(target)
            }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, GOD_SPELL_CLIENT_DELAY)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    /** Tele Block: only on players, 5 minutes (halved by Protect from Magic). */
    private class TeleBlockAttack(manager: SpellAttackManager, objTypes: ObjTypeList) :
        StandardSpellAttack(manager, objTypes) {
        override fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            if (target !is Player) {
                reject(TELEBLOCK_PLAYERS_ONLY_MESSAGE)
                return
            }
            if (target.isTeleblocked()) {
                reject(TELEBLOCK_ALREADY_MESSAGE)
                return
            }
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            castAnim(attack, std_seqs.tele_block_staff, std_seqs.tele_block)
            val spell = attack.spell.obj

            if (manager.rollSplash(this, target, attack, castResult)) {
                val proj =
                    manager.spawnProjectile(
                        this,
                        target,
                        std_spots.tele_block_fail,
                        projanims.magic_spell,
                    )
                val (serverDelay, clientDelay) = proj.durations
                statAdvance(stats.magic, TELEBLOCK_FAIL_BONUS_XP)
                manager.playSplashFx(this, target, clientDelay, std_synths.tele_block_cast, 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.stopCombat(this)
                return
            }

            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    std_spots.tele_block_travel,
                    projanims.magic_spell,
                )
            val clientDelay = proj.durations.clientDelay
            val protectMagic = target.vars[varbits.protect_from_magic] != 0
            val cycles = if (protectMagic) TELEBLOCK_PROTECTED_CYCLES else TELEBLOCK_CYCLES
            target.teleblock(cycles)
            target.mes(
                "A Tele Block has been cast on you by ${player.displayName}. " +
                    "It will expire in ${formatTeleblockTime(cycles)}."
            )
            statAdvance(stats.magic, TELEBLOCK_HIT_BONUS_XP)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = std_synths.tele_block_cast,
                soundRadius = 8,
                hitSpot = std_spots.tele_block_impact,
                hitSpotHeight = 0,
                hitSound = std_synths.tele_block_impact,
            )
            manager.stopCombat(this)
        }
    }

    /** The extra effect a god spell has when it hits, per the wiki. */
    enum class GodSpellEffect(val cape: () -> List<ObjType>) {
        /** Saradomin Strike drains 1 Prayer point from players. */
        Saradomin({ listOf(std_objs.saradomin_cape, std_objs.saradomin_cape_imbued) }) {
            override fun apply(target: PathingEntity) {
                if (target is Player) {
                    target.statSub(stats.prayer, 1, 0)
                }
            }
        },

        /** Claws of Guthix lowers Defence by 5%. */
        Guthix({ listOf(std_objs.guthix_cape, std_objs.guthix_cape_imbued) }) {
            override fun apply(target: PathingEntity) {
                StatDrains.drainPercent(target, stats.defence, 5)
            }
        },

        /** Flames of Zamorak lowers Magic by 5%. */
        Zamorak({ listOf(std_objs.zamorak_cape, std_objs.zamorak_cape_imbued) }) {
            override fun apply(target: PathingEntity) {
                StatDrains.drainPercent(target, stats.magic, 5)
            }
        };

        abstract fun apply(target: PathingEntity)
    }

    /** Stat drains that work on both players and npcs. */
    internal object StatDrains {
        fun isLowered(target: PathingEntity, stat: StatType): Boolean =
            when (target) {
                is Player -> target.stat(stat) < target.statBase(stat)
                is Npc -> npcLevel(target, stat) < npcBase(target, stat)
            }

        /** Lowers [stat] by [percent]% of its base level (at least 1). */
        fun drainPercent(target: PathingEntity, stat: StatType, percent: Int) {
            when (target) {
                is Player -> {
                    val amount = max(1, target.statBase(stat) * percent / 100)
                    target.statSub(stat, amount, 0)
                }
                is Npc -> {
                    val amount = max(1, npcBase(target, stat) * percent / 100)
                    setNpcLevel(target, stat, max(0, npcLevel(target, stat) - amount))
                }
            }
        }

        private fun npcLevel(npc: Npc, stat: StatType): Int =
            when (stat.id) {
                stats.attack.id -> npc.attackLvl
                stats.strength.id -> npc.strengthLvl
                stats.defence.id -> npc.defenceLvl
                stats.magic.id -> npc.magicLvl
                else -> 0
            }

        private fun npcBase(npc: Npc, stat: StatType): Int =
            when (stat.id) {
                stats.attack.id -> npc.baseAttackLvl
                stats.strength.id -> npc.baseStrengthLvl
                stats.defence.id -> npc.baseDefenceLvl
                stats.magic.id -> npc.baseMagicLvl
                else -> 0
            }

        private fun setNpcLevel(npc: Npc, stat: StatType, level: Int) {
            when (stat.id) {
                stats.attack.id -> npc.attackLvl = level
                stats.strength.id -> npc.strengthLvl = level
                stats.defence.id -> npc.defenceLvl = level
                stats.magic.id -> npc.magicLvl = level
            }
        }
    }

    companion object {
        const val ALREADY_HELD_MESSAGE: String = "Your target is already held by a magical force."
        const val IMMUNE_MESSAGE: String = "The target is currently immune to that spell."
        const val CRUMBLE_UNDEAD_MESSAGE: String =
            "This spell only affects skeletons, zombies, ghosts and shades"
        const val TELEBLOCK_PLAYERS_ONLY_MESSAGE: String =
            "You can only cast this spell on other players."
        const val TELEBLOCK_ALREADY_MESSAGE: String =
            "That player is already being affected by this spell."
        const val CRUMBLE_UNDEAD_MAX_HIT: Int = 15
        const val IBAN_BLAST_MAX_HIT: Int = 25
        const val GOD_SPELL_MAX_HIT: Int = 20
        const val GOD_SPELL_CHARGED_MAX_HIT: Int = 30
        const val GOD_SPELL_CLIENT_DELAY: Int = 60

        /** Tele Block gives 95 experience on a hit and 84 on a splash (80 is the base cast xp). */
        const val TELEBLOCK_HIT_BONUS_XP: Double = 15.0
        const val TELEBLOCK_FAIL_BONUS_XP: Double = 4.0

        fun magicDartMaxHit(magicLvl: Int): Int = 10 + magicLvl / 10

        fun godSpellMaxHit(player: Player, effect: GodSpellEffect): Int {
            val cape = player.back
            val wearingCape = cape != null && effect.cape().any { it.id == cape.id }
            return if (player.isChargeActive() && wearingCape) {
                GOD_SPELL_CHARGED_MAX_HIT
            } else {
                GOD_SPELL_MAX_HIT
            }
        }

        private fun statName(stat: StatType): String =
            when (stat.id) {
                stats.attack.id -> "attack"
                stats.strength.id -> "strength"
                stats.defence.id -> "defence"
                else -> "magic"
            }
    }
}

typealias std_seqs = StandardSpellSeqs

typealias std_spots = StandardSpellSpots

typealias std_synths = StandardSpellSynths

typealias std_objs = StandardSpellObjs

object StandardSpellSeqs : SeqReferences() {
    val confuse = find("human_castconfuse")
    val confuse_staff = find("human_castconfuse_staff")
    val weaken = find("human_castweaken")
    val weaken_staff = find("human_castweaken_staff")
    val curse = find("human_castcurse")
    val curse_staff = find("human_castcurse_staff")
    val enfeeble = find("human_castenfeeble")
    val enfeeble_staff = find("human_castenfeeble_staff")
    val stun = find("human_caststun")
    val stun_staff = find("human_caststun_staff")
    val entangle = find("human_castentangle")
    val entangle_staff = find("human_castentangle_staff")
    val crumble = find("human_castcrumbleundead")
    val crumble_staff = find("human_castcrumbleundead_staff")
    val magic_dart = find("slayer_magicdart_cast")
    val iban_blast = find("human_castibanblast")
    val god_spell = find("human_casting")
    val tele_block = find("human_casting_tele_block")
    val tele_block_staff = find("human_casting_tele_block_staff")
}

object StandardSpellSpots : SpotanimReferences() {
    val confuse_casting = find("confuse_casting")
    val confuse_travel = find("confuse_travel")
    val confuse_impact = find("confuse_impact")
    val weaken_casting = find("weaken_casting")
    val weaken_travel = find("weaken_travel")
    val weaken_impact = find("weaken_impact")
    val curse_casting = find("curse_casting")
    val curse_travel = find("curse_travel")
    val curse_impact = find("curse_impact")
    val vulnerability_casting = find("vulnerability_casting")
    val vulnerability_travel = find("vulnerability_travel")
    val vulnerability_impact = find("vulnerability_impact")
    val enfeeble_casting = find("enfeeble_casting")
    val enfeeble_travel = find("enfeeble_travel")
    val enfeeble_impact = find("enfeeble_impact")
    val stun_casting = find("stun_casting")
    val stun_travel = find("stun_travel")
    val stun_impact = find("stun_impact")
    val entangle_casting = find("entangle_casting")
    val entangle_travel = find("entangle_travel")
    val entangle_impact = find("entangle_impact")
    val snare_impact = find("snare_impact")
    val bind_impact = find("bind_impact")
    val crumble_casting = find("crumbleundead_casting")
    val crumble_travel = find("crumbleundead_travel")
    val crumble_impact = find("crumbleundead_impact")
    val magic_dart_travel = find("slayer_magicdart_travel")
    val magic_dart_impact = find("slayer_magicdart_impact")
    val iban_casting = find("ibanblast_casting")
    val iban_travel = find("ibanblast_travel")
    val iban_impact = find("ibanblast_impact")
    val saradomin_strike = find("saradomin_lightning")
    val claws_of_guthix = find("gunthix_claw")
    val flames_of_zamorak = find("zamorak_flame")

    /** Spotanim 1299: the Tele Block projectile (named `teleport_reverse` in the cache). */
    val tele_block_travel = find("teleport_reverse")
    val tele_block_fail = find("tele_block_travel_forfail")
    val tele_block_impact = find("tele_block_impact")
}

object StandardSpellSynths : SynthReferences() {
    val confuse_cast = find("confuse_cast_and_fire")
    val confuse_hit = find("confuse_hit")
    val weaken = find("weaken_all")
    val curse_cast = find("curse_cast_and_fire")
    val curse_hit = find("curse_hit")
    val vulnerability = find("vulnerability_all")
    val enfeeble_cast = find("enfeeble_cast_and_fire")
    val enfeeble_hit = find("enfeeble_hit")
    val stun = find("stun_all")
    val bind_cast = find("bind_cast")
    val bind_impact = find("bind_impact")
    val crumble_cast = find("crumble_cast_and_fire")
    val crumble_hit = find("crumble_hit")
    val magic_dart_hit = find("magic_dart_hit")
    val tele_block_cast = find("teleportblock_cast")
    val tele_block_impact = find("teleportblock_impact")
}

object StandardSpellObjs : ObjReferences() {
    val saradomin_cape = find("saradomin_cape")
    val guthix_cape = find("guthix_cape")
    val zamorak_cape = find("zamorak_cape")
    val saradomin_cape_imbued = find("ma2_saradomin_cape")
    val guthix_cape_imbued = find("ma2_guthix_cape")
    val zamorak_cape_imbued = find("ma2_zamorak_cape")
}
