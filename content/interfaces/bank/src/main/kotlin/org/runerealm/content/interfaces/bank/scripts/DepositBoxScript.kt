package org.runerealm.content.interfaces.bank.scripts

import jakarta.inject.Inject
import kotlin.math.max
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.vars.intVarBit
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.script.onIfModalButton
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.interf.InterfaceReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.type.interf.IfButtonOp
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Bank deposit boxes: the "Deposit" option on a deposit box opens a window where inventory items,
 * the whole inventory or all worn items can be deposited straight into the bank.
 */
class DepositBoxScript @Inject constructor(private val bankInv: BankInvScript) : PluginScript() {
    private var ProtectedAccess.mode by intVarBit(depositbox_varbits.mode)
    private var ProtectedAccess.requestedQuantity by intVarp(depositbox_varps.requested_quantity)

    override fun ScriptContext.startup() {
        onIfModalButton(depositbox_components.inventory) { depositSlot(it.comsub, it.op) }
        onIfModalButton(depositbox_components.deposit_inv) { with(bankInv) { depositInv() } }
        onIfModalButton(depositbox_components.deposit_worn) { with(bankInv) { depositWorn() } }
        onIfModalButton(depositbox_components.quantity_1) { mode = MODE_1 }
        onIfModalButton(depositbox_components.quantity_5) { mode = MODE_5 }
        onIfModalButton(depositbox_components.quantity_10) { mode = MODE_10 }
        onIfModalButton(depositbox_components.quantity_all) { mode = MODE_ALL }
        onIfModalButton(depositbox_components.quantity_x) {
            if (it.op == IfButtonOp.Op2) {
                requestedQuantity = countDialog()
            }
            mode = MODE_X
        }
    }

    fun open(access: ProtectedAccess) {
        access.ifOpenMainModal(depositbox_interfaces.depositbox)
        access.ifSetEvents(
            depositbox_components.inventory,
            0..<INV_SIZE,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op10,
        )
    }

    private suspend fun ProtectedAccess.depositSlot(slot: Int, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            objExamine(inv, slot)
            return
        }
        val count =
            when (op) {
                IfButtonOp.Op1 -> leftClickQuantity()
                IfButtonOp.Op2 -> 1
                IfButtonOp.Op3 -> 5
                IfButtonOp.Op4 -> 10
                IfButtonOp.Op5 -> countDialog().also { requestedQuantity = it }
                IfButtonOp.Op6 -> Int.MAX_VALUE
                else -> return
            }
        with(bankInv) { invDeposit(slot, count, inv) }
    }

    private fun ProtectedAccess.leftClickQuantity(): Int =
        when (mode) {
            MODE_5 -> 5
            MODE_10 -> 10
            MODE_X -> max(1, requestedQuantity)
            MODE_ALL -> Int.MAX_VALUE
            else -> 1
        }

    private companion object {
        const val INV_SIZE = 28

        // `depositbox_mode` values, per the `bank_depositbox_drawslot` client script.
        const val MODE_1 = 0
        const val MODE_5 = 1
        const val MODE_ALL = 2
        const val MODE_X = 3
        const val MODE_10 = 4
    }
}

typealias depositbox_interfaces = DepositBoxInterfaces

typealias depositbox_components = DepositBoxComponents

typealias depositbox_varbits = DepositBoxVarBits

typealias depositbox_varps = DepositBoxVarps

object DepositBoxInterfaces : InterfaceReferences() {
    val depositbox = find("bank_depositbox")
}

object DepositBoxComponents : ComponentReferences() {
    val inventory = find("bank_depositbox:inventory")
    val deposit_inv = find("bank_depositbox:deposit_inv")
    val deposit_worn = find("bank_depositbox:deposit_worn")
    val quantity_1 = find("bank_depositbox:1")
    val quantity_5 = find("bank_depositbox:5")
    val quantity_10 = find("bank_depositbox:10")
    val quantity_x = find("bank_depositbox:x")
    val quantity_all = find("bank_depositbox:all")
}

object DepositBoxVarBits : VarBitReferences() {
    val mode = find("depositbox_mode")
}

object DepositBoxVarps : VarpReferences() {
    val requested_quantity = find("depositbox_requestedquantity")
}
