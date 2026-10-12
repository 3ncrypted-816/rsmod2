package org.runerealm.content.interfaces.settings.scripts

import jakarta.inject.Inject
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.player.vars.intVarBit
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.game.entity.Player
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/** The quest list's "Settings" button opens All Settings at the Interfaces category. */
class QuestListSettingsScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(questlist_settings_components.settings_button) {
            player.openQuestListSettings()
        }
    }

    private fun Player.openQuestListSettings() {
        val opened =
            protectedAccess.launch(this) {
                player.settingsCategory = INTERFACES_CATEGORY
                ifOpenOverlay(interfaces.settings)
            }
        if (!opened) {
            mes("Please finish what you are doing before opening the settings menu.")
        }
    }

    private companion object {
        /** The "Interfaces" settings category (enum_422), which holds the Quest List settings. */
        const val INTERFACES_CATEGORY = 6
    }
}

private var Player.settingsCategory by intVarBit(questlist_settings_varbits.settings_category)

typealias questlist_settings_components = QuestListSettingsComponents

typealias questlist_settings_varbits = QuestListSettingsVarBits

object QuestListSettingsComponents : ComponentReferences() {
    val settings_button = find("questlist:settings_button")
}

object QuestListSettingsVarBits : VarBitReferences() {
    val settings_category = find("settings_category")
}
