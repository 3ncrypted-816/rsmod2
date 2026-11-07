package org.runerealm.content.skills.magic.utility

import jakarta.inject.Inject
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.timers
import org.runerealm.api.player.output.mes
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.script.onPlayerSoftTimer
import org.runerealm.api.spells.charge.activateCharge
import org.runerealm.api.spells.charge.clearCharge
import org.runerealm.api.spells.charge.isChargeOnCooldown
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * [Charge](https://oldschool.runescape.wiki/w/Charge): for 7 minutes, god spells cast while wearing
 * the matching god cape hit up to 30 instead of 20. It can only be cast once every 60 seconds. The
 * boost itself is applied by the god spell attacks.
 */
class ChargeSpell
@Inject
constructor(
    private val caster: UtilitySpellCaster,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = caster.spell(objs.spell_charge)
        onIfOverlayButton(spell.component) { protectedAccess.launch(player) { charge(spell) } }
        onPlayerSoftTimer(timers.charge_spell) {
            player.clearCharge()
            player.mes(FADED_MESSAGE)
        }
    }

    private fun ProtectedAccess.charge(spell: MagicSpell) {
        if (player.isChargeOnCooldown()) {
            mes(COOLDOWN_MESSAGE)
            return
        }
        if (!caster.cast(this, spell)) {
            return
        }
        player.activateCharge()
        anim(utility_seqs.charge)
        mes(CHARGED_MESSAGE)
    }

    companion object {
        const val CHARGED_MESSAGE: String = "You feel charged with magic power."
        const val FADED_MESSAGE: String = "Your magical charge fades away."
        const val COOLDOWN_MESSAGE: String =
            "You can't recast that yet, your current Charge is too strong."
    }
}
