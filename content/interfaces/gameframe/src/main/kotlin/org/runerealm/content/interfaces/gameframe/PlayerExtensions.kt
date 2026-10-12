package org.runerealm.content.interfaces.gameframe

import org.runerealm.api.player.ui.ifMoveSub
import org.runerealm.api.player.ui.ifOpenSub
import org.runerealm.api.player.ui.ifOpenTop
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.type.interf.IfSubType
import org.runerealm.game.ui.Component
import org.runerealm.game.ui.UserInterfaceMap

internal fun Player.openGameframe(gameframe: Gameframe, eventBus: EventBus) {
    ui.setGameframe(gameframe.mappings)
    for (overlay in gameframe.overlays) {
        ifOpenSub(overlay.interf, overlay.target, IfSubType.Overlay, eventBus)
    }
}

internal fun Player.moveGameframe(from: Gameframe, dest: Gameframe, eventBus: EventBus) {
    ifOpenTop(dest.topLevel)
    ui.setGameframe(dest.mappings)
    val moveComponents = StandardOverlays.move
    for (moveComponent in moveComponents) {
        val target = Component(moveComponent.packed)
        val sourceComponent =
            from.mappings[target]
                ?: error("Expected move target in source mapping: '${moveComponent.internalName}'")
        val destComponent =
            dest.mappings[target]
                ?: error("Expected move target in dest mapping: '${moveComponent.internalName}'")
        ifMoveSub(sourceComponent, destComponent, target, eventBus)
    }
}

private fun UserInterfaceMap.setGameframe(mappings: Map<Component, Component>) {
    gameframe.clear()
    for ((original, translated) in mappings) {
        gameframe[original] = translated
    }
}
