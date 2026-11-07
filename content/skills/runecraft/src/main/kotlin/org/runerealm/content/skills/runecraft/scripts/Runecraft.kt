package org.runerealm.content.skills.runecraft.scripts

import jakarta.inject.Inject
import org.runerealm.api.config.refs.stats
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.stat.runecraftingLvl
import org.runerealm.api.player.vars.VarPlayerIntMapSetter
import org.runerealm.api.random.GameRandom
import org.runerealm.api.script.advanced.onWearposChange
import org.runerealm.api.script.onOpLoc1
import org.runerealm.api.script.onOpLocU
import org.runerealm.api.script.onPlayerLogin
import org.runerealm.api.stats.xpmod.XpModifiers
import org.runerealm.content.skills.runecraft.configs.Altar
import org.runerealm.content.skills.runecraft.configs.ComboRune
import org.runerealm.content.skills.runecraft.configs.EssenceKind
import org.runerealm.content.skills.runecraft.configs.RunecraftAltars
import org.runerealm.content.skills.runecraft.configs.runecraft_objs
import org.runerealm.content.skills.runecraft.configs.runecraft_seqs
import org.runerealm.content.skills.runecraft.configs.runecraft_spotanims
import org.runerealm.game.entity.Player
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.game.type.obj.Wearpos
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * Runecraft, per the OSRS Wiki ([Runecraft](https://oldschool.runescape.wiki/w/Runecraft)):
 * - Mysterious ruins are entered by using the altar's talisman on them, or with "Enter" while
 *   wearing its tiara (or an elemental/catalytic tiara, or a Runecraft cape).
 * - "Craft-rune" binds every carried essence; higher levels make several runes per essence.
 * - Exit portals return the player outside the ruins.
 * - A tiara and talisman used on their altar make a talisman tiara.
 * - Runes (or their talisman) used on another elemental altar make combination runes: one pure
 *   essence and one rune per attempt, 50% success without a binding necklace.
 * - The Kourend blood and soul altars bind dark essence fragments.
 *
 * Not implemented: the Abyss, the Ourania altar (ZMI), Guardians of the Rift, Magic Imbue and
 * binding necklace charges.
 */
class Runecraft
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (altar in RunecraftAltars.all) {
            onOpLoc1(altar.altar) { craft(altar) }
            altar.portal?.let { portal -> onOpLoc1(portal) { leave(altar) } }
            registerRuins(altar)
            registerTiara(altar)
        }
        for (combo in RunecraftAltars.combos) {
            onOpLocU(combo.altar, combo.rune) { craftCombo(combo) }
            onOpLocU(combo.altar, combo.talisman) { craftCombo(combo) }
        }
        onWearposChange {
            if (wearpos == Wearpos.Hat || wearpos == Wearpos.Back) {
                player.updateRuinVarBits()
            }
        }
        onPlayerLogin { player.updateRuinVarBits() }
    }

    private fun ScriptContext.registerRuins(altar: Altar) {
        val ruins = altar.ruins ?: return
        val talismans = buildList {
            add(ruins.talisman)
            if (altar in RunecraftAltars.elemental) add(runecraft_objs.elemental_talisman)
            if (altar in RunecraftAltars.catalytic) add(runecraft_objs.catalytic_talisman)
        }
        for (variant in ruins.variants) {
            for (talisman in talismans) {
                onOpLocU(variant, talisman) { enterWithTalisman(altar, talisman) }
            }
        }
        // Only the "Enter" variant, shown while wearing a tiara, has an option.
        onOpLoc1(ruins.variants.last()) { enterWithTiara(altar) }
    }

    private fun ScriptContext.registerTiara(altar: Altar) {
        val ruins = altar.ruins ?: return
        onOpLocU(altar.altar, runecraft_objs.tiara) { bindTiara(altar) }
        onOpLocU(altar.altar, ruins.talisman) { bindTiara(altar) }
    }

    private suspend fun ProtectedAccess.enterWithTalisman(altar: Altar, talisman: ObjType) {
        val ruins = altar.ruins ?: return
        mes("You hold the ${name(talisman)} towards the mysterious ruins.")
        mes("You feel a powerful force take hold of you...")
        delay(ENTER_DELAY)
        teleport(ruins.inside)
    }

    private suspend fun ProtectedAccess.enterWithTiara(altar: Altar) {
        val ruins = altar.ruins ?: return
        if (!player.canEnterWithWorn(altar)) {
            mes("You need to wear the right tiara to enter these ruins.")
            return
        }
        mes("You feel a powerful force take hold of you...")
        delay(ENTER_DELAY)
        teleport(ruins.inside)
    }

    private fun ProtectedAccess.leave(altar: Altar) {
        val ruins = altar.ruins ?: return
        mes("You step through the portal...")
        teleport(ruins.outside)
    }

    private fun ProtectedAccess.craft(altar: Altar) {
        if (player.runecraftingLvl < altar.level) {
            mes("You need a Runecraft level of ${altar.level} to craft ${altar.name} runes.")
            return
        }
        val objs = runecraft_objs
        val rune = if (altar.essence == EssenceKind.Any) invTotal(inv, objs.rune_essence) else 0
        val pure = if (altar.essence != EssenceKind.Dark) invTotal(inv, objs.pure_essence) else 0
        val daeyalt =
            if (altar.essence != EssenceKind.Dark) invTotal(inv, objs.daeyalt_essence) else 0
        val dark =
            if (altar.essence == EssenceKind.Dark) invTotal(inv, objs.dark_essence_fragments) else 0
        val total = rune + pure + daeyalt + dark
        if (total == 0) {
            val message =
                when {
                    altar.essence == EssenceKind.Dark ->
                        "You need dark essence fragments to bind ${altar.name} runes."
                    altar.essence == EssenceKind.Pure && objs.rune_essence in inv ->
                        "This altar can only bind pure essence."
                    else -> "You do not have any essence to bind."
                }
            mes(message)
            return
        }
        if (rune > 0) invDel(inv, objs.rune_essence, count = rune)
        if (pure > 0) invDel(inv, objs.pure_essence, count = pure)
        if (daeyalt > 0) invDel(inv, objs.daeyalt_essence, count = daeyalt)
        if (dark > 0) invDel(inv, objs.dark_essence_fragments, count = dark)

        val runes = total * altar.runesPerEssence(player.runecraftingLvl)
        invAdd(inv, altar.rune, count = runes)
        // Wiki: daeyalt essence grants 50% more experience than pure essence.
        val xp = altar.xp * (total - daeyalt) + altar.xp * DAEYALT_XP_MULTIPLIER * daeyalt
        statAdvance(stats.runecrafting, xp * xpMods.get(player, stats.runecrafting))
        anim(runecraft_seqs.bind)
        spotanim(runecraft_spotanims.bind, height = BIND_SPOT_HEIGHT)
        mes("You bind the temple's power into ${name(altar.rune)}s.")
    }

    private fun ProtectedAccess.craftCombo(combo: ComboRune) {
        val objs = runecraft_objs
        if (player.runecraftingLvl < combo.level) {
            val name = name(combo.product)
            mes("You need a Runecraft level of ${combo.level} to craft ${name}s.")
            return
        }
        if (combo.talisman !in inv) {
            mes("You need ${article(name(combo.talisman))} ${name(combo.talisman)} to do that.")
            return
        }
        val essence = invTotal(inv, objs.pure_essence)
        val runes = invTotal(inv, combo.rune)
        val attempts = minOf(essence, runes)
        if (attempts == 0) {
            if (essence == 0) {
                mes("You need pure essence to craft combination runes.")
            } else {
                mes("You need ${name(combo.rune)}s to craft combination runes.")
            }
            return
        }
        invDel(inv, objs.pure_essence, count = attempts)
        invDel(inv, combo.rune, count = attempts)
        invDel(inv, combo.talisman, count = 1)
        val binding = player.worn[Wearpos.Front.slot]?.id == objs.binding_necklace.id
        val made = if (binding) attempts else (1..attempts).count { random.randomBoolean() }
        if (made > 0) {
            invAdd(inv, combo.product, count = made)
            statAdvance(
                stats.runecrafting,
                combo.xp * made * xpMods.get(player, stats.runecrafting),
            )
        }
        anim(runecraft_seqs.bind)
        spotanim(runecraft_spotanims.bind, height = BIND_SPOT_HEIGHT)
        mes("You bind the temple's power into ${name(combo.product)}s.")
    }

    private fun ProtectedAccess.bindTiara(altar: Altar) {
        val ruins = altar.ruins ?: return
        val objs = runecraft_objs
        if (objs.tiara !in inv || ruins.talisman !in inv) {
            mes("You need a tiara and ${article(name(ruins.talisman))} ${name(ruins.talisman)}.")
            return
        }
        invDel(inv, ruins.talisman, count = 1)
        invDel(inv, objs.tiara, count = 1)
        invAdd(inv, ruins.tiara)
        statAdvance(stats.runecrafting, altar.tiaraXp * xpMods.get(player, stats.runecrafting))
        anim(runecraft_seqs.bind)
        mes("You bind the power of the talisman into your tiara.")
    }

    private fun name(obj: ObjType): String = objTypes[obj].name.lowercase()

    private fun article(name: String): String = if (name.first() in "aeiou") "an" else "a"

    private companion object {
        const val ENTER_DELAY = 2
        const val BIND_SPOT_HEIGHT = 100
        const val DAEYALT_XP_MULTIPLIER = 1.5
    }
}

/** Whether the worn tiara (or cape) lets this player "Enter" the ruins of [altar]. */
fun Player.canEnterWithWorn(altar: Altar): Boolean {
    val ruins = altar.ruins ?: return false
    val objs = runecraft_objs
    val hat = worn[Wearpos.Hat.slot]?.id
    val cape = worn[Wearpos.Back.slot]?.id
    return hat == ruins.tiara.id ||
        (hat == objs.elemental_tiara.id && altar in RunecraftAltars.elemental) ||
        (hat == objs.catalytic_tiara.id && altar in RunecraftAltars.catalytic) ||
        cape == objs.runecraft_cape.id ||
        cape == objs.runecraft_cape_t.id
}

/** Sets the client varbits that show "Enter" on the ruins the worn tiara (or cape) opens. */
fun Player.updateRuinVarBits() {
    for (altar in RunecraftAltars.all) {
        val ruins = altar.ruins ?: continue
        val value = if (canEnterWithWorn(altar)) 1 else 0
        if (vars[ruins.varbit] != value) {
            VarPlayerIntMapSetter.set(this, ruins.varbit, value)
        }
    }
}
