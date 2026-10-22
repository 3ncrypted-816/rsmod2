package org.runerealm.api.toxins

import org.runerealm.game.hit.Hit
import org.runerealm.game.hit.HitType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.UnpackedObjType

/**
 * [Weapon poisoning](https://oldschool.runescape.wiki/w/Poison#Weapon_poisoning):
 * - A successful (non-zero) hit with a poisoned weapon has a 1/4 chance (melee) or 1/8 chance
 *   (ranged) to poison the target.
 * - Weapon poison(p) has severity 20 (starts at 4 melee damage), (p+) 25 (5), (p++) 30 (6), and
 *   karambwan paste(kp) 30 (6, melee only).
 * - Poison from ranged weapons has its severity lowered by 14, so it starts at 2/3/4 damage.
 *
 * Poisoned items are recognised by their name suffix, e.g. "Bronze dagger(p+)" or "Iron bolts
 * (p++)".
 */
public object WeaponPoison {
    public const val MELEE_CHANCE: Int = 4
    public const val RANGED_CHANCE: Int = 8
    public const val RANGED_SEVERITY_REDUCTION: Int = 14

    /** @return the melee poison severity of [type], or `0` if it is not poisoned. */
    public fun severity(type: UnpackedObjType?): Int {
        val name = type?.name?.trimEnd()?.lowercase() ?: return 0
        return when {
            name.endsWith("(p++)") -> 30
            name.endsWith("(p+)") -> 25
            name.endsWith("(p)") -> 20
            name.endsWith("(kp)") -> 30
            else -> 0
        }
    }

    /**
     * @return the poison severity the weapon (or ammunition) behind [hit] would inflict, or `0`
     *   when the hit was not dealt with a poisoned melee or ranged weapon.
     */
    public fun severity(hit: Hit, objTypes: ObjTypeList): Int =
        when (hit.type) {
            HitType.Melee -> severity(hit.righthandType(objTypes))
            HitType.Ranged -> {
                val ammo = hit.secondaryType(objTypes)
                val base = if (ammo != null) severity(ammo) else severity(hit.righthandType(objTypes))
                if (base > 0) base - RANGED_SEVERITY_REDUCTION else 0
            }
            else -> 0
        }

    /** @return the 1-in-N chance of [hit] poisoning its target. */
    public fun chance(hit: Hit): Int = if (hit.type == HitType.Ranged) RANGED_CHANCE else MELEE_CHANCE
}
