package org.runerealm.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.objs.OpObjT
import org.runerealm.api.net.rsprot.player.InterfaceEvents
import org.runerealm.api.player.events.interact.ObjTDefaultEvents
import org.runerealm.api.player.output.clearMapFlag
import org.runerealm.api.player.protect.clearPendingAction
import org.runerealm.api.player.vars.ctrlMoveSpeed
import org.runerealm.api.registry.obj.ObjRegistry
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.interact.InteractionObjT
import org.runerealm.game.movement.RouteRequestCoord
import org.runerealm.game.obj.Obj
import org.runerealm.game.type.comp.ComponentTypeList
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.game.type.interf.InterfaceTypeList
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.ui.Component
import org.runerealm.map.CoordGrid

/** Handles an interface component (such as Telekinetic Grab) used on a ground obj. */
class OpObjTHandler
@Inject
constructor(
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val objRegistry: ObjRegistry,
    private val componentTypes: ComponentTypeList,
    private val interfaceTypes: InterfaceTypeList,
) : MessageHandler<OpObjT> {
    private val logger = InlineLogger()

    private val OpObjT.asComponent: Component
        get() = Component(selectedInterfaceId, selectedComponentId)

    override fun handle(player: Player, message: OpObjT) {
        if (player.isDelayed) {
            player.clearMapFlag()
            return
        }

        val coords = CoordGrid(message.x, message.z, player.level)
        val obj = findObj(player, coords, message.id)
        if (obj == null) {
            player.clearMapFlag()
            return
        }

        val interfaceType = interfaceTypes[message.asComponent]
        val componentType = componentTypes[message.asComponent]
        val isValidInterface =
            player.ui.containsOverlay(interfaceType) || player.ui.containsModal(interfaceType)
        if (!isValidInterface) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val comsub = message.selectedSub
        val targetEnabled =
            InterfaceEvents.isEnabled(player.ui, componentType, comsub, IfEvent.TgtObj)
        if (!targetEnabled) {
            player.clearMapFlag()
            player.clearPendingAction(eventBus)
            return
        }

        val selectedObj = message.selectedObj.takeIf { it != -1 }?.let(objTypes::get)
        val opKey = componentType.packed.toLong()
        val opTrigger = eventBus.contains(ObjTDefaultEvents.Op::class.java, opKey)
        val apTrigger = eventBus.contains(ObjTDefaultEvents.Ap::class.java, opKey)
        val interaction =
            InteractionObjT(
                target = obj,
                objType = selectedObj,
                component = componentType,
                comsub = comsub,
                hasOpTrigger = opTrigger,
                hasApTrigger = apTrigger,
            )
        val speed = if (message.controlKey) player.ctrlMoveSpeed() else null
        player.clearPendingAction(eventBus)
        player.resetFaceEntity()
        player.faceSquare(coords)
        player.interaction = interaction
        player.routeRequest = RouteRequestCoord(coords, clientRequest = true)
        player.tempMoveSpeed = speed
        logger.debug { "OpObjT: obj=$obj, component=$componentType, comsub=$comsub" }
    }

    private fun findObj(player: Player, coords: CoordGrid, type: Int): Obj? =
        objRegistry.findAll(coords).firstOrNull { it.type == type && it.isVisibleTo(player) }
}
