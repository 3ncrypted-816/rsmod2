package org.runerealm.content.interfaces.settings

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.testing.GameTestState
import org.runerealm.content.interfaces.settings.scripts.QuestListSettingsScript
import org.runerealm.content.interfaces.settings.scripts.questlist_settings_components

class QuestListSettingsTest {
    @Test
    fun GameTestState.`settings button opens all settings`() =
        runGameTest(QuestListSettingsScript::class) {
            player.ifOpenOverlay(interfaces.questlist, components.toplevel_target_side2)
            player.ifButton(questlist_settings_components.settings_button)
            advance()
            assertTrue(player.ui.containsOverlay(interfaces.settings))
        }
}
