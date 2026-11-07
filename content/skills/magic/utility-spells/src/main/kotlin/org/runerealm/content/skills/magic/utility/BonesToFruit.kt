package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.objs
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.game.type.obj.ObjType
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * [Bones to Bananas](https://oldschool.runescape.wiki/w/Bones_to_Bananas) turns every normal bone
 * in the inventory into a banana.
 * [Bones to Peaches](https://oldschool.runescape.wiki/w/Bones_to_Peaches) turns every normal and
 * big bone into a peach.
 */
class BonesToFruit
@Inject
constructor(
    private val caster: UtilitySpellCaster,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val bananas = caster.spell(objs.spell_bones_to_bananas)
        onIfOverlayButton(bananas.component) {
            protectedAccess.launch(player) { convert(bananas, BANANA_BONES, utility_objs.banana) }
        }
        val peaches = caster.spell(objs.spell_bones_to_peaches)
        onIfOverlayButton(peaches.component) {
            protectedAccess.launch(player) { convert(peaches, PEACH_BONES, utility_objs.peach) }
        }
    }

    private fun ProtectedAccess.convert(spell: MagicSpell, bones: List<ObjType>, fruit: ObjType) {
        if (player.isUtilitySpellDelayed()) {
            return
        }
        val slots =
            player.inv.indices.filter { slot -> bones.any { player.inv[slot]?.id == it.id } }
        if (slots.isEmpty()) {
            mes(NO_BONES_MESSAGE)
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        player.setUtilitySpellDelay(DELAY)
        for (slot in slots) {
            val count = player.inv[slot]?.count ?: continue
            invReplace(player.inv, slot, count, fruit)
        }
        anim(utility_seqs.bones_to_fruit)
        spotanim(utility_spots.bones_to_fruit, height = 92)
        soundSynth(utility_synths.bones_to_fruit)
    }

    companion object {
        const val NO_BONES_MESSAGE: String = "You aren't holding any bones!"
        const val DELAY: Int = 3

        private val BANANA_BONES: List<ObjType> = listOf(utility_objs.bones)
        private val PEACH_BONES: List<ObjType> = listOf(utility_objs.bones, utility_objs.big_bones)
    }
}
