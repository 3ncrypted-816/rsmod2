package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.Constants
import org.runerealm.api.config.refs.objs
import org.runerealm.api.invtx.invAdd
import org.runerealm.api.player.ironman.IRONMAN_TAKE_MESSAGE
import org.runerealm.api.player.ironman.isIronman
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.repo.obj.ObjRepository
import org.runerealm.api.repo.world.WorldRepository
import org.runerealm.api.script.onApObjT
import org.runerealm.game.obj.Obj
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * [Telekinetic Grab](https://oldschool.runescape.wiki/w/Telekinetic_Grab) picks up a ground item
 * (or a whole stack of a stackable item) from up to 10 tiles away, as long as the player can see
 * it.
 * - "Too late - it's gone!" if the item disappears while the spell travels.
 * - "Your telegrab fizzles as you move too far away." if the player changes level meanwhile.
 */
class TelekineticGrab
@Inject
constructor(
    private val caster: UtilitySpellCaster,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = caster.spell(objs.spell_tele_grab)
        onApObjT(spell.component) { telegrab(spell, it.obj) }
    }

    private suspend fun ProtectedAccess.telegrab(spell: MagicSpell, obj: Obj) {
        if (player.isIronman && obj.nullableOwnerId != null && !obj.isOriginalOwner(player)) {
            mes(IRONMAN_TAKE_MESSAGE)
            return
        }
        if (!hasInvSpace(obj)) {
            mes(Constants.dm_take_invspace)
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        val level = player.level
        faceSquare(obj.coords)
        anim(utility_seqs.telegrab)
        spotanim(utility_spots.telegrab_casting, height = 92)
        soundSynth(utility_synths.telegrab)
        delay(TRAVEL_DELAY)
        if (player.level != level) {
            mes(FIZZLE_MESSAGE)
            return
        }
        spotanimMap(worldRepo, utility_spots.telegrab_impact, obj.coords)
        if (!objRepo.del(obj)) {
            mes(Constants.dm_take_taken)
            return
        }
        val add = player.invAdd(player.inv, obj.type, obj.count, autoCommit = false)
        if (add.failure) {
            mes(Constants.dm_take_invspace)
            return
        }
        add.commitAll()
    }

    private fun ProtectedAccess.hasInvSpace(obj: Obj): Boolean =
        player.invAdd(player.inv, obj.type, obj.count, autoCommit = false).success

    companion object {
        const val FIZZLE_MESSAGE: String = "Your telegrab fizzles as you move too far away."
        const val TRAVEL_DELAY: Int = 2
    }
}
