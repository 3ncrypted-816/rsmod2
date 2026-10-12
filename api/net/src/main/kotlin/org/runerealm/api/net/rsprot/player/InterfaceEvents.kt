package org.runerealm.api.net.rsprot.player

import org.runerealm.game.type.comp.UnpackedComponentType
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.game.ui.UserInterfaceMap

internal object InterfaceEvents {
    fun isEnabled(
        ui: UserInterfaceMap,
        component: UnpackedComponentType,
        comsub: Int,
        event: IfEvent,
    ): Boolean {
        val verifyStaticEvents = comsub == -1
        return if (verifyStaticEvents) {
            component.hasEvent(event)
        } else {
            ui.hasEvent(component, comsub, event)
        }
    }
}
