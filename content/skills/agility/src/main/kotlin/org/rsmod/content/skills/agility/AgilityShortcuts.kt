package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Agility shortcuts listed in [SHORTCUTS]. */
class AgilityShortcuts @Inject constructor(private val locTypes: LocTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        val byLoc =
            SHORTCUTS.flatMap { s -> s.locs.map { it to s } }.groupBy({ it.first }, { it.second })
        for (type in locTypes.values) {
            val shortcuts = byLoc[type.internalName] ?: continue
            onOpLoc1(type) { use(it.loc, shortcuts) }
        }
    }

    private suspend fun ProtectedAccess.use(loc: BoundLocInfo, candidates: List<Shortcut>) {
        // The same loc type can be used by more than one shortcut; pick the closest one.
        val shortcut = candidates.minByOrNull { it.distanceTo(loc.coords) } ?: return
        if (player.statBase(stats.agility) < shortcut.level) {
            mes("You need an Agility level of ${shortcut.level} to negotiate this obstacle.")
            return
        }
        val from = player.coords
        val dest =
            if (from.distance(shortcut.sideA) <= from.distance(shortcut.sideB)) {
                shortcut.sideB
            } else {
                shortcut.sideA
            }
        cross(shortcut, dest)
        if (shortcut.xp > 0) {
            statAdvance(stats.agility, shortcut.xp)
        }
    }

    private suspend fun ProtectedAccess.cross(shortcut: Shortcut, dest: CoordGrid) {
        val start = player.coords
        val face = faceTowards(start, dest)
        when (shortcut.style) {
            ShortcutStyle.ClimbOver -> {
                anim(shortcut_seqs.climb_over)
                exactMove(start, dest, delay1 = 0, delay2 = 60, dir = face)
                delay(2)
            }
            ShortcutStyle.FenceJump -> {
                anim(shortcut_seqs.fence_jump)
                exactMove(start, dest, delay1 = 0, delay2 = 30, dir = face)
                delay(1)
            }
            ShortcutStyle.SteppingStones -> {
                anim(shortcut_seqs.jump)
                exactMove(start, dest, delay1 = 0, delay2 = 90, dir = face)
                delay(3)
            }
            ShortcutStyle.Scramble -> {
                anim(shortcut_seqs.scramble)
                exactMove(start, dest, delay1 = 0, delay2 = 90, dir = face)
                delay(3)
            }
            ShortcutStyle.Tunnel -> {
                anim(shortcut_seqs.tunnel_enter)
                delay(2)
                anim(shortcut_seqs.tunnel_exit)
                delay(1)
            }
            ShortcutStyle.Pipe -> {
                anim(shortcut_seqs.pipe)
                exactMove(start, dest, delay1 = 0, delay2 = 150, dir = face)
                delay(5)
            }
            ShortcutStyle.Squeeze -> {
                anim(shortcut_seqs.squeeze)
                delay(2)
                anim(shortcut_seqs.unsqueeze)
            }
        }
        telejump(dest)
        resetAnim()
    }

    private fun faceTowards(from: CoordGrid, to: CoordGrid): Int {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return when {
            kotlin.math.abs(dx) >= kotlin.math.abs(dz) && dx > 0 -> FACE_EAST
            kotlin.math.abs(dx) >= kotlin.math.abs(dz) -> FACE_WEST
            dz > 0 -> FACE_NORTH
            else -> FACE_SOUTH
        }
    }

    private fun Shortcut.distanceTo(coords: CoordGrid): Int =
        minOf(coords.distance(sideA), coords.distance(sideB))

    private fun CoordGrid.distance(other: CoordGrid): Int =
        if (level != other.level) Int.MAX_VALUE
        else maxOf(kotlin.math.abs(x - other.x), kotlin.math.abs(z - other.z))

    private companion object {
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536
    }
}

internal object shortcut_seqs : SeqReferences() {
    val climb_over = find("human_walk_crumbledwall")
    val fence_jump = find("human_lowwall")
    val jump = find("human_spot_jump")
    val scramble = find("human_reachforladder")
    val tunnel_enter = find("agilty_shortcut_enter_hole")
    val tunnel_exit = find("agilty_shortcut_exit_hole")
    val pipe = find("human_doublepipesqueeze")
    val squeeze = find("human_pipesqueeze")
    val unsqueeze = find("human_pipeunsqueeze")
}
