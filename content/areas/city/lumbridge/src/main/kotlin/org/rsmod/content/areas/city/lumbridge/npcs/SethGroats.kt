package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SethGroats : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SethGroatsNpcs.seth_groats) { sethDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.sethDialogue(npc: Npc) =
        startDialogue(npc) { chatNpc(happy, "M'arnin'....going to milk me cowsies!") }
}

internal object SethGroatsNpcs : NpcReferences() {
    val seth_groats = find("favour_seth_groats")
}
