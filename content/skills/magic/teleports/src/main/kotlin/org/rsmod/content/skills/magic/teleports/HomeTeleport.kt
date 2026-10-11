package org.rsmod.content.skills.magic.teleports

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.synth.SynthReferences
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * [Lumbridge Home Teleport](https://oldschool.runescape.wiki/w/Lumbridge_Home_Teleport): no runes,
 * no experience, a 24-tick cast that cannot be started in combat and is interrupted by combat.
 *
 * This server intentionally has **no** 30-minute cooldown.
 */
class HomeTeleport @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(home_teleport_components.standard) {
            protectedAccess.launch(player) { homeTeleport() }
        }
    }
}

internal suspend fun ProtectedAccess.homeTeleport() {
    if (player.inRecentCombat(mapClock)) {
        mes(RECENT_COMBAT_MESSAGE)
        return
    }
    val start = mapClock
    for (stage in HOME_TELEPORT_STAGES) {
        stage.synth?.let(::soundSynth)
        anim(stage.seq)
        spotanim(stage.spot)
        delay(stage.ticks)
        if (player.attackedSince(start)) {
            resetAnim()
            spotanim(null)
            return
        }
    }
    telejump(LUMBRIDGE_HOME)
    resetAnim()
}

private fun Player.inRecentCombat(mapClock: Int): Boolean =
    lastCombat != 0 && lastCombat + constants.combat_activecombat_delay > mapClock

private fun Player.attackedSince(start: Int): Boolean = lastCombat != 0 && lastCombat >= start

private var Player.lastCombat by intVarp(varps.lastcombat)

const val RECENT_COMBAT_MESSAGE = "You can't use this teleport after recent combat."

/** Destination per the wiki infobox map: 3221, 3218. */
val LUMBRIDGE_HOME = CoordGrid(3221, 3218, 0)

private class HomeTeleportStage(
    val seq: org.rsmod.game.type.seq.SeqType,
    val spot: org.rsmod.game.type.spot.SpotanimType,
    val synth: org.rsmod.game.type.synth.SynthType?,
    val ticks: Int,
)

/** Five animation stages totalling the wiki's 24-tick cast. */
private val HOME_TELEPORT_STAGES =
    listOf(
        HomeTeleportStage(home_seqs.chalk, home_spots.chalk, home_synths.chalk, 7),
        HomeTeleportStage(home_seqs.sit, home_spots.chalk_static, home_synths.sitdown, 5),
        HomeTeleportStage(home_seqs.book, home_spots.book, home_synths.book, 4),
        HomeTeleportStage(home_seqs.recite, home_spots.portal, home_synths.portal, 4),
        HomeTeleportStage(home_seqs.teleport, home_spots.teleport, null, 4),
    )

typealias home_teleport_components = HomeTeleportComponents

object HomeTeleportComponents : ComponentReferences() {
    val standard = find("magic_spellbook:teleport_home_standard")
}

internal object home_seqs : SeqReferences() {
    val chalk = find("aide_drawing_chalk_circle")
    val sit = find("aide_sitting_down_crosslegged")
    val book = find("aide_player_getting_book")
    val recite = find("aide_reciting_incantation")
    val teleport = find("aide_player_teleporting")
}

internal object home_spots : SpotanimReferences() {
    val chalk = find("aide_chalk_circle")
    val chalk_static = find("aide_chalk_circle_static")
    val book = find("aide_player_book_get")
    val portal = find("aide_player_book_portal")
    val teleport = find("aide_player_teleport")
}

internal object home_synths : SynthReferences() {
    val chalk = find("aide_teleport_chalk")
    val sitdown = find("aide_teleport_sitdown")
    val book = find("aide_teleport_book")
    val portal = find("aide_teleport_portal")
}
