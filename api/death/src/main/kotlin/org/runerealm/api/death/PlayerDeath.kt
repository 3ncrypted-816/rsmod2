package org.runerealm.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.runerealm.api.area.checker.WildernessArea
import org.runerealm.api.config.constants
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.jingles
import org.runerealm.api.config.refs.midis
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.queues
import org.runerealm.api.config.refs.seqs
import org.runerealm.api.config.refs.spotanims
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.config.refs.varns
import org.runerealm.api.config.refs.varps
import org.runerealm.api.freeze.clearFreeze
import org.runerealm.api.hunt.NpcSearch
import org.runerealm.api.hunt.PlayerSearch
import org.runerealm.api.npc.hit.modifier.NpcHitModifier
import org.runerealm.api.npc.hit.queueHit
import org.runerealm.api.npc.vars.typePlayerUidVarn
import org.runerealm.api.player.deathResetTimers
import org.runerealm.api.player.disablePrayers
import org.runerealm.api.player.hit.queueHit
import org.runerealm.api.player.ironman.HARDCORE_DEATH_MESSAGE
import org.runerealm.api.player.ironman.isHardcoreIronman
import org.runerealm.api.player.ironman.revokeHardcoreStatus
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.repo.obj.ObjRepository
import org.runerealm.game.entity.Npc
import org.runerealm.game.entity.Player
import org.runerealm.game.hit.HitType
import org.runerealm.game.interact.InteractionOp
import org.runerealm.game.inv.InvObj
import org.runerealm.game.inv.Inventory
import org.runerealm.game.type.hunt.HuntVis
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.stat.StatTypeList
import org.runerealm.game.type.util.UncheckedType
import org.runerealm.map.CoordGrid

@Singleton
public class PlayerDeath
@Inject
constructor(
    private val statTypes: StatTypeList,
    private val objTypes: ObjTypeList,
    private val objRepo: ObjRepository,
    private val playerSearch: PlayerSearch,
    private val npcSearch: NpcSearch,
    private val npcHitModifier: NpcHitModifier,
) {
    private var Player.specialAttackType by intVarp(varps.sa_attack)

    public suspend fun death(access: ProtectedAccess) {
        access.deathSequence()
    }

    private suspend fun ProtectedAccess.deathSequence() {
        activateRetribution()
        val respawn = CoordGrid(0, 50, 50, 21, 18)
        val randomRespawn = mapFindSquareLineOfWalk(respawn, minRadius = 0, maxRadius = 2)
        stopAction()
        delay(2)
        anim(seqs.human_death)
        delay(4)
        combatClearQueue()
        clearQueue(queues.death)
        midiSong(midis.stop_music)
        midiJingle(jingles.death_jingle_2)
        mes("Oh dear, you are dead!")
        // All deaths are currently unsafe; safe minigame deaths do not exist yet.
        if (player.isHardcoreIronman) {
            player.revokeHardcoreStatus()
            mes(HARDCORE_DEATH_MESSAGE)
        }
        dropWildernessItems(player.coords)
        telejump(randomRespawn ?: respawn)
        resetAnim()
        // TODO: Gravestones for deaths outside the Wilderness.
        resetPlayerState(statTypes)
        restoreToplevelTabs(
            components.toplevel_target_pvp_icons,
            components.toplevel_target_side1,
            components.toplevel_target_side2,
            components.toplevel_target_side4,
            components.toplevel_target_side5,
            components.toplevel_target_side6,
            components.toplevel_target_side9,
            components.toplevel_target_side8,
            components.toplevel_target_side7,
            components.toplevel_target_side10,
            components.toplevel_target_side11,
            components.toplevel_target_side12,
            components.toplevel_target_side13,
        )
    }

    /**
     * [Retribution](https://oldschool.runescape.wiki/w/Retribution): on death, deals up to 25% of
     * the player's base Prayer level (rounded down; it can hit 0) to enemies within one tile. In
     * multi-way combat every adjacent enemy is hit; in single-way combat only the player's opponent
     * (the player who dealt the most damage, or the npc fighting them).
     *
     * Players are only hit where PvP is allowed: both players must be in the Wilderness.
     */
    private fun ProtectedAccess.activateRetribution() {
        if (player.vars[varbits.retribution] == 0) {
            return
        }
        spotanim(spotanims.retribution)
        val maxHit = statBase(stats.prayer) / RETRIBUTION_DIVISOR
        val multi = mapMultiway()
        val hero = findHero()
        val coords = player.coords

        val players =
            playerSearch.findAll(coords, RETRIBUTION_RADIUS, HuntVis.Off).filter {
                it !== player &&
                    it.coords.level == coords.level &&
                    WildernessArea.contains(coords) &&
                    WildernessArea.contains(it.coords) &&
                    (multi || it === hero)
            }
        for (target in players.toList()) {
            target.queueHit(player, delay = 1, HitType.Typeless, random.of(0, maxHit))
        }

        val npcs =
            npcSearch.findAllAny(coords, RETRIBUTION_RADIUS, HuntVis.Off).filter {
                it.coords.level == coords.level &&
                    it.hitpoints > 0 &&
                    it.type.hasOp(InteractionOp.Op2) &&
                    (multi || it.aggressivePlayer == player.uid)
            }
        for (target in npcs.toList()) {
            target.queueHit(player, 1, HitType.Typeless, random.of(0, maxHit), npcHitModifier)
        }
    }

    /**
     * Wilderness deaths per the wiki: the player keeps their 3 most valuable items (none while
     * skulled), plus one more with Protect Item active. Everything else, along with bones, drops
     * for the player who dealt the most damage (or for the dead player, if no player did).
     */
    @OptIn(UncheckedType::class)
    private fun ProtectedAccess.dropWildernessItems(deathCoords: CoordGrid) {
        if (!WildernessArea.contains(deathCoords)) {
            return
        }
        val receiver = findHero() ?: player
        val skulled =
            player.skullIcon == constants.skullicon_default ||
                player.skullIcon == constants.skullicon_red
        var keep = if (skulled) 0 else ITEMS_KEPT_ON_DEATH
        if (player.vars[varbits.protect_item] == 1) {
            keep++
        }

        val held = mutableListOf<Pair<Inventory, Int>>()
        for (inv in listOf(player.inv, player.worn)) {
            for (slot in inv.indices) {
                if (inv[slot] != null) {
                    held += inv to slot
                }
            }
        }
        held.sortByDescending { (inv, slot) -> objTypes[checkNotNull(inv[slot])].cost }

        for ((inv, slot) in held) {
            val obj = checkNotNull(inv[slot])
            val kept = minOf(obj.count, keep)
            keep -= kept
            val lost = obj.count - kept
            inv[slot] = if (kept > 0) InvObj(obj.id, kept, obj.vars) else null
            if (lost > 0) {
                val dropped = InvObj(obj.id, lost, obj.vars)
                if (objTypes[dropped].stackable || lost == 1) {
                    objRepo.add(dropped, deathCoords, constants.lootdrop_duration, receiver)
                } else {
                    repeat(lost) {
                        objRepo.add(
                            InvObj(obj.id, 1, obj.vars),
                            deathCoords,
                            constants.lootdrop_duration,
                            receiver,
                        )
                    }
                }
            }
        }
        objRepo.add(objs.bones, deathCoords, constants.lootdrop_duration, receiver)
    }

    private fun ProtectedAccess.resetPlayerState(stats: StatTypeList) {
        player.disablePrayers()
        player.deathResetTimers()
        player.clearFreeze()

        player.specialAttackType = 0
        player.skullIcon = null

        rebuildAppearance()

        camReset()
        statRestoreAll(stats.values)
        minimapReset()
    }

    private companion object {
        const val ITEMS_KEPT_ON_DEATH = 3
        const val RETRIBUTION_DIVISOR = 4
        const val RETRIBUTION_RADIUS = 1
    }
}

private var Npc.aggressivePlayer by typePlayerUidVarn(varns.aggressive_player)
