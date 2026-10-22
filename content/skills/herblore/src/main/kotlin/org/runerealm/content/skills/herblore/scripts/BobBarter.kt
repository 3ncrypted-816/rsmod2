package org.runerealm.content.skills.herblore.scripts

import org.runerealm.api.player.dialogue.Dialogue
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.script.onOpNpc1
import org.runerealm.api.script.onOpNpc4
import org.runerealm.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.runerealm.content.skills.herblore.configs.herblore_npcs
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Bob Barter (herbs) at the Grand Exchange, per the OSRS Wiki ([Bob Barter
 * (herbs)](https://oldschool.runescape.wiki/w/Bob_Barter_(herbs))): "Decant" (or the dialogue)
 * combines every carried potion into vials of 1-4 doses.
 *
 * Not implemented: decanting noted potions, buying missing vials, and the "Prices" guide.
 */
class BobBarter : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(herblore_npcs.bob_barter) { startDialogue(it.npc) { talk() } }
        onOpNpc4(herblore_npcs.bob_barter) { decant() }
    }

    private suspend fun Dialogue.talk() {
        chatNpc(happy, "Hello, chief. Want me to decant your potions for you? It's free!")
        val decant = choice2("Yes, please decant my potions.", true, "No, thanks.", false)
        if (decant) {
            access.decant()
        } else {
            chatPlayer(neutral, "No, thanks.")
        }
    }

    private suspend fun ProtectedAccess.decant() {
        val doses =
            choice4(
                "1-dose potions",
                1,
                "2-dose potions",
                2,
                "3-dose potions",
                3,
                "4-dose potions",
                4,
                title = "What would you like your potions decanted into?",
            )
        val decanted = PotionDoses.decantAll(this, doses)
        if (decanted == 0) {
            mes("Bob has nothing to decant for you.")
        } else {
            mes("Bob decants your potions into $doses-dose vials.")
        }
    }
}

/** Spawns Bob Barter (herbs) in the south-west corner of the Grand Exchange. */
object HerbloreNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BobBarter>("npcs.toml")
    }
}
