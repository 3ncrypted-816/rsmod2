package org.runerealm.content.skills.magic.teleports

import jakarta.inject.Inject
import org.runerealm.api.area.checker.WildernessArea
import org.runerealm.api.combat.commons.magic.MagicSpell
import org.runerealm.api.combat.manager.MagicRuneManager
import org.runerealm.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.runerealm.api.config.refs.objs
import org.runerealm.api.config.refs.params
import org.runerealm.api.config.refs.stats
import org.runerealm.api.config.refs.varbits
import org.runerealm.api.player.protect.ProtectedAccess
import org.runerealm.api.player.protect.ProtectedAccessLauncher
import org.runerealm.api.player.ui.ifOpenMainModal
import org.runerealm.api.player.ui.ifSetEvents
import org.runerealm.api.player.ui.ifSetText
import org.runerealm.api.player.vars.boolVarBit
import org.runerealm.api.player.vars.intVarp
import org.runerealm.api.script.onApPlayerT
import org.runerealm.api.script.onIfModalButton
import org.runerealm.api.script.onIfOverlayButton
import org.runerealm.api.spells.MagicSpellRegistry
import org.runerealm.api.teleblock.isTeleblocked
import org.runerealm.api.type.builders.varp.VarpBuilder
import org.runerealm.api.type.refs.comp.ComponentReferences
import org.runerealm.api.type.refs.interf.InterfaceReferences
import org.runerealm.api.type.refs.seq.SeqReferences
import org.runerealm.api.type.refs.spot.SpotanimReferences
import org.runerealm.api.type.refs.synth.SynthReferences
import org.runerealm.api.type.refs.varp.VarpReferences
import org.runerealm.events.EventBus
import org.runerealm.game.entity.Player
import org.runerealm.game.type.interf.IfEvent
import org.runerealm.game.type.obj.ObjType
import org.runerealm.game.type.obj.ObjTypeList
import org.runerealm.map.CoordGrid
import org.runerealm.plugin.scripts.PluginScript
import org.runerealm.plugin.scripts.ScriptContext

/**
 * The standard spellbook's teleport spells, following the OSRS Wiki (e.g.
 * [Varrock Teleport](https://oldschool.runescape.wiki/w/Varrock_Teleport)):
 * - City teleports cast the standard teleport animation and graphic, and land after 3 ticks. They
 *   cannot be used above level 20 Wilderness or while Tele Blocked.
 * - Teleport to House only gives a message, since there are no player-owned houses.
 * - Tele Other spells ([Tele Other](https://oldschool.runescape.wiki/w/Tele_Other)) ask the target
 *   to accept the teleport. The target must have Accept Aid on, must not be Tele Blocked and must
 *   not be above level 20 Wilderness.
 *
 * Quest requirements (e.g. Plague City for Ardougne Teleport) are not checked, since quests are not
 * tracked by this server.
 */
class SpellbookTeleports
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (spellObj in CITY_TELEPORTS) {
            val spell = spell(spellObj)
            val dest = destination(spellObj)
            onIfOverlayButton(spell.component) {
                protectedAccess.launch(player) { cityTeleport(spell, dest) }
            }
        }

        val house = spell(objs.spell_poh_teleport)
        onIfOverlayButton(house.component) {
            protectedAccess.launch(player) { mes(NO_HOUSE_MESSAGE) }
        }

        for (spellObj in TELEOTHER_SPELLS) {
            val spell = spell(spellObj)
            val dest = destination(TELEOTHER_DESTINATIONS.getValue(spellObj))
            val name = TELEOTHER_NAMES.getValue(spellObj)
            onApPlayerT(spell.component) { teleOther(it.target, spell, dest, name) }
        }

        onIfModalButton(teleother_components.accept) { acceptTeleOther() }
        onIfModalButton(teleother_components.decline) { declineTeleOther() }
    }

    private fun spell(obj: ObjType): MagicSpell =
        checkNotNull(spells.getObjSpell(obj)) { "Spell not found: $obj" }

    private fun destination(obj: ObjType): CoordGrid =
        DESTINATION_OVERRIDES[obj.id] ?: objTypes[obj].param(params.spell_telecoord)

    private suspend fun ProtectedAccess.cityTeleport(spell: MagicSpell, dest: CoordGrid) {
        if (!ItemTeleports.canTeleport(this, SPELL_WILDERNESS_LIMIT)) {
            return
        }
        val cast = runes.attemptCast(player, spell)
        if (cast.isFailure()) {
            return
        }
        statAdvance(stats.magic, spell.castXp)
        soundSynth(teleport_synths.teleport)
        ItemTeleports.jewelleryTeleport(this, dest)
    }

    private fun ProtectedAccess.teleOther(
        target: Player,
        spell: MagicSpell,
        dest: CoordGrid,
        name: String,
    ) {
        if (!target.acceptAid) {
            mes(NOT_ACCEPTING_AID_MESSAGE)
            return
        }
        if (target.isTeleblocked() || !canTeleportFrom(target)) {
            mes(TARGET_CANNOT_TELEPORT_MESSAGE)
            return
        }
        if (target.isAccessProtected) {
            mes(TARGET_BUSY_MESSAGE)
            return
        }
        val cast = runes.attemptCast(player, spell)
        if (cast.isFailure()) {
            return
        }
        statAdvance(stats.magic, spell.castXp)
        faceSquare(target.coords)
        anim(teleother_seqs.cast)
        spotanim(teleother_spots.cast, height = 92)
        soundSynth(teleother_synths.cast)

        target.teleOtherDest = dest.packed
        target.ifOpenMainModal(teleother_interfaces.teleport_other, eventBus)
        target.ifSetText(teleother_components.caster_name, player.displayName)
        target.ifSetText(teleother_components.location, name)
        target.ifSetEvents(teleother_components.accept, 0..0, IfEvent.Op1)
        target.ifSetEvents(teleother_components.decline, 0..0, IfEvent.Op1)
    }

    private suspend fun ProtectedAccess.acceptTeleOther() {
        val packed = player.teleOtherDest
        player.teleOtherDest = 0
        ifClose()
        if (packed == 0) {
            return
        }
        if (!ItemTeleports.canTeleport(this, SPELL_WILDERNESS_LIMIT)) {
            return
        }
        anim(teleother_seqs.impact)
        spotanim(teleother_spots.impact)
        soundSynth(teleport_synths.teleport)
        delay(3)
        telejump(CoordGrid(packed))
        resetAnim()
    }

    private fun ProtectedAccess.declineTeleOther() {
        player.teleOtherDest = 0
        ifClose()
    }

    private fun canTeleportFrom(target: Player): Boolean =
        WildernessArea.level(target.coords) <= SPELL_WILDERNESS_LIMIT

    private companion object {
        const val SPELL_WILDERNESS_LIMIT = 20
        const val NO_HOUSE_MESSAGE = "You don't have a house to teleport to."
        const val NOT_ACCEPTING_AID_MESSAGE = "That player is not accepting aid."
        const val TARGET_CANNOT_TELEPORT_MESSAGE = "That player cannot be teleported right now."
        const val TARGET_BUSY_MESSAGE = "That player is busy right now."
    }
}

private var Player.acceptAid by boolVarBit(varbits.option_acceptaid)
private var Player.teleOtherDest by intVarp(teleother_varps.dest)

/** The city teleports of the standard spellbook. */
internal val CITY_TELEPORTS: List<ObjType> =
    listOf(
        objs.spell_varrock_teleport,
        objs.spell_lumbridge_teleport,
        objs.spell_falador_teleport,
        objs.spell_camelot_teleport,
        objs.spell_kourendcastle_teleport,
        objs.spell_ardougne_teleport,
        objs.spell_civitas_fortis_teleport,
        objs.spell_watchtower_teleport,
        objs.spell_trollheim_teleport,
        objs.spell_apeatoll_teleport,
    )

internal val TELEOTHER_SPELLS: List<ObjType> =
    listOf(
        objs.spell_teleother_lumbridge,
        objs.spell_teleother_falador,
        objs.spell_teleother_camelot,
    )

/** Tele Other spells send the target to the same tile as the matching city teleport. */
private val TELEOTHER_DESTINATIONS: Map<ObjType, ObjType> =
    mapOf(
        objs.spell_teleother_lumbridge to objs.spell_lumbridge_teleport,
        objs.spell_teleother_falador to objs.spell_falador_teleport,
        objs.spell_teleother_camelot to objs.spell_camelot_teleport,
    )

private val TELEOTHER_NAMES: Map<ObjType, String> =
    mapOf(
        objs.spell_teleother_lumbridge to "Lumbridge",
        objs.spell_teleother_falador to "Falador",
        objs.spell_teleother_camelot to "Camelot",
    )

/**
 * The cache's Watchtower Teleport coordinate is not the top of the Watchtower; the wiki's landing
 * tile (also used by the Watchtower teleport tablet) is used instead.
 */
private val DESTINATION_OVERRIDES: Map<Int, CoordGrid> =
    mapOf(objs.spell_watchtower_teleport.id to CoordGrid(2546, 3114, 2))

typealias teleother_interfaces = TeleOtherInterfaces

object TeleOtherInterfaces : InterfaceReferences() {
    val teleport_other = find("teleport_other")
}

typealias teleother_components = TeleOtherComponents

object TeleOtherComponents : ComponentReferences() {
    val accept = find("teleport_other:teleport_accept")
    val decline = find("teleport_other:teleport_decline")
    val caster_name = find("teleport_other:casters_name")
    val location = find("teleport_other:teleport_location")
}

typealias teleother_seqs = TeleOtherSeqs

object TeleOtherSeqs : SeqReferences() {
    val cast = find("human_teleport_other_casting")
    val impact = find("human_teleport_other_impact")
}

typealias teleother_spots = TeleOtherSpots

object TeleOtherSpots : SpotanimReferences() {
    val cast = find("teleport_other_casting")
    val impact = find("teleport_other_impact")
}

typealias teleother_synths = TeleOtherSynths

object TeleOtherSynths : SynthReferences() {
    val cast = find("tele_other_cast")
}

typealias teleport_synths = SpellTeleportSynths

object SpellTeleportSynths : SynthReferences() {
    val teleport = find("teleport_all")
}

typealias teleother_varps = TeleOtherVarps

object TeleOtherVarps : VarpReferences() {
    /** Server-side: the packed coord a pending Tele Other will send this player to. */
    val dest = find("teleother_dest")
}

internal object TeleOtherVarpBuilder : VarpBuilder() {
    init {
        build("teleother_dest") { temporary = true }
    }
}
