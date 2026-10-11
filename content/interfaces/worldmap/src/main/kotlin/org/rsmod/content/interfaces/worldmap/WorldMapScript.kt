package org.rsmod.content.interfaces.worldmap

import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The world map, opened from the minimap's world map orb. The map itself is drawn entirely by the
 * client; the server only opens and closes the interface.
 */
class WorldMapScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(worldmap_components.orb) { player.toggleWorldMap() }
        onIfOverlayButton(worldmap_components.close) { player.closeWorldMap() }
        onIfOverlayButton(worldmap_components.esckey) { player.closeWorldMap() }
    }

    private fun Player.toggleWorldMap() {
        if (ui.containsOverlay(worldmap_interfaces.worldmap)) {
            closeWorldMap()
            return
        }
        ifOpenOverlay(worldmap_interfaces.worldmap, worldmap_components.map_container, eventBus)
    }

    private fun Player.closeWorldMap() {
        ifCloseOverlay(worldmap_interfaces.worldmap, eventBus)
    }
}

typealias worldmap_interfaces = WorldMapInterfaces

typealias worldmap_components = WorldMapComponents

object WorldMapInterfaces : InterfaceReferences() {
    val worldmap = find("worldmap")
}

object WorldMapComponents : ComponentReferences() {
    val orb = find("orbs:worldmap")
    val map_container = find("toplevel_osrs_stretch:map_container")
    val close = find("worldmap:close")
    val esckey = find("worldmap:esckey")
}
