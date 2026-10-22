package org.runerealm.api.recoil

import kotlin.math.min
import org.runerealm.api.player.ring
import org.runerealm.api.player.vars.intVarBit
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.obj.ObjReferences
import org.runerealm.api.type.refs.varbit.VarBitReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.varbit.VarBitType
import org.runerealm.game.type.varp.VarpType

/*
 * [Ring of recoil](https://oldschool.runescape.wiki/w/Ring_of_recoil) and the recoil effect of a
 * charged [ring of suffering](https://oldschool.runescape.wiki/w/Ring_of_suffering):
 * - When the wearer takes damage from a player or npc, `10% + 1` of that damage (rounded down) is
 *   dealt back to the attacker. The attack must deal damage for the recoil to happen.
 * - A ring of recoil can deal 40 damage in total before it shatters. The damage left is tracked per
 *   player, not per ring, so a newly worn ring carries on from the previous one.
 * - A ring of suffering is charged with rings of recoil (40 charges each, up to 100,000). Each point
 *   of recoil damage uses one charge, and the ring reverts to its uncharged form once empty.
 */

/** Total damage a ring of recoil can deal before shattering. */
public const val RECOIL_RING_CHARGES: Int = 40

/** Maximum recoil charges a ring of suffering can hold. */
public const val SUFFERING_MAX_CHARGES: Int = 100_000

/** @return the recoil damage dealt back to an attacker that hit for [damage]. */
public fun recoilDamage(damage: Int): Int = if (damage <= 0) 0 else damage / 10 + 1

/** Damage the player's ring of recoil has already dealt, from `0` to [RECOIL_RING_CHARGES]. */
internal var Player.recoilDamageDealt: Int by intVarp(recoil_varps.ring_of_recoil_damage)

/** Recoil charges stored in the player's ring of suffering. */
internal var Player.sufferingCharges: Int by intVarBit(recoil_varbits.suffering_charges)

/** @return the damage the player's ring(s) of recoil can still deal before shattering. */
public val Player.recoilChargesLeft: Int
    get() = RECOIL_RING_CHARGES - recoilDamageDealt

/** @return the recoil charges held by the player's ring of suffering. */
public val Player.sufferingRecoilCharges: Int
    get() = sufferingCharges

/** @return `true` if the player is wearing a ring with an active recoil effect. */
public fun Player.isWearingRecoilRing(): Boolean {
    val ring = ring ?: return false
    return ring.id == recoil_objs.ring_of_recoil.id ||
        (SufferingRing.byCharged(ring.id) != null && sufferingCharges > 0)
}

/** The ring of suffering variants: each charged (recoil) form and its uncharged form. */
internal enum class SufferingRing(val uncharged: ObjType, val charged: ObjType) {
    Regular(recoil_objs.suffering, recoil_objs.suffering_r),
    Imbued(recoil_objs.suffering_i, recoil_objs.suffering_ri),
    SoulWars(recoil_objs.suffering_sw, recoil_objs.suffering_sw_r);

    companion object {
        fun byCharged(id: Int): SufferingRing? = entries.firstOrNull { it.charged.id == id }

        fun byUncharged(id: Int): SufferingRing? = entries.firstOrNull { it.uncharged.id == id }

        fun byAny(id: Int): SufferingRing? = byCharged(id) ?: byUncharged(id)
    }
}

internal fun Player.addSufferingCharges(charges: Int): Int {
    val added = min(charges, SUFFERING_MAX_CHARGES - sufferingCharges)
    sufferingCharges += added
    return added
}

internal object recoil_objs : ObjReferences() {
    val ring_of_recoil: ObjType = find("ring_of_recoil")
    val ring_of_recoil_cert: ObjType = find("cert_ring_of_recoil")
    val suffering: ObjType = find("zenyte_ring_enchanted")
    val suffering_r: ObjType = find("zenyte_ring_enchanted_recoil")
    val suffering_i: ObjType = find("nzone_zenyte_ring_enchanted")
    val suffering_ri: ObjType = find("nzone_zenyte_ring_enchanted_recoil")
    val suffering_sw: ObjType = find("sw_zenyte_ring_enchanted")
    val suffering_sw_r: ObjType = find("sw_zenyte_ring_enchanted_recoil")
}

internal object recoil_varps : VarpReferences() {
    val ring_of_recoil_damage: VarpType = find("ring_of_recoil_damage")
}

internal object recoil_varbits : VarBitReferences() {
    val suffering_charges: VarBitType = find("charges_ring_of_suffering_quantity")
}

internal object RecoilVarpBuilds : VarpBuilder() {
    init {
        build("ring_of_recoil_damage")
    }
}
