package org.runerealm.api.teleblock

import org.runerealm.api.config.refs.timers
import org.runerealm.api.player.output.mes
import org.runerealm.api.script.onPlayerSoftTimer
import org.runerealm.game.entity.Player
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/** Ends a Tele Block once it runs out, or as soon as the player leaves the Wilderness. */
public class TeleblockScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(timers.teleblock) { player.checkTeleblock() }
    }

    private fun Player.checkTeleblock() {
        if (!processTeleblock()) {
            mes(TELEBLOCK_EXPIRED_MESSAGE)
        }
    }

    internal companion object {
        const val TELEBLOCK_EXPIRED_MESSAGE = "Your Tele Block has expired."
    }
}
