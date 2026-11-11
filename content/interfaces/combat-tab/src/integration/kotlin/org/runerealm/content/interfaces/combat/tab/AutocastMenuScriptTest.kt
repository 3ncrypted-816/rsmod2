package org.runerealm.content.interfaces.combat.tab

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.runerealm.api.config.refs.objs
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.api.spells.autocast.AutocastWeapons
import org.runerealm.api.spells.autocast.scripts.AutocastScript
import org.runerealm.api.testing.GameTestState
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList

/**
 * The autocast staff rules used by [AutocastMenuScript]. Test caches have no weapon categories, so
 * the menu itself (which needs a staff category) is checked in the client.
 */
class AutocastMenuScriptTest {
    @Test
    fun GameTestState.`ancient spells can only be autocast with ancient weapons`() =
        runInjectedGameTest(Deps::class, null, AutocastScript::class) { deps ->
            val barrage = deps.autocastId(objs.spell_ice_barrage)
            val strike = deps.autocastId(objs.spell_wind_strike)
            for (weapon in listOf(objs.ancient_staff, objs.dragon_hunter_wand)) {
                assertTrue(deps.canAutocast(weapon, barrage)) { "$weapon" }
                assertTrue(deps.canAutocast(weapon, strike)) { "$weapon" }
            }
            assertFalse(deps.canAutocast(objs.air_staff, barrage))
            assertTrue(deps.canAutocast(objs.air_staff, strike))
        }

    @Test
    fun GameTestState.`ancient combat spells have autocast ids`() =
        runInjectedGameTest(Deps::class, null, AutocastScript::class) { deps ->
            val ids = (0..MAX_ID).mapNotNull { deps.registry.getAutocastSpell(it)?.obj?.id }
            val ancients =
                listOf(
                    objs.spell_smoke_rush,
                    objs.spell_shadow_burst,
                    objs.spell_blood_blitz,
                    objs.spell_ice_barrage,
                )
            assertEquals(emptyList<ObjType>(), ancients.filter { it.id !in ids })
        }

    class Deps
    @Inject
    constructor(
        val registry: MagicSpellRegistry,
        private val weapons: AutocastWeapons,
        private val objTypes: ObjTypeList,
    ) {
        fun autocastId(spell: ObjType): Int =
            (0..MAX_ID).first { registry.getAutocastSpell(it)?.obj?.id == spell.id }

        fun canAutocast(weapon: ObjType, autocastId: Int): Boolean =
            weapons.canStaffAutocast(objTypes[weapon], autocastId)
    }

    private companion object {
        const val MAX_ID = 58
    }
}
