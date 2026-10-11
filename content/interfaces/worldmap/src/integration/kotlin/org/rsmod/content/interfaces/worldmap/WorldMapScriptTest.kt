package org.rsmod.content.interfaces.worldmap

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.testing.GameTestState

class WorldMapScriptTest {
    @Test
    fun GameTestState.`orb toggles the world map and close button closes it`() =
        runGameTest(WorldMapScript::class) {
            player.ifOpenOverlay(interfaces.orbs, components.toplevel_target_orbs)

            player.ifButton(worldmap_components.orb)
            advance(ticks = 1)
            assertTrue(player.ui.containsOverlay(worldmap_interfaces.worldmap))

            player.ifButton(worldmap_components.close)
            advance(ticks = 1)
            assertFalse(player.ui.containsOverlay(worldmap_interfaces.worldmap))

            player.ifButton(worldmap_components.orb)
            advance(ticks = 1)
            player.ifButton(worldmap_components.orb)
            advance(ticks = 1)
            assertFalse(player.ui.containsOverlay(worldmap_interfaces.worldmap))
        }
}
