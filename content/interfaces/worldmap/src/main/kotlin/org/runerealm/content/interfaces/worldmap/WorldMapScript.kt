package org.runerealm.content.interfaces.worldmap

import jakarta.inject.Inject
import org.runerealm.api.config.refs.components
import org.runerealm.api.player.ui.ifCloseOverlay
import org.runerealm.api.player.ui.ifOpenOverlay
import org.runerealm.api.player.ui.ifSetEvents
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.interf.InterfaceReferences
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.type.comp.ComponentType
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.game.ui.Component
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

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
        val floating = worldmap_components.map_container.toComponent()
        if (ui.gameframe.getOrNull(floating)?.packed?.let { it != -1 } ?: true) {
            // Resizable layouts have a floating map container; the 8 edge/corner handles resize it.
            ifOpenOverlay(worldmap_interfaces.worldmap, worldmap_components.map_container, eventBus)
            ifSetEvents(worldmap_components.content, 0..<RESIZE_HANDLES, IfEvent.Depth1)
        } else {
            // Fixed mode has no floating container, so the map covers the game view instead.
            ifOpenOverlay(worldmap_interfaces.worldmap, components.mainmodal, eventBus)
        }
    }

    private fun Player.closeWorldMap() {
        ifCloseOverlay(worldmap_interfaces.worldmap, eventBus)
    }

    private fun ComponentType.toComponent(): Component = Component(packed)

    private companion object {
        const val RESIZE_HANDLES = 8
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
    val content = find("worldmap:content")
}
