package org.runerealm.content.generic.locs.banks

import org.runerealm.api.config.refs.content
import org.runerealm.api.type.editors.loc.LocEditor
import org.runerealm.api.type.refs.loc.LocReferences
import org.runerealm.game.type.loc.LocType

internal typealias bank_locs = BankLocs

internal object BankLocs : LocReferences() {
    val bankbooth = find("aide_bankbooth")
    val bankbooth_multi = find("aide_bankbooth_multi")
}

internal object BankLocEditor : LocEditor() {
    init {
        booth(bank_locs.bankbooth)
        booth(bank_locs.bankbooth_multi)
    }

    private fun booth(type: LocType) {
        edit(type) { contentGroup = content.bank_booth }
    }
}
