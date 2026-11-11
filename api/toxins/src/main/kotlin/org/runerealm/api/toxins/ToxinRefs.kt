package org.runerealm.api.toxins

import org.runerealm.api.type.builders.varn.VarnBuilder
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.varn.VarnReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.varn.VarnType
import org.runerealm.game.type.varp.VarpType

public typealias toxin_varps = ToxinVarps

internal typealias toxin_varns = ToxinVarns

public typealias toxin_objs = ToxinObjs

public object ToxinVarps : VarpReferences() {
    /**
     * The client's `poison` varp, which drives the hitpoints orb: `0` is healthy, a positive value
     * below [VENOM_THRESHOLD] is poison, [VENOM_THRESHOLD] and above is venom, and a negative value
     * is the remaining poison immunity. See [PlayerToxins] for the full encoding.
     */
    public val poison: VarpType = find("poison")
}

internal object ToxinVarns : VarnReferences() {
    /** An npc's poison/venom state, using the same encoding as the player `poison` varp. */
    val toxin: VarnType = find("npc_toxin")
    /** The player who poisoned or envenomed the npc, credited with its poison damage. */
    val toxin_source: VarnType = find("npc_toxin_source")
}

internal object ToxinVarnBuilds : VarnBuilder() {
    init {
        build("npc_toxin")
        build("npc_toxin_source")
    }
}

public object ToxinObjs : ObjReferences() {
    public val serpentine_helm: ObjType = find("serpentine_helm_charged")
    public val serpentine_helm_cyan: ObjType = find("serpentine_helm_charged_cyan")
    public val serpentine_helm_red: ObjType = find("serpentine_helm_charged_red")
}
