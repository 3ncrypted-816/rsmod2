package org.runerealm.content.interfaces.combat.tab

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.Spellbook
import org.runerealm.api.config.refs.components
import org.runerealm.api.config.refs.interfaces
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.righthand
import org.runerealm.api.player.ui.ifOpenOverlay
import org.runerealm.api.player.ui.ifSetEvents
import org.runerealm.api.player.vars.boolVarBit
import org.runerealm.api.player.vars.boolVarp
import org.runerealm.api.player.vars.enumVarBit
import org.runerealm.api.player.vars.intVarBit
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.api.spells.autocast.AutocastWeapons
import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.interf.InterfaceReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.WeaponCategory
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * The [autocast](https://oldschool.runescape.wiki/w/Autocast) spell menu:
 * - The combat tab's two spell buttons (offensive, and defensive with the shield icon) open the
 *   autocast menu in the combat tab. The client lists the spells the worn weapon can autocast on
 *   the current spellbook.
 * - Choosing a spell sets it as the weapon's autocast spell and returns to the combat tab. Since
 *   April 2025, a spell can be chosen without the level or runes for it.
 * - The server still checks the spell belongs to the player's spellbook and that the weapon can
 *   autocast it (for example, Ancient Magicks only with an ancient staff, sceptre or wand; see
 *   [AutocastWeapons.canStaffAutocast]).
 */
class AutocastMenuScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val spells: MagicSpellRegistry,
    private val autocast: AutocastWeapons,
) : PluginScript() {
    private var Player.autocastEnabled by boolVarBit(varbits.autocast_set)
    private var Player.autocastSpell by intVarBit(varbits.autocast_spell)
    private var Player.defensiveCasting by boolVarBit(varbits.autocast_defmode)
    private val Player.spellbook by enumVarBit<Spellbook>(varbits.spellbook)

    override fun ScriptContext.startup() {
        onIfOverlayButton(autocast_menu_components.normal) { player.openMenu(defensive = false) }
        onIfOverlayButton(autocast_menu_components.defensive) { player.openMenu(defensive = true) }
        onIfOverlayButton(autocast_menu_components.spells) { player.selectSpell(comsub) }
    }

    private fun Player.openMenu(defensive: Boolean) {
        val weapon = objTypes.getOrNull(righthand) ?: return
        val category = WeaponCategory.getOrUnarmed(weapon.weaponCategory)
        autocast.getVarBits(category) ?: return
        menuDefensive = defensive
        ifOpenOverlay(autocast_menu_interfaces.autocast, components.toplevel_target_side0, eventBus)
        ifSetEvents(autocast_menu_components.spells, 0..MAX_AUTOCAST_ID, IfEvent.Op1)
    }

    private fun Player.selectSpell(autocastId: Int) {
        val defensive = menuDefensive
        menuDefensive = false
        val weapon = objTypes.getOrNull(righthand)
        val category = WeaponCategory.getOrUnarmed(weapon?.weaponCategory)
        val staffVarBits = autocast.getVarBits(category)
        val spell = spells.getAutocastSpell(autocastId)
        if (weapon != null && staffVarBits != null && spell != null) {
            when {
                spell.spellbook != spellbook ->
                    mes("You can't autocast that spell with your current active spellbook.")
                // `canStaffAutocast` sends the message when the weapon cannot autocast the spell.
                autocast.canStaffAutocast(this, weapon, autocastId) -> {
                    autocast.set(this, staffVarBits, autocastId, defensive)
                    autocastSpell = autocastId
                    defensiveCasting = defensive
                    autocastEnabled = true
                }
            }
        }
        ifOpenOverlay(interfaces.combat_interface, components.toplevel_target_side0, eventBus)
    }

    private companion object {
        /** The highest autocast id the menu can send (the `autocast_spells` enum's keys). */
        const val MAX_AUTOCAST_ID = 58
    }
}

/** Whether the open autocast menu was opened from the defensive button. */
private var Player.menuDefensive by boolVarp(autocast_menu_varps.defensive)

typealias autocast_menu_varps = AutocastMenuVarps

object AutocastMenuVarps : VarpReferences() {
    val defensive = find("autocast_menu_defensive")
}

internal object AutocastMenuVarpBuilder : VarpBuilder() {
    init {
        build("autocast_menu_defensive") { temporary = true }
    }
}

typealias autocast_menu_interfaces = AutocastMenuInterfaces

typealias autocast_menu_components = AutocastMenuComponents

object AutocastMenuInterfaces : InterfaceReferences() {
    val autocast = find("autocast")
}

object AutocastMenuComponents : ComponentReferences() {
    val normal = find("combat_interface:autocast_normal")
    val defensive = find("combat_interface:autocast_defensive")
    val spells = find("autocast:spells")
}
