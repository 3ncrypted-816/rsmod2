package org.runerealm.content.generic.locs.banks

import org.runerealm.api.config.refs.content
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.player.ironman.ULTIMATE_IRONMAN_BANK_MESSAGE
import org.runerealm.api.player.ironman.isUltimateIronman
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.script.onOpLoc2
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

class BankBooth : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc2(content.bank_booth) { openBank() }
    }

    private fun ProtectedAccess.openBank() {
        if (player.isUltimateIronman) {
            mes(ULTIMATE_IRONMAN_BANK_MESSAGE)
            return
        }
        ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
    }
}
