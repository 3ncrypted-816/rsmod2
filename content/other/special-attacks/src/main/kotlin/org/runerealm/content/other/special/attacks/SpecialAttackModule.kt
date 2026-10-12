package org.runerealm.content.other.special.attacks

import org.runerealm.api.specials.SpecialAttackMap
import org.runerealm.content.other.special.attacks.boost.StatBoostSpecialAttacks
import org.runerealm.content.other.special.attacks.melee.DragonLongswordSpecialAttack
import org.runerealm.content.other.special.attacks.ranged.DarkBowSpecialAttack
import org.runerealm.plugin.module.PluginModule

class SpecialAttackModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpecialAttackMap>(StatBoostSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DarkBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonLongswordSpecialAttack::class.java)
    }
}
